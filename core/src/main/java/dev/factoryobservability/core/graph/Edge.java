package dev.factoryobservability.core.graph;

import java.util.Objects;
import java.util.UUID;

import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.resource.ResourceId;

/** A declared or evidenced relationship; proximity alone must not create an exact flow edge. */
public record Edge(UUID id, UUID from, UUID to, ResourceId resource, Evidence evidence) {
    public Edge {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(evidence, "evidence");
    }
}
