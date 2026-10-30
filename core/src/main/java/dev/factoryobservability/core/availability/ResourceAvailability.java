package dev.factoryobservability.core.availability;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalDouble;

import dev.factoryobservability.core.Checks;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceUnit;

/** Unknown quantities are empty, not zero. Available is declared by the provider, not derived here. */
public record ResourceAvailability(
        ResourceId resource,
        ResourceUnit unit,
        AvailabilityScope scope,
        Instant asOf,
        OptionalDouble observed,
        OptionalDouble reachable,
        OptionalDouble allocated,
        OptionalDouble available,
        Evidence evidence) {
    public ResourceAvailability {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(evidence, "evidence");
        if (resource.kind() != unit.kind()) {
            throw new IllegalArgumentException("Availability resource kind and unit kind must match");
        }
        validate(observed, "observed");
        validate(reachable, "reachable");
        validate(allocated, "allocated");
        validate(available, "available");
        atMost(reachable, observed, "reachable must not exceed observed");
        atMost(available, reachable, "available must not exceed reachable");
        atMost(available, observed, "available must not exceed observed");
        atMost(allocated, observed, "allocated must not exceed observed");
    }

    private static void validate(OptionalDouble amount, String name) {
        Objects.requireNonNull(amount, name).ifPresent(value -> Checks.nonNegative(value, name));
    }

    private static void atMost(OptionalDouble lower, OptionalDouble upper, String message) {
        if (lower.isPresent() && upper.isPresent() && lower.getAsDouble() > upper.getAsDouble()) {
            throw new IllegalArgumentException(message);
        }
    }
}
