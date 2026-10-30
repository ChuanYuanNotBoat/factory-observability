package dev.factoryobservability.minecraft.observation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import dev.factoryobservability.core.Checks;
import dev.factoryobservability.core.metric.InMemoryMetricStore;
import dev.factoryobservability.core.metric.SnapshotMetricProjector;
import dev.factoryobservability.core.observation.ObservationBus;
import dev.factoryobservability.core.observation.VisibilityTimeline;

/** A world's ephemeral analysis state; no Minecraft types or world references are retained. */
public final class ObservationSession {
    private final String worldId;
    private final ObservationIndex index;
    private final InMemoryMetricStore metrics;
    private final VisibilityTimeline timeline;
    private final ObservationBus bus;
    private final Deque<String> failures = new ArrayDeque<>();

    public ObservationSession(String worldId, int nodeCapacity, int sampleCapacity, int queueCapacity) {
        this.worldId = Checks.text(worldId, "worldId");
        index = new ObservationIndex(nodeCapacity);
        metrics = new InMemoryMetricStore(sampleCapacity);
        timeline = new VisibilityTimeline(nodeCapacity * 8);
        bus = new ObservationBus(queueCapacity, (id, error) -> failure(id + ": " + error.getClass().getSimpleName()));
        bus.subscribe("index", index);
        bus.subscribe("metrics", new SnapshotMetricProjector(metrics));
        bus.subscribe("visibility", timeline);
    }

    public void failure(String message) {
        if (failures.size() == 16) { failures.removeFirst(); }
        failures.addLast(message);
    }

    public String worldId() { return worldId; }
    public ObservationIndex index() { return index; }
    public InMemoryMetricStore metrics() { return metrics; }
    public VisibilityTimeline timeline() { return timeline; }
    public ObservationBus bus() { return bus; }
    public List<String> failures() { return List.copyOf(failures); }
}
