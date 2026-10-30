package dev.factoryobservability.minecraft.observation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.metric.MetricKey;
import dev.factoryobservability.core.metric.TimeRange;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.observation.VisibilityEnded;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuSessionTrackerTest {
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    private final SubjectRef subject = new SubjectRef("world", "minecraft:overworld", "menu-session-1");
    private final ResourceId iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);
    private final ResourceId namedIron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM, Map.of("components_sha256", "fixture"));

    @Test void aggregatesCoveredSlotsButKeepsVariantsSeparateAndZerosReplacedResources() {
        var output = new ArrayList<Observation>();
        var tracker = new MenuSessionTracker(subject, output::add);
        tracker.sample(start, List.of(stack(iron, 32), stack(iron, 16), stack(namedIron, 4)), 3, 6);
        List<ResourceSnapshot> samples = output.stream().map(Observation::payload).filter(ResourceSnapshot.class::isInstance)
                .map(ResourceSnapshot.class::cast).toList();
        assertEquals(2, samples.size());
        assertTrue(samples.stream().anyMatch(value -> value.stack().resource().equals(iron) && value.stack().amount() == 48));
        assertTrue(output.stream().allMatch(value -> value.evidence().coverage() == 0.5));
        output.clear();
        tracker.sample(start.plusSeconds(1), List.of(stack(namedIron, 4)), 3, 6);
        assertTrue(output.stream().anyMatch(value -> value.payload() instanceof ResourceSnapshot snapshot
                && snapshot.stack().resource().equals(iron) && snapshot.stack().amount() == 0));
    }

    @Test void closingOrFailingAReadRetainsLastQuantityAndCreatesAVisibilityGap() {
        var session = new ObservationSession("world", 8, 100, 100);
        session.index().register(subject, "Chest", Node.Kind.STORAGE, start);
        var tracker = new MenuSessionTracker(subject, session.bus());
        tracker.sample(start, List.of(stack(iron, 32)), 1, 1);
        tracker.sample(start.plusSeconds(10), List.of(stack(iron, 48)), 1, 1);
        tracker.interrupt(start.plusSeconds(11));
        tracker.sample(start.plusSeconds(30), List.of(stack(iron, 48)), 1, 1);
        tracker.sample(start.plusSeconds(35), List.of(stack(iron, 48)), 1, 1);
        tracker.end(start.plusSeconds(36), VisibilityEnded.Reason.MENU_CLOSED);
        session.bus().drain(100);

        var row = session.index().views().getFirst();
        assertFalse(row.visible());
        assertEquals(48, row.resources().get(iron).amount());
        assertEquals(VisibilityEnded.Reason.MENU_CLOSED, row.ended().orElseThrow());
        assertEquals(0.25, session.timeline().coverage(subject, new TimeRange(start, start.plusSeconds(60))), 0.00001);
        assertTrue(session.metrics().query(subject, MetricKey.inventory(iron, ResourceUnit.ITEMS),
                new TimeRange(start, start.plusSeconds(60))).stream().noneMatch(value -> value.value() == 0));
        assertThrows(IllegalStateException.class, () -> tracker.sample(start.plusSeconds(40), List.of(), 1, 1));
    }

    @Test void changingScopeRequiresANewSessionAndCannotSilentlyCreateInventoryDeltas() {
        var tracker = new MenuSessionTracker(subject, value -> {});
        tracker.sample(start, List.of(stack(iron, 10)), 1, 2);
        assertThrows(IllegalArgumentException.class, () -> tracker.sample(start.plusSeconds(1), List.of(), 2, 2));
    }

    @Test void emptyReadableInventoriesStillAccumulateVisibility() {
        var session = new ObservationSession("world", 8, 100, 100);
        session.index().register(subject, "Empty chest", Node.Kind.STORAGE, start);
        var tracker = new MenuSessionTracker(subject, session.bus());
        tracker.sample(start, List.of(), 9, 9);
        tracker.sample(start.plusSeconds(10), List.of(), 9, 9);
        tracker.end(start.plusSeconds(11), VisibilityEnded.Reason.MENU_CLOSED);
        session.bus().drain(100);
        assertEquals(0.5, session.timeline().coverage(subject, new TimeRange(start, start.plusSeconds(20))), 0.00001);
    }

    private ResourceStack stack(ResourceId resource, int count) { return new ResourceStack(resource, count, ResourceUnit.ITEMS); }
}
