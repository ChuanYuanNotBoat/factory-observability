package dev.factoryobservability.compat.api;

import java.util.Objects;
import java.util.Set;

public record RuntimeContext(Set<String> loadedModIds, Side side) {
    public RuntimeContext {
        loadedModIds = Set.copyOf(loadedModIds);
        Objects.requireNonNull(side, "side");
    }

    public enum Side {
        CLIENT,
        SERVER
    }
}
