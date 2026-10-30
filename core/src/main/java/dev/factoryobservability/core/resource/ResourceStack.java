package dev.factoryobservability.core.resource;

import java.util.Objects;

import dev.factoryobservability.core.Checks;

public record ResourceStack(ResourceId resource, double amount, ResourceUnit unit) {
    public ResourceStack {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(unit, "unit");
        Checks.nonNegative(amount, "amount");
        if (resource.kind() != unit.kind()) {
            throw new IllegalArgumentException("Resource kind and unit kind must match");
        }
    }
}
