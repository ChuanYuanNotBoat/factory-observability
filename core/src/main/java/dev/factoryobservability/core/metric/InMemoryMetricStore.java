package dev.factoryobservability.core.metric;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import dev.factoryobservability.core.observation.SubjectRef;

/** Prototype store bounded globally by arrival count; not a durable time-series database. */
public final class InMemoryMetricStore implements MetricStore {
    private final int maximumSamples;
    private final Deque<MetricSample> samples = new ArrayDeque<>();

    public InMemoryMetricStore(int maximumSamples) {
        if (maximumSamples < 1) {
            throw new IllegalArgumentException("maximumSamples must be positive");
        }
        this.maximumSamples = maximumSamples;
    }

    @Override
    public synchronized void append(MetricSample sample) {
        Objects.requireNonNull(sample, "sample");
        if (samples.size() == maximumSamples) {
            samples.removeFirst();
        }
        samples.addLast(sample);
    }

    @Override
    public synchronized List<MetricSample> query(SubjectRef subject, MetricKey key, TimeRange range) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(range, "range");
        return samples.stream()
                .filter(sample -> sample.subject().equals(subject) && sample.key().equals(key))
                .filter(sample -> range.contains(sample.time()))
                .sorted(Comparator.comparing(MetricSample::time))
                .toList();
    }
}
