package dev.factoryobservability.core.resource;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import dev.factoryobservability.core.Checks;

/** Qualifiers carry stable component identities or hashes, never live game objects. */
public record ResourceId(String namespace, String path, ResourceKind kind, Map<String, String> qualifiers) {
    public ResourceId {
        Checks.text(namespace, "namespace");
        Checks.text(path, "path");
        Objects.requireNonNull(kind, "kind");
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-z0-9/._-]+")) {
            throw new IllegalArgumentException("Resource identifiers must use lowercase namespaced paths");
        }
        Objects.requireNonNull(qualifiers, "qualifiers").forEach((key, value) -> {
            Checks.text(key, "qualifier key");
            Objects.requireNonNull(value, "qualifier value");
        });
        qualifiers = Collections.unmodifiableMap(new TreeMap<>(qualifiers));
    }

    public ResourceId(String namespace, String path, ResourceKind kind) {
        this(namespace, path, kind, Map.of());
    }
}
