package dev.factoryobservability.core.metric;

import java.time.Instant;
import java.util.Objects;

/** Half-open interval [startInclusive, endExclusive). */
public record TimeRange(Instant startInclusive, Instant endExclusive) {
    public TimeRange {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Time range must have positive duration");
        }
    }

    public boolean contains(Instant time) {
        return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
    }
}
