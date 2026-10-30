package dev.factoryobservability.compat.api;

@FunctionalInterface
public interface AdapterRegistrar {
    void register(ObservationProvider provider);
}
