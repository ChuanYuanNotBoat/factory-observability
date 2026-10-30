package dev.factoryobservability.core.metric;

import java.time.Instant;
import java.util.Objects;

import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.observation.SubjectRef;

public record MetricSample(SubjectRef subject, MetricKey key, Instant time, double value, Evidence evidence) {
    public MetricSample {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(evidence, "evidence");
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Metric value must be finite");
        }
    }
}
