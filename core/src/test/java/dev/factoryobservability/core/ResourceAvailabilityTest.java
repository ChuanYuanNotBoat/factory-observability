package dev.factoryobservability.core;

import java.time.Instant;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.availability.AvailabilityScope;
import dev.factoryobservability.core.availability.ResourceAvailability;
import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceAvailabilityTest {
    private final ResourceId iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);
    private final AvailabilityScope scope = new AvailabilityScope(new SubjectRef("world", "minecraft:overworld", "factory-1"));
    private final Evidence evidence = new Evidence(Provenance.CLIENT_OBSERVATION, DataQuality.OBSERVED, 0.2, 1.0);

    @Test
    void observedStockDoesNotMakeReachabilityOrAvailabilityKnown() {
        var result = availability(OptionalDouble.of(64), OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty());
        assertEquals(64, result.observed().orElseThrow());
        assertFalse(result.reachable().isPresent());
        assertFalse(result.allocated().isPresent());
        assertFalse(result.available().isPresent());
    }

    @Test
    void availableStockIsNotAutomaticallyObservedMinusAllocated() {
        var result = availability(OptionalDouble.of(100), OptionalDouble.of(60), OptionalDouble.of(10), OptionalDouble.of(20));
        assertEquals(20, result.available().orElseThrow());
    }

    @Test
    void rejectsImpossibleKnownBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> availability(OptionalDouble.of(10), OptionalDouble.of(11), OptionalDouble.empty(), OptionalDouble.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> availability(OptionalDouble.of(10), OptionalDouble.of(5), OptionalDouble.empty(), OptionalDouble.of(6)));
        assertThrows(IllegalArgumentException.class,
                () -> availability(OptionalDouble.of(10), OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.of(11)));
        assertThrows(IllegalArgumentException.class,
                () -> availability(OptionalDouble.of(-1), OptionalDouble.empty(), OptionalDouble.empty(), OptionalDouble.empty()));
    }

    private ResourceAvailability availability(OptionalDouble observed, OptionalDouble reachable,
            OptionalDouble allocated, OptionalDouble available) {
        return new ResourceAvailability(iron, ResourceUnit.ITEMS, scope, Instant.EPOCH,
                observed, reachable, allocated, available, evidence);
    }
}
