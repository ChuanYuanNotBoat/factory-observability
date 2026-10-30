package dev.factoryobservability.core.observation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import dev.factoryobservability.core.evidence.Evidence;

public record Observation(UUID id, Instant time, SubjectRef subject, ObservationPayload payload, Evidence evidence) {
    public Observation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(evidence, "evidence");
    }
}
