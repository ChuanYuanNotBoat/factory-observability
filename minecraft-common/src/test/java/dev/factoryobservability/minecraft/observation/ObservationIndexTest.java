package dev.factoryobservability.minecraft.observation;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.observation.VisibilityEnded;
import dev.factoryobservability.core.observation.VisibilityStarted;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationIndexTest {
    private final SubjectRef item = new SubjectRef("world", "minecraft:overworld", "entity-1");
    private final ResourceId iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);
    private final Evidence sync = new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED, 1, 1);

    @Test void itemDisappearanceIsCensoredAndDoesNotInventPickupOrZero() {
        var index = new ObservationIndex(8);
        index.register(item, "Iron", Node.Kind.ITEM_ENTITY, Instant.EPOCH);
        index.emit(new Observation(UUID.randomUUID(), Instant.EPOCH, item, new VisibilityStarted(), sync));
        index.emit(new Observation(UUID.randomUUID(), Instant.EPOCH, item,
                new ResourceSnapshot(new ResourceStack(iron, 16, ResourceUnit.ITEMS)), sync));
        index.emit(new Observation(UUID.randomUUID(), Instant.EPOCH.plusSeconds(1), item,
                new VisibilityEnded(VisibilityEnded.Reason.LOST_VISIBILITY), sync));
        var view = index.views().getFirst();
        assertFalse(view.visible());
        assertEquals(16, view.resources().get(iron).amount());
        assertEquals(VisibilityEnded.Reason.LOST_VISIBILITY, view.ended().orElseThrow());
    }

    @Test void userDeclaredEdgesShareInspectorNodeIdentityAndAreRemovedOnEviction() {
        var index = new ObservationIndex(2);
        index.register(item, "Iron", Node.Kind.ITEM_ENTITY, Instant.EPOCH);
        var target = new SubjectRef("world", "minecraft:overworld", "menu-1");
        index.register(target, "Chest", Node.Kind.STORAGE, Instant.EPOCH);
        var first = index.views().getFirst().node().id();
        var second = index.views().getLast().node().id();
        index.declareEdge(first, second, iron);
        var graph = index.graph("world");
        assertEquals(first, graph.edges().getFirst().from());
        assertEquals(Provenance.USER_DECLARED, graph.edges().getFirst().evidence().provenance());
        assertEquals(DataQuality.INFERRED, graph.edges().getFirst().evidence().quality());
        index.register(new SubjectRef("world", "minecraft:overworld", "menu-2"), "Furnace", Node.Kind.STORAGE, Instant.EPOCH);
        assertEquals(2, index.views().size());
        assertTrue(index.graph("world").edges().isEmpty());
    }
}
