package dev.factoryobservability.core.resource;

import java.util.Objects;

import dev.factoryobservability.core.Checks;

/** Different units are not interchangeable without an explicit conversion. */
public record ResourceUnit(String symbol, ResourceKind kind) {
    public static final ResourceUnit ITEMS = new ResourceUnit("item", ResourceKind.ITEM);
    public static final ResourceUnit MILLIBUCKETS = new ResourceUnit("mB", ResourceKind.FLUID);
    public static final ResourceUnit FORGE_ENERGY = new ResourceUnit("FE", ResourceKind.ENERGY);

    public ResourceUnit {
        Checks.text(symbol, "symbol");
        Objects.requireNonNull(kind, "kind");
    }
}
