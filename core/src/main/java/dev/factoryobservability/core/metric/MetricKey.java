package dev.factoryobservability.core.metric;

import java.util.Objects;

import dev.factoryobservability.core.Checks;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceUnit;

/** Initial metric keys describe resource amounts; resource identity includes qualifiers. */
public record MetricKey(String namespace, String name, ResourceId resource, ResourceUnit unit) {
    public MetricKey {
        Checks.text(namespace, "namespace");
        Checks.text(name, "name");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(unit, "unit");
        if (resource.kind() != unit.kind()) {
            throw new IllegalArgumentException("Metric resource kind and unit kind must match");
        }
    }

    public static MetricKey inventory(ResourceId resource, ResourceUnit unit) {
        return new MetricKey("factory_observability", "inventory", resource, unit);
    }
}
