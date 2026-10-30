package dev.factoryobservability.minecraft.observation;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.graph.Edge;
import dev.factoryobservability.core.graph.Factory;
import dev.factoryobservability.core.graph.FactoryGraph;
import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.graph.Stage;
import dev.factoryobservability.core.observation.EntityPosition;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ObservationSink;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.observation.VisibilityEnded;
import dev.factoryobservability.core.observation.VisibilityStarted;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceStack;

/** Bounded current-session inspector state. All mutation happens on the game thread. */
public final class ObservationIndex implements ObservationSink {
    public record View(Node node, Instant firstSeen, Instant lastSeen, boolean visible,
            Optional<VisibilityEnded.Reason> ended, Map<ResourceId, ResourceStack> resources,
            Optional<EntityPosition> position, Evidence evidence) {
        public View { resources = Map.copyOf(resources); }
    }

    private final int capacity;
    private final Map<SubjectRef, View> views = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    public ObservationIndex(int capacity) {
        if (capacity < 1) { throw new IllegalArgumentException("Index capacity must be positive"); }
        this.capacity = capacity;
    }

    public void register(SubjectRef subject, String title, Node.Kind kind, Instant time) {
        if (views.containsKey(subject)) { return; }
        if (views.size() == capacity) {
            SubjectRef oldest = views.values().stream().filter(view -> !view.visible())
                    .map(view -> view.node().subject()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Active node budget exhausted"));
            UUID removed = views.remove(oldest).node().id();
            edges.removeIf(edge -> edge.from().equals(removed) || edge.to().equals(removed));
        }
        Node node = new Node(UUID.randomUUID(), kind, subject, Map.of("name", title));
        views.put(subject, new View(node, time, time, false, Optional.empty(), Map.of(), Optional.empty(),
                new Evidence(Provenance.CLIENT_OBSERVATION, DataQuality.UNKNOWN, 0.0, 0.0)));
    }

    @Override
    public void emit(Observation observation) {
        View old = views.get(observation.subject());
        if (old == null || observation.time().isBefore(old.lastSeen())) { return; }
        boolean visible = old.visible();
        Optional<VisibilityEnded.Reason> ended = old.ended();
        Map<ResourceId, ResourceStack> resources = new HashMap<>(old.resources());
        Optional<EntityPosition> position = old.position();
        Evidence evidence = old.evidence();
        Instant lastSeen = old.lastSeen();
        if (observation.payload() instanceof VisibilityStarted) {
            visible = true;
            ended = Optional.empty();
            evidence = observation.evidence();
            lastSeen = observation.time();
        } else if (observation.payload() instanceof VisibilityEnded end) {
            visible = false;
            ended = Optional.of(end.reason());
        } else if (observation.payload() instanceof ResourceSnapshot snapshot) {
            ResourceStack stack = snapshot.stack();
            if (stack.amount() == 0) { resources.remove(stack.resource()); }
            else { resources.put(stack.resource(), stack); }
            evidence = observation.evidence();
            lastSeen = observation.time();
        } else if (observation.payload() instanceof EntityPosition value) {
            position = Optional.of(value);
            lastSeen = observation.time();
        }
        views.put(observation.subject(), new View(old.node(), old.firstSeen(), lastSeen,
                visible, ended, resources, position, evidence));
    }

    public List<View> views() { return List.copyOf(views.values()); }

    public void name(SubjectRef subject, String title) {
        View old = views.get(subject);
        if (old == null || title.equals(old.node().labels().get("name"))) { return; }
        Node renamed = new Node(old.node().id(), old.node().kind(), subject, Map.of("name", title));
        views.put(subject, new View(renamed, old.firstSeen(), old.lastSeen(), old.visible(), old.ended(),
                old.resources(), old.position(), old.evidence()));
    }

    public void declareEdge(UUID from, UUID to, ResourceId resource) {
        if (edges.size() >= capacity) { throw new IllegalStateException("Relationship budget exhausted"); }
        if (from.equals(to)) { throw new IllegalArgumentException("Choose two different nodes"); }
        Set<UUID> nodes = views.values().stream().map(view -> view.node().id()).collect(java.util.stream.Collectors.toSet());
        if (!nodes.contains(from) || !nodes.contains(to)) { throw new IllegalArgumentException("Unknown node"); }
        if (edges.stream().noneMatch(edge -> edge.from().equals(from) && edge.to().equals(to) && edge.resource().equals(resource))) {
            edges.add(new Edge(UUID.randomUUID(), from, to, resource,
                    new Evidence(Provenance.USER_DECLARED, DataQuality.INFERRED, 0.0, 1.0)));
        }
    }

    public FactoryGraph graph(String worldId) {
        UUID factoryId = UUID.nameUUIDFromBytes(("factory:" + worldId).getBytes(StandardCharsets.UTF_8));
        var factory = new Factory(factoryId, "Session observations", worldId);
        List<Node> nodes = views.values().stream().map(View::node)
                .filter(node -> node.subject().worldId().equals(worldId)).toList();
        List<Stage> stages = new ArrayList<>();
        for (Node.Kind kind : List.of(Node.Kind.STORAGE, Node.Kind.ITEM_ENTITY)) {
            Set<UUID> members = nodes.stream().filter(node -> node.kind() == kind).map(Node::id)
                    .collect(java.util.stream.Collectors.toSet());
            stages.add(new Stage(UUID.nameUUIDFromBytes((factoryId + ":" + kind).getBytes(StandardCharsets.UTF_8)),
                    factoryId, kind.name(), members, Set.of()));
        }
        Set<UUID> ids = nodes.stream().map(Node::id).collect(java.util.stream.Collectors.toSet());
        return new FactoryGraph(factory, stages, nodes,
                edges.stream().filter(edge -> ids.contains(edge.from()) && ids.contains(edge.to())).toList());
    }

    public void removeEdge(UUID id) { edges.removeIf(edge -> edge.id().equals(id)); }
}
