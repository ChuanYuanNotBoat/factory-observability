package dev.factoryobservability.core.observation;

@FunctionalInterface
public interface ObservationSink {
    void emit(Observation observation);
}
