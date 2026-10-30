package dev.factoryobservability.core.graph;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import dev.factoryobservability.core.observation.SubjectRef;

public record Node(UUID id, Kind kind, SubjectRef subject, Map<String, String> labels) {
    public Node {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(subject, "subject");
        labels = Map.copyOf(labels);
    }

    public enum Kind { STORAGE, ITEM_ENTITY, MACHINE, TRANSPORT, UNKNOWN }
}
