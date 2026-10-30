package dev.factoryobservability.compat.api;

import java.util.Set;

import dev.factoryobservability.core.observation.ObservationSink;

/** Providers own hook registration and must release hooks and world references on stop. */
public interface ObservationProvider {
    String id();

    Set<AdapterCapability> capabilities();

    /** Called on the game thread; emit immutable DTOs only. */
    void start(ObservationSink sink);

    void stop();
}
