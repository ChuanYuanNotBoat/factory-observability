package dev.factoryobservability.core.availability;

import java.util.Objects;

import dev.factoryobservability.core.observation.SubjectRef;

/** Scope identity is supplied by the provider, for example a factory or a storage network. */
public record AvailabilityScope(SubjectRef subject) {
    public AvailabilityScope {
        Objects.requireNonNull(subject, "subject");
    }
}
