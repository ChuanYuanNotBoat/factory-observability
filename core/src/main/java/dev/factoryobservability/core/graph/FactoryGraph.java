package dev.factoryobservability.core.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable graph with local references and acyclic stage nesting validated at the boundary. */
public record FactoryGraph(Factory factory, List<Stage> stages, List<Node> nodes, List<Edge> edges) {
    public FactoryGraph {
        Objects.requireNonNull(factory, "factory");
        stages = List.copyOf(stages);
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        Set<UUID> nodeIds = unique(nodes.stream().map(Node::id).toList());
        Set<UUID> stageIds = unique(stages.stream().map(Stage::id).toList());
        unique(edges.stream().map(Edge::id).toList());
        Map<UUID, Stage> byId = new HashMap<>();
        for (Node node : nodes) {
            require(factory.worldId().equals(node.subject().worldId()), "Node belongs to a different world");
        }
        for (Stage stage : stages) {
            require(stage.factoryId().equals(factory.id()), "Stage belongs to a different factory");
            require(nodeIds.containsAll(stage.nodes()), "Unknown node in stage");
            require(stageIds.containsAll(stage.children()), "Unknown child stage");
            byId.put(stage.id(), stage);
        }
        Set<UUID> visited = new HashSet<>();
        for (Stage stage : stages) {
            visit(stage.id(), byId, visited, new HashSet<>());
        }
        for (Edge edge : edges) {
            require(nodeIds.contains(edge.from()) && nodeIds.contains(edge.to()), "Unknown edge endpoint");
        }
    }

    private static Set<UUID> unique(List<UUID> ids) {
        Set<UUID> unique = new HashSet<>(ids);
        require(unique.size() == ids.size(), "Duplicate graph identity");
        return unique;
    }

    private static void visit(UUID id, Map<UUID, Stage> stages, Set<UUID> visited, Set<UUID> path) {
        require(path.add(id), "Cyclic stage nesting");
        if (visited.add(id)) {
            for (UUID child : stages.get(id).children()) {
                visit(child, stages, visited, path);
            }
        }
        path.remove(id);
    }

    private static void require(boolean valid, String message) {
        if (!valid) {
            throw new IllegalArgumentException(message);
        }
    }
}
