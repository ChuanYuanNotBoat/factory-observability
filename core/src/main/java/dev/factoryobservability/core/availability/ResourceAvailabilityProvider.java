package dev.factoryobservability.core.availability;

import dev.factoryobservability.core.resource.ResourceId;

public interface ResourceAvailabilityProvider {
    /** Returns explicitly unknown fields when a scope cannot be observed. */
    ResourceAvailability query(ResourceId resource, AvailabilityScope scope);
}
