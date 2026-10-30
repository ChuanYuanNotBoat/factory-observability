package dev.factoryobservability.compat.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.observation.ObservationSink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderRegistryTest {
    @Test void duplicateIdsFailAndPartiallyStartedProvidersAreCleanedUpWithoutStoppingHealthyOnes() {
        var events = new ArrayList<String>();
        var registry = new ProviderRegistry((id, error) -> events.add("error:" + id));
        var healthy = new Fixture("healthy", false, events);
        registry.register(new Fixture("broken", true, events));
        registry.register(healthy);
        assertThrows(IllegalArgumentException.class, () -> registry.register(healthy));
        registry.start(value -> {});
        registry.start(value -> {});
        registry.stop();
        registry.stop();
        assertEquals(List.of("start:broken", "error:broken", "stop:broken", "start:healthy", "stop:healthy"), events);
    }

    private record Fixture(String id, boolean broken, List<String> events) implements ObservationProvider {
        @Override public Set<AdapterCapability> capabilities() { return Set.of(AdapterCapability.ITEM_STORAGE); }
        @Override public void start(ObservationSink sink) {
            events.add("start:" + id);
            if (broken) { throw new IllegalStateException("fixture"); }
        }
        @Override public void stop() { events.add("stop:" + id); }
    }
}
