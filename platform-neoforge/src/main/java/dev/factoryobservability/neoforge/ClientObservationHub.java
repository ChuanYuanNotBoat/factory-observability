package dev.factoryobservability.neoforge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ObservationSink;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.minecraft.observation.ObservationSession;

public final class ClientObservationHub implements ObservationSink {
    private final ClientSettings settings;
    private final Map<ResourceId, Component> names = new LinkedHashMap<>();
    private final Map<ResourceId, ItemStack> previews = new LinkedHashMap<>();
    private ObservationSession session;
    private int omittedEntities;

    public ClientObservationHub(ClientSettings settings) { this.settings = settings; }

    public ObservationSession beginWorld(String worldId) {
        if (session == null || !session.worldId().equals(worldId)) {
            session = new ObservationSession(worldId, settings.maximumNodes(), settings.maximumSamples(), settings.queueCapacity());
            names.clear();
            previews.clear();
            omittedEntities = 0;
        }
        return session;
    }

    public void clear() { session = null; names.clear(); previews.clear(); omittedEntities = 0; }
    public Optional<ObservationSession> session() { return Optional.ofNullable(session); }

    @Override
    public void emit(Observation observation) {
        if (session != null && session.worldId().equals(observation.subject().worldId())) { session.bus().emit(observation); }
    }

    public void remember(ResourceId resource, ItemStack stack) {
        if (!names.containsKey(resource) && names.size() >= settings.maximumNodes() * 4) {
            ResourceId oldest = names.keySet().iterator().next();
            names.remove(oldest);
            previews.remove(oldest);
        }
        names.put(resource, stack.getHoverName().copy());
        previews.put(resource, stack.copyWithCount(1));
    }

    public ItemStack preview(ResourceId resource) { return previews.getOrDefault(resource, ItemStack.EMPTY).copy(); }

    public String name(ResourceId resource) {
        Component value = names.get(resource);
        return value == null ? resource.namespace() + ":" + resource.path() : value.getString();
    }
    public void omittedEntity() { omittedEntities++; }
    public int omittedEntities() { return omittedEntities; }
}
