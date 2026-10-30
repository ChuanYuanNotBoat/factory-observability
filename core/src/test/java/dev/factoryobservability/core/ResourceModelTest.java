package dev.factoryobservability.core;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceModelTest {
    private final ResourceId iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);

    @Test
    void resourceIdentityKeepsComponentVariantsSeparateAndImmutable() {
        var qualifiers = new HashMap<>(Map.of("component_hash", "a"));
        var first = new ResourceId("example", "cell", ResourceKind.ITEM, qualifiers);
        qualifiers.put("component_hash", "b");
        var second = new ResourceId("example", "cell", ResourceKind.ITEM, qualifiers);

        assertEquals("a", first.qualifiers().get("component_hash"));
        assertNotEquals(first, second);
        assertThrows(UnsupportedOperationException.class, () -> first.qualifiers().put("x", "y"));
    }

    @Test
    void rejectsMixingItemsAndFluidUnits() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceStack(iron, 10, ResourceUnit.MILLIBUCKETS));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidQuantities(double amount) {
        assertThrows(IllegalArgumentException.class, () -> new ResourceStack(iron, amount, ResourceUnit.ITEMS));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY})
    void rejectsInvalidEvidenceFractions(double fraction) {
        assertThrows(IllegalArgumentException.class,
                () -> new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED, fraction, 0.9));
        assertThrows(IllegalArgumentException.class,
                () -> new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED, 0.2, fraction));
    }

    @Test
    void highConfidenceDoesNotIncreaseCoverage() {
        var evidence = new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED, 0.2, 1.0);
        assertEquals(0.2, evidence.coverage());
        assertEquals(1.0, evidence.confidence());
    }
}
