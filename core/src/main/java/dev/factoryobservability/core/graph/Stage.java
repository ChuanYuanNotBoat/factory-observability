package dev.factoryobservability.core.graph;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import dev.factoryobservability.core.Checks;

public record Stage(UUID id, UUID factoryId, String name, Set<UUID> nodes, Set<UUID> children) {
    public Stage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factoryId, "factoryId");
        Checks.text(name, "name");
        nodes = Set.copyOf(nodes);
        children = Set.copyOf(children);
    }
}
