package dev.factoryobservability.core.observation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import dev.factoryobservability.core.Checks;

/** Game-thread queue with a drain budget and subscriber failure isolation. */
public final class ObservationBus implements ObservationSink {
    private final int capacity;
    private final Deque<Observation> pending = new ArrayDeque<>();
    private final Map<String, ObservationSink> subscribers = new LinkedHashMap<>();
    private final BiConsumer<String, RuntimeException> onFailure;
    private long dropped;

    public ObservationBus(int capacity, BiConsumer<String, RuntimeException> onFailure) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Queue capacity must be positive");
        }
        this.capacity = capacity;
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
    }

    public void subscribe(String id, ObservationSink subscriber) {
        Checks.text(id, "id");
        Objects.requireNonNull(subscriber, "subscriber");
        if (subscribers.putIfAbsent(id, subscriber) != null) {
            throw new IllegalArgumentException("Duplicate subscriber: " + id);
        }
    }

    @Override
    public void emit(Observation observation) {
        Objects.requireNonNull(observation, "observation");
        if (pending.size() >= capacity) {
            dropped++;
        } else {
            pending.addLast(observation);
        }
    }

    public int drain(int budget) {
        if (budget < 0) {
            throw new IllegalArgumentException("Drain budget must be non-negative");
        }
        int processed = 0;
        while (processed < budget && !pending.isEmpty()) {
            Observation observation = pending.removeFirst();
            for (var entry : new ArrayList<>(subscribers.entrySet())) {
                try {
                    entry.getValue().emit(observation);
                } catch (RuntimeException error) {
                    subscribers.remove(entry.getKey());
                    onFailure.accept(entry.getKey(), error);
                }
            }
            processed++;
        }
        return processed;
    }

    public int pendingCount() { return pending.size(); }
    public long droppedCount() { return dropped; }
}
