package dev.factoryobservability.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.factoryobservability.core.graph.Factory;
import dev.factoryobservability.core.graph.FactoryGraph;
import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.graph.Stage;
import dev.factoryobservability.core.observation.SubjectRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FactoryGraphTest {
    private final Factory factory = new Factory(UUID.randomUUID(), "Iron", "world");
    private final Node node = new Node(UUID.randomUUID(), Node.Kind.STORAGE,
            new SubjectRef("world", "minecraft:overworld", "chest"), Map.of());

    @Test void acceptsNestedStagesWithSharedStableNodes() {
        var child = new Stage(UUID.randomUUID(), factory.id(), "Washing", Set.of(node.id()), Set.of());
        var parent = new Stage(UUID.randomUUID(), factory.id(), "Processing", Set.of(), Set.of(child.id()));
        var graph = new FactoryGraph(factory, List.of(parent, child), List.of(node), List.of());
        assertEquals(node.id(), graph.nodes().getFirst().id());
        assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
    }

    @Test void rejectsDanglingReferencesAndCrossWorldNodes() {
        var invalid = new Stage(UUID.randomUUID(), factory.id(), "Stage", Set.of(UUID.randomUUID()), Set.of());
        assertThrows(IllegalArgumentException.class, () -> new FactoryGraph(factory, List.of(invalid), List.of(node), List.of()));
        var otherWorld = new Node(UUID.randomUUID(), Node.Kind.STORAGE,
                new SubjectRef("other", "minecraft:overworld", "chest"), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new FactoryGraph(factory, List.of(), List.of(otherWorld), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new FactoryGraph(factory, List.of(), List.of(node, node), List.of()));
    }

    @Test void rejectsStageCycles() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        var first = new Stage(a, factory.id(), "A", Set.of(), Set.of(b));
        var second = new Stage(b, factory.id(), "B", Set.of(), Set.of(a));
        assertThrows(IllegalArgumentException.class, () -> new FactoryGraph(factory, List.of(first, second), List.of(), List.of()));
    }
}
