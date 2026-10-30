package dev.factoryobservability.fixtures;

import java.time.Instant;
import java.util.Locale;

import dev.factoryobservability.core.metric.InMemoryMetricStore;
import dev.factoryobservability.core.metric.MetricKey;
import dev.factoryobservability.core.metric.MetricSample;
import dev.factoryobservability.core.metric.SnapshotMetricProjector;
import dev.factoryobservability.core.metric.TimeRange;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceUnit;
import dev.factoryobservability.minecraft.observation.MenuObservationBridge;

/** A deterministic synthetic data path; does not launch or connect to Minecraft. */
public final class ObservationDemo {
    private ObservationDemo() {}

    public static void main(String[] args) {
        var store = new InMemoryMetricStore(100);
        var menu = new MenuObservationBridge(new SnapshotMetricProjector(store));
        var slot = new SubjectRef("demo-world", "minecraft:overworld", "menu-session-1/slot-0");
        var iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);
        var start = Instant.parse("2026-01-01T00:00:00Z");

        menu.observeSlot(slot, start, iron, 32, 1.0);
        menu.observeSlot(slot, start.plusSeconds(10), iron, 48, 1.0);

        System.out.println("Synthetic menu observation -> inventory metrics (no game connection)");
        for (MetricSample sample : store.query(slot, MetricKey.inventory(iron, ResourceUnit.ITEMS),
                new TimeRange(start, start.plusSeconds(11)))) {
            System.out.printf(Locale.ROOT, "%s %s:%s %.0f %s [%s, %s, coverage=%.2f, confidence=%.2f]%n",
                    sample.time(), iron.namespace(), iron.path(), sample.value(), sample.key().unit().symbol(),
                    sample.evidence().provenance(), sample.evidence().quality(),
                    sample.evidence().coverage(), sample.evidence().confidence());
        }
        System.out.println("Inventory change: +16 items; production and gross flow remain unknown.");
        System.out.println("Slot coverage describes the supplied snapshot scope, not continuous factory coverage.");
    }
}
