package dev.factoryobservability.core.metric;

import java.util.List;

import dev.factoryobservability.core.observation.SubjectRef;

public interface MetricStore {
    void append(MetricSample sample);

    /** Returns a detached chronological snapshot; missing observations remain missing. */
    List<MetricSample> query(SubjectRef subject, MetricKey key, TimeRange range);
}
