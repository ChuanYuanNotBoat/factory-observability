package dev.factoryobservability.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.metric.TimeRange;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ObservationBus;
import dev.factoryobservability.core.observation.ObservationPayload;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.observation.VisibilityEnded;
import dev.factoryobservability.core.observation.VisibilityStarted;
import dev.factoryobservability.core.observation.VisibilityTimeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservationLifecycleTest {
    private final SubjectRef subject = new SubjectRef("world", "minecraft:overworld", "source");
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");

    @Test void visibilityGapsAreNotFilledAndActiveCoverageStopsAtLastHeartbeat() {
        var timeline = new VisibilityTimeline(16);
        timeline.emit(event(0, new VisibilityStarted()));
        timeline.emit(event(12, new VisibilityStarted()));
        timeline.emit(event(13, new VisibilityEnded(VisibilityEnded.Reason.MENU_CLOSED)));
        timeline.emit(event(30, new VisibilityStarted()));
        timeline.emit(event(36, new VisibilityStarted()));
        assertEquals(0.3, timeline.coverage(subject, new TimeRange(start, start.plusSeconds(60))), 0.00001);
        assertEquals(0.0, timeline.coverage(subject, new TimeRange(start.plusSeconds(13), start.plusSeconds(30))));
        assertEquals(0.0, timeline.coverage(new SubjectRef("other", "minecraft:overworld", "source"),
                new TimeRange(start, start.plusSeconds(60))));
    }

    @Test void outOfOrderClosureCannotEraseAnActiveObservation() {
        var timeline = new VisibilityTimeline(8);
        timeline.emit(event(0, new VisibilityStarted()));
        timeline.emit(event(10, new VisibilityStarted()));
        timeline.emit(event(5, new VisibilityEnded(VisibilityEnded.Reason.LOST_VISIBILITY)));
        timeline.emit(event(20, new VisibilityStarted()));
        assertEquals(1.0, timeline.coverage(subject, new TimeRange(start, start.plusSeconds(20))));
    }

    @Test void queueBudgetAndOverflowAreExplicitAndSubscriberFailureIsIsolated() {
        var errors = new ArrayList<String>();
        var output = new ArrayList<Observation>();
        var bus = new ObservationBus(2, (id, error) -> errors.add(id));
        bus.subscribe("broken", observation -> { throw new IllegalStateException("fixture"); });
        bus.subscribe("healthy", output::add);
        assertThrows(IllegalArgumentException.class, () -> bus.subscribe("healthy", output::add));
        bus.emit(event(0, new VisibilityStarted()));
        bus.emit(event(1, new VisibilityStarted()));
        bus.emit(event(2, new VisibilityStarted()));
        assertEquals(1, bus.droppedCount());
        assertEquals(1, bus.drain(1));
        assertEquals(1, bus.pendingCount());
        bus.drain(10);
        assertEquals(2, output.size());
        assertEquals(List.of("broken"), errors);
    }

    private Observation event(long seconds, ObservationPayload payload) {
        return new Observation(UUID.randomUUID(), start.plusSeconds(seconds), subject, payload,
                new Evidence(Provenance.CLIENT_OBSERVATION, DataQuality.OBSERVED, 1, 1));
    }
}
