package dev.factoryobservability.core.metric;

import java.util.Objects;

import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ObservationSink;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.resource.ResourceStack;

/** Projects snapshots into gauges without inferring production, consumption or gross flow. */
public final class SnapshotMetricProjector implements ObservationSink {
    private final MetricStore store;

    public SnapshotMetricProjector(MetricStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public void emit(Observation observation) {
        Objects.requireNonNull(observation, "observation");
        if (observation.payload() instanceof ResourceSnapshot snapshot) {
            ResourceStack stack = snapshot.stack();
            store.append(new MetricSample(observation.subject(), MetricKey.inventory(stack.resource(), stack.unit()),
                    observation.time(), stack.amount(), observation.evidence()));
        }
    }
}
