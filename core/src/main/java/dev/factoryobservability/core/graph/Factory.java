package dev.factoryobservability.core.graph;

import java.util.Objects;
import java.util.UUID;

import dev.factoryobservability.core.Checks;

public record Factory(UUID id, String name, String worldId) {
    public Factory {
        Objects.requireNonNull(id, "id");
        Checks.text(name, "name");
        Checks.text(worldId, "worldId");
    }
}
