package dev.factoryobservability.minecraft.observation;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ObservationPayload;
import dev.factoryobservability.core.observation.ObservationSink;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.observation.VisibilityEnded;
import dev.factoryobservability.core.observation.VisibilityStarted;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;

/** One session observes a fixed, explicitly bounded set of non-player slots. */
public final class MenuSessionTracker {
    private final SubjectRef subject;
    private final ObservationSink sink;
    private Map<ResourceId, Double> previous = Map.of();
    private boolean open = true;
    private boolean visible;
    private int covered = -1;
    private int total = -1;

    public MenuSessionTracker(SubjectRef subject, ObservationSink sink) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public void sample(Instant time, List<ResourceStack> contents, int coveredSlots, int totalSlots) {
        if (!open) { throw new IllegalStateException("Menu session has ended"); }
        if (coveredSlots < 1 || totalSlots < coveredSlots) {
            throw new IllegalArgumentException("Menu coverage must have at least one observed slot");
        }
        if (covered != -1 && (covered != coveredSlots || total != totalSlots)) {
            throw new IllegalArgumentException("Changed slot scope requires a new menu session");
        }
        Map<ResourceId, Double> current = new HashMap<>();
        for (ResourceStack stack : contents) {
            if (stack.resource().kind() != ResourceKind.ITEM || !stack.unit().equals(ResourceUnit.ITEMS)) {
                throw new IllegalArgumentException("Menu snapshots require item quantities");
            }
            current.merge(stack.resource(), stack.amount(), Double::sum);
            if (!Double.isFinite(current.get(stack.resource()))) {
                throw new IllegalArgumentException("Menu aggregate exceeds finite quantity range");
            }
        }
        covered = coveredSlots;
        total = totalSlots;
        Evidence evidence = new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED,
                (double) coveredSlots / totalSlots, 1.0);
        // An empty but readable inventory is still a heartbeat; an unreadable inventory is not.
        emit(time, new VisibilityStarted(), evidence);
        visible = true;
        var resources = new HashSet<>(previous.keySet());
        resources.addAll(current.keySet());
        for (ResourceId resource : resources) {
            emit(time, new ResourceSnapshot(new ResourceStack(resource, current.getOrDefault(resource, 0.0),
                    ResourceUnit.ITEMS)), evidence);
        }
        previous = Map.copyOf(current);
    }

    public void interrupt(Instant time) {
        if (open && visible) {
            emit(time, new VisibilityEnded(VisibilityEnded.Reason.SOURCE_ERROR), unknownEvidence());
            visible = false;
        }
    }

    public void end(Instant time, VisibilityEnded.Reason reason) {
        if (open) {
            emit(time, new VisibilityEnded(reason), unknownEvidence());
            open = false;
            visible = false;
        }
    }

    private static Evidence unknownEvidence() {
        return new Evidence(Provenance.CLIENT_OBSERVATION, DataQuality.UNKNOWN, 0.0, 0.0);
    }

    private void emit(Instant time, ObservationPayload payload, Evidence evidence) {
        sink.emit(new Observation(UUID.randomUUID(), time, subject, payload, evidence));
    }
}
