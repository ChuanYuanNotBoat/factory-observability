package dev.factoryobservability.core.observation;

import java.util.Objects;

import dev.factoryobservability.core.resource.ResourceStack;

/** One observed resource quantity, not a transfer event or a complete inventory. */
public record ResourceSnapshot(ResourceStack stack) implements ObservationPayload {
    public ResourceSnapshot {
        Objects.requireNonNull(stack, "stack");
    }
}
