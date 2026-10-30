package dev.factoryobservability.core.observation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import dev.factoryobservability.core.metric.TimeRange;

/** Bounded observed intervals. Active coverage stops at the last confirmed heartbeat, never at query time. */
public final class VisibilityTimeline implements ObservationSink {
    private record Interval(SubjectRef subject, Instant start, Instant end) {}
    private record Active(Instant start, Instant lastSeen) {}
    private final int capacity;
    private final Deque<Interval> closed = new ArrayDeque<>();
    private final Map<SubjectRef, Active> active = new HashMap<>();

    public VisibilityTimeline(int capacity) {
        if (capacity < 1) { throw new IllegalArgumentException("Capacity must be positive"); }
        this.capacity = capacity;
    }

    @Override
    public void emit(Observation observation) {
        SubjectRef subject = observation.subject();
        Instant time = observation.time();
        if (observation.payload() instanceof VisibilityStarted) {
            Active previous = active.get(subject);
            if (previous == null) {
                if (active.size() == capacity) { throw new IllegalStateException("Too many active observation scopes"); }
                active.put(subject, new Active(time, time));
            } else if (!time.isBefore(previous.lastSeen())) {
                active.put(subject, new Active(previous.start(), time));
            }
        } else if (observation.payload() instanceof VisibilityEnded) {
            Active previous = active.get(subject);
            if (previous != null && !time.isBefore(previous.lastSeen())) {
                active.remove(subject);
                if (closed.size() == capacity) { closed.removeFirst(); }
                closed.addLast(new Interval(subject, previous.start(), previous.lastSeen()));
            }
        } else {
            Active previous = active.get(subject);
            if (previous != null && !time.isBefore(previous.lastSeen())) {
                active.put(subject, new Active(previous.start(), time));
            }
        }
    }

    public double coverage(SubjectRef subject, TimeRange range) {
        long observed = 0;
        for (Interval interval : closed) {
            if (interval.subject().equals(subject)) {
                observed += overlap(interval.start(), interval.end(), range);
            }
        }
        Active current = active.get(subject);
        if (current != null) { observed += overlap(current.start(), current.lastSeen(), range); }
        return Math.min(1.0, (double) observed / Duration.between(range.startInclusive(), range.endExclusive()).toNanos());
    }

    private static long overlap(Instant start, Instant end, TimeRange range) {
        Instant left = start.isAfter(range.startInclusive()) ? start : range.startInclusive();
        Instant right = end.isBefore(range.endExclusive()) ? end : range.endExclusive();
        return left.isBefore(right) ? Duration.between(left, right).toNanos() : 0;
    }
}
