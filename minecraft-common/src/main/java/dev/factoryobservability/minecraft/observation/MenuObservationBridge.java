package dev.factoryobservability.minecraft.observation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ObservationSink;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;

/**
 * Normalizes a loader-supplied synchronized menu slot without reading the game world.
 * The caller must identify the slot (including menu session), exclude player inventory
 * where appropriate, and emit an explicit zero for a previously observed resource when
 * a slot is cleared or replaced. Closing the menu must not synthesize a zero snapshot.
 */
public final class MenuObservationBridge {
    private final ObservationSink sink;

    public MenuObservationBridge(ObservationSink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /** Coverage applies only to this slot's declared scope, never to the entire factory. */
    public void observeSlot(SubjectRef slot, Instant time, ResourceId item, int count, double coverage) {
        Objects.requireNonNull(item, "item");
        if (item.kind() != ResourceKind.ITEM) {
            throw new IllegalArgumentException("Menu item slots require an ITEM resource");
        }
        sink.emit(new Observation(UUID.randomUUID(), time, slot,
                new ResourceSnapshot(new ResourceStack(item, count, ResourceUnit.ITEMS)),
                new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED, coverage, 1.0)));
    }
}
