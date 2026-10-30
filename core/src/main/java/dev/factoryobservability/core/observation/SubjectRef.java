package dev.factoryobservability.core.observation;

import dev.factoryobservability.core.Checks;

/** A locally scoped identity. World IDs must not contain a plaintext server endpoint. */
public record SubjectRef(String worldId, String dimensionId, String id) {
    public SubjectRef {
        Checks.text(worldId, "worldId");
        Checks.text(dimensionId, "dimensionId");
        Checks.text(id, "id");
    }
}
