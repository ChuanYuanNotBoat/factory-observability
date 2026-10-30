package dev.factoryobservability.minecraft.observation;

import java.time.Instant;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuObservationBridgeTest {
    private final SubjectRef slot = new SubjectRef("world", "minecraft:overworld", "menu-1/slot-0");
    private final ResourceId iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);

    @Test
    void clientSyncRetainsObservedQualityAndLimitedCoverageIncludingAnExplicitEmptySlot() {
        var observations = new ArrayList<Observation>();
        var bridge = new MenuObservationBridge(observations::add);
        bridge.observeSlot(slot, Instant.EPOCH, iron, 32, 0.2);
        bridge.observeSlot(slot, Instant.EPOCH.plusSeconds(1), iron, 0, 0.2);

        assertEquals(2, observations.size());
        var first = observations.getFirst();
        assertEquals(slot, first.subject());
        assertEquals(Provenance.CLIENT_SYNC, first.evidence().provenance());
        assertEquals(DataQuality.OBSERVED, first.evidence().quality());
        assertEquals(0.2, first.evidence().coverage());
        assertEquals(0, ((ResourceSnapshot) observations.getLast().payload()).stack().amount());
    }

    @Test
    void invalidSlotsDoNotEmitObservations() {
        var observations = new ArrayList<Observation>();
        var bridge = new MenuObservationBridge(observations::add);
        assertThrows(IllegalArgumentException.class, () -> bridge.observeSlot(slot, Instant.EPOCH, iron, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> bridge.observeSlot(slot, Instant.EPOCH, iron, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> bridge.observeSlot(slot, Instant.EPOCH,
                new ResourceId("minecraft", "water", ResourceKind.FLUID), 1, 1));
        assertTrue(observations.isEmpty());
    }
}
