package dev.factoryobservability.core;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.metric.InMemoryMetricStore;
import dev.factoryobservability.core.metric.MetricKey;
import dev.factoryobservability.core.metric.MetricSample;
import dev.factoryobservability.core.metric.SnapshotMetricProjector;
import dev.factoryobservability.core.metric.TimeRange;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricPipelineTest {
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    private final SubjectRef subject = new SubjectRef("world-a", "minecraft:overworld", "slot-1");
    private final ResourceId iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);
    private final MetricKey key = MetricKey.inventory(iron, ResourceUnit.ITEMS);
    private final Evidence evidence = new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED, 0.2, 1.0);
    private final TimeRange range = new TimeRange(start, start.plusSeconds(10));

    @Test
    void projectsAnInventoryGaugeAndPreservesEvidence() {
        var store = new InMemoryMetricStore(10);
        new SnapshotMetricProjector(store).emit(new Observation(UUID.randomUUID(), start, subject,
                new ResourceSnapshot(new ResourceStack(iron, 32, ResourceUnit.ITEMS)), evidence));

        var result = store.query(subject, key, range);
        assertEquals(List.of(new MetricSample(subject, key, start, 32, evidence)), result);
        assertTrue(store.query(subject, new MetricKey("factory_observability", "production", iron, ResourceUnit.ITEMS),
                range).isEmpty());
    }

    @Test
    void isolatesWorldDimensionSubjectAndResourceVariants() {
        var store = new InMemoryMetricStore(10);
        store.append(sample(start, 32));
        store.append(new MetricSample(new SubjectRef("world-b", "minecraft:overworld", "slot-1"), key, start, 64, evidence));
        store.append(new MetricSample(new SubjectRef("world-a", "minecraft:the_nether", "slot-1"), key, start, 64, evidence));
        store.append(new MetricSample(new SubjectRef("world-a", "minecraft:overworld", "slot-2"), key, start, 64, evidence));
        var variant = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM, Map.of("component_hash", "x"));
        store.append(new MetricSample(subject, MetricKey.inventory(variant, ResourceUnit.ITEMS), start, 64, evidence));

        assertEquals(List.of(sample(start, 32)), store.query(subject, key, range));
    }

    @Test
    void sortsOutOfOrderSamplesAndUsesHalfOpenRangesWithoutFillingGaps() {
        var store = new InMemoryMetricStore(10);
        store.append(sample(start.plusSeconds(5), 48));
        store.append(sample(start, 32));
        store.append(sample(start.plusSeconds(10), 64));

        var snapshot = store.query(subject, key, range);
        assertEquals(List.of(sample(start, 32), sample(start.plusSeconds(5), 48)), snapshot);
        assertTrue(store.query(subject, key, new TimeRange(start.plusSeconds(6), start.plusSeconds(9))).isEmpty());
        store.append(sample(start.plusSeconds(8), 50));
        assertEquals(2, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
    }

    @Test
    void evictsOldestArrivalsWhenTheGlobalCapacityIsReached() {
        var store = new InMemoryMetricStore(2);
        store.append(sample(start, 1));
        store.append(sample(start.plusSeconds(1), 2));
        store.append(sample(start.plusSeconds(2), 3));
        assertEquals(List.of(sample(start.plusSeconds(1), 2), sample(start.plusSeconds(2), 3)),
                store.query(subject, key, range));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryMetricStore(0));
    }

    private MetricSample sample(Instant time, double value) {
        return new MetricSample(subject, key, time, value, evidence);
    }
}
