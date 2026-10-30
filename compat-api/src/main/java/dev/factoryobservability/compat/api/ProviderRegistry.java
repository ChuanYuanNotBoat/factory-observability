package dev.factoryobservability.compat.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import dev.factoryobservability.core.Checks;
import dev.factoryobservability.core.observation.ObservationSink;

/** Failure-isolated lifecycle. A provider whose start fails is still offered stop for cleanup. */
public final class ProviderRegistry implements AdapterRegistrar {
    private final Map<String, ObservationProvider> providers = new LinkedHashMap<>();
    private final Map<String, ObservationProvider> active = new LinkedHashMap<>();
    private final BiConsumer<String, RuntimeException> onFailure;
    private boolean started;

    public ProviderRegistry(BiConsumer<String, RuntimeException> onFailure) {
        this.onFailure = Objects.requireNonNull(onFailure, "onFailure");
    }

    @Override
    public void register(ObservationProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (started) { throw new IllegalStateException("Cannot register after starting providers"); }
        String id = Checks.text(provider.id(), "provider id");
        if (providers.putIfAbsent(id, provider) != null) { throw new IllegalArgumentException("Duplicate provider: " + id); }
    }

    public void start(ObservationSink sink) {
        Objects.requireNonNull(sink, "sink");
        if (started) { return; }
        started = true;
        providers.forEach((id, provider) -> {
            try {
                provider.start(sink);
                active.put(id, provider);
            } catch (RuntimeException error) {
                onFailure.accept(id, error);
                stop(id, provider);
            }
        });
    }

    public void stop() {
        if (!started) { return; }
        active.forEach(this::stop);
        active.clear();
        started = false;
    }

    private void stop(String id, ObservationProvider provider) {
        try { provider.stop(); }
        catch (RuntimeException error) { onFailure.accept(id, error); }
    }
}
