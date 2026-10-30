package dev.factoryobservability.neoforge;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.BiFunction;

import dev.factoryobservability.compat.api.AdapterCapability;
import dev.factoryobservability.compat.api.ObservationProvider;
import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.evidence.Evidence;
import dev.factoryobservability.core.evidence.Provenance;
import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.observation.EntityPosition;
import dev.factoryobservability.core.observation.Observation;
import dev.factoryobservability.core.observation.ObservationPayload;
import dev.factoryobservability.core.observation.ObservationSink;
import dev.factoryobservability.core.observation.ResourceSnapshot;
import dev.factoryobservability.core.observation.SubjectRef;
import dev.factoryobservability.core.observation.VisibilityEnded;
import dev.factoryobservability.core.observation.VisibilityStarted;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.core.resource.ResourceUnit;
import dev.factoryobservability.minecraft.observation.MenuSessionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Uses only client-synchronized state; never queries server containers or capabilities. */
public final class GenericObservationProvider implements ObservationProvider {
    private static final Evidence SYNC = new Evidence(Provenance.CLIENT_SYNC, DataQuality.OBSERVED, 1.0, 1.0);
    private static final Evidence UNKNOWN = new Evidence(Provenance.CLIENT_OBSERVATION, DataQuality.UNKNOWN, 0.0, 0.0);
    private final ClientObservationHub hub;
    private final ClientSettings settings;
    private final BiFunction<String, String, String> identities;
    private final Map<UUID, TrackedItem> items = new HashMap<>();
    private final Consumer<ClientTickEvent.Post> tickListener = event -> tick();
    private final Consumer<ClientPlayerNetworkEvent.LoggingOut> logoutListener = event -> logout();
    private final Consumer<EntityJoinLevelEvent> joinListener = this::join;
    private final Consumer<EntityLeaveLevelEvent> leaveListener = this::leave;
    private ObservationSink sink;
    private ClientLevel level;
    private AbstractContainerMenu menu;
    private MenuSessionTracker menuTracker;
    private List<Slot> menuSlots = List.of();
    private int totalMenuSlots;
    private long ticks;

    private static final class TrackedItem {
        private final ItemEntity entity;
        private final SubjectRef subject;
        private ResourceId previous;
        private boolean visible;
        private TrackedItem(ItemEntity entity, SubjectRef subject) { this.entity = entity; this.subject = subject; }
    }

    public GenericObservationProvider(ClientObservationHub hub, ClientSettings settings,
            BiFunction<String, String, String> identities) {
        this.hub = hub;
        this.settings = settings;
        this.identities = identities;
    }

    @Override public String id() { return "factory_observability:generic_client"; }
    @Override public Set<AdapterCapability> capabilities() { return Set.of(AdapterCapability.ITEM_STORAGE, AdapterCapability.TRANSPORT_POSITION); }

    @Override public void start(ObservationSink target) {
        if (sink != null) { throw new IllegalStateException("Provider is already running"); }
        sink = java.util.Objects.requireNonNull(target, "target");
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, tickListener);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, logoutListener);
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, joinListener);
        NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, leaveListener);
    }

    @Override public void stop() {
        NeoForge.EVENT_BUS.unregister(tickListener);
        NeoForge.EVENT_BUS.unregister(logoutListener);
        NeoForge.EVENT_BUS.unregister(joinListener);
        NeoForge.EVENT_BUS.unregister(leaveListener);
        logout();
        sink = null;
    }

    private void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { return; }
        try {
            ensureLevel(mc.level);
            ticks++;
            Instant now = Instant.now();
            AbstractContainerMenu current = mc.screen instanceof AbstractContainerScreen<?> container ? container.getMenu() : null;
            if (current != menu || (current != null && menuTracker == null && !menuSlots.isEmpty())) {
                endMenu(now, VisibilityEnded.Reason.MENU_CLOSED);
                if (current != null) { beginMenu(current, now); }
            }
            if (ticks % settings.samplePeriodTicks() == 0) {
                sampleMenu(now);
                for (TrackedItem item : new ArrayList<>(items.values())) { sampleItem(item, now); }
            }
            hub.session().ifPresent(session -> session.bus().drain(settings.eventsPerTick()));
        } catch (RuntimeException error) {
            if (menuTracker != null) { menuTracker.interrupt(Instant.now()); }
            failure("client observation", error);
            hub.session().ifPresent(session -> session.bus().drain(settings.eventsPerTick()));
        }
    }

    /** Capture the visible menu before the helper closes it; no timed wait is needed. */
    public void captureCurrentMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !(mc.screen instanceof AbstractContainerScreen<?> container)) { return; }
        try {
            ensureLevel(mc.level);
            Instant now = Instant.now();
            if (menu != container.getMenu()) {
                endMenu(now, VisibilityEnded.Reason.MENU_CLOSED);
                beginMenu(container.getMenu(), now);
            }
            sampleMenu(now);
            endMenu(now, VisibilityEnded.Reason.MENU_CLOSED);
            hub.session().ifPresent(session -> session.bus().drain(settings.queueCapacity()));
        } catch (RuntimeException error) {
            if (menuTracker != null) { menuTracker.interrupt(Instant.now()); }
            failure("menu capture", error);
        }
    }

    private void ensureLevel(ClientLevel current) {
        if (level == current) { return; }
        Instant now = Instant.now();
        endMenu(now, VisibilityEnded.Reason.SESSION_ENDED);
        for (TrackedItem item : items.values()) { emit(item.subject, now, new VisibilityEnded(VisibilityEnded.Reason.SESSION_ENDED), UNKNOWN); }
        items.clear();
        hub.session().ifPresent(session -> session.bus().drain(Integer.MAX_VALUE));
        Minecraft mc = Minecraft.getInstance();
        String worldId;
        if (mc.getSingleplayerServer() != null) {
            worldId = identities.apply("singleplayer", mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString());
        } else if (mc.getCurrentServer() != null) {
            ServerAddress address = ServerAddress.parseString(mc.getCurrentServer().ip);
            worldId = identities.apply("multiplayer", address.getHost().toLowerCase(Locale.ROOT) + ":" + address.getPort());
        } else {
            worldId = identities.apply("session", UUID.randomUUID().toString());
        }
        hub.beginWorld(worldId);
        level = current;
        // One bounded seed pass per level, not a per-tick entity scan.
        int examined = 0;
        for (Entity entity : current.entitiesForRendering()) {
            if (++examined > 1024) { break; }
            if (entity instanceof ItemEntity item) { track(item, now); }
        }
    }

    private void beginMenu(AbstractContainerMenu current, Instant now) {
        menu = current;
        menuSlots = current.slots.stream().filter(slot -> slot.container != Minecraft.getInstance().player.getInventory())
                .limit(settings.maximumMenuSlots()).toList();
        totalMenuSlots = (int) current.slots.stream()
                .filter(slot -> slot.container != Minecraft.getInstance().player.getInventory()).count();
        if (menuSlots.isEmpty()) { return; }
        SubjectRef subject = subject("menu/" + UUID.randomUUID());
        String title = Minecraft.getInstance().screen.getTitle().getString();
        hub.session().orElseThrow().index().register(subject, title, Node.Kind.STORAGE, now);
        menuTracker = new MenuSessionTracker(subject, sink);
    }

    private void sampleMenu(Instant now) {
        if (menuTracker == null) { return; }
        List<Slot> currentSlots = menu.slots.stream().filter(slot -> slot.container != Minecraft.getInstance().player.getInventory())
                .limit(settings.maximumMenuSlots()).toList();
        int currentTotal = (int) menu.slots.stream().filter(slot -> slot.container != Minecraft.getInstance().player.getInventory()).count();
        if (!currentSlots.equals(menuSlots) || currentTotal != totalMenuSlots) {
            AbstractContainerMenu current = menu;
            endMenu(now, VisibilityEnded.Reason.LOST_VISIBILITY);
            beginMenu(current, now);
        }
        if (menuTracker == null) { return; }
        List<ResourceStack> contents = new ArrayList<>();
        for (Slot slot : menuSlots) {
            var stack = slot.getItem();
            if (!stack.isEmpty()) {
                ResourceStack resource = ItemIdentityMapper.map(stack, level.registryAccess());
                contents.add(resource);
                hub.remember(resource.resource(), stack);
            }
        }
        menuTracker.sample(now, contents, menuSlots.size(), totalMenuSlots);
    }

    private void endMenu(Instant time, VisibilityEnded.Reason reason) {
        if (menuTracker != null) { menuTracker.end(time, reason); }
        menuTracker = null;
        menu = null;
        menuSlots = List.of();
    }

    private void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getLevel() == level && event.getEntity() instanceof ItemEntity item) {
            try { track(item, Instant.now()); }
            catch (RuntimeException error) { failure("entity registration", error); }
        }
    }

    private void track(ItemEntity item, Instant now) {
        if (items.containsKey(item.getUUID())) { return; }
        if (items.size() >= settings.maximumEntities()) { hub.omittedEntity(); return; }
        SubjectRef subject = subject("item-entity/" + item.getUUID());
        hub.session().orElseThrow().index().register(subject, item.getName().getString(), Node.Kind.ITEM_ENTITY, now);
        TrackedItem tracked = new TrackedItem(item, subject);
        items.put(item.getUUID(), tracked);
        sampleItem(tracked, now);
    }

    private void sampleItem(TrackedItem tracked, Instant now) {
        try {
            if (tracked.entity.isRemoved() || tracked.entity.level() != level) {
                emit(tracked.subject, now, new VisibilityEnded(VisibilityEnded.Reason.LOST_VISIBILITY), UNKNOWN);
                items.remove(tracked.entity.getUUID());
                return;
            }
            var stack = tracked.entity.getItem();
            if (stack.isEmpty()) {
                if (tracked.visible) { emit(tracked.subject, now, new VisibilityEnded(VisibilityEnded.Reason.LOST_VISIBILITY), UNKNOWN); }
                tracked.visible = false;
                return; // Empty during synchronization is not proof of destruction.
            }
            ResourceStack resource = ItemIdentityMapper.map(stack, level.registryAccess());
            hub.remember(resource.resource(), stack);
            hub.session().orElseThrow().index().name(tracked.subject, stack.getHoverName().getString());
            emit(tracked.subject, now, new VisibilityStarted(), SYNC);
            tracked.visible = true;
            if (tracked.previous != null && !tracked.previous.equals(resource.resource())) {
                emit(tracked.subject, now, new ResourceSnapshot(new ResourceStack(tracked.previous, 0, ResourceUnit.ITEMS)), SYNC);
            }
            emit(tracked.subject, now, new ResourceSnapshot(resource), SYNC);
            emit(tracked.subject, now, new EntityPosition(tracked.entity.getX(), tracked.entity.getY(), tracked.entity.getZ()), SYNC);
            tracked.previous = resource.resource();
        } catch (RuntimeException error) {
            if (tracked.visible) { emit(tracked.subject, now, new VisibilityEnded(VisibilityEnded.Reason.SOURCE_ERROR), UNKNOWN); }
            tracked.visible = false;
            failure("item observation", error);
        }
    }

    private void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) { return; }
        TrackedItem item = items.remove(event.getEntity().getUUID());
        if (item != null) {
            var reason = event.getEntity().getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK
                    ? VisibilityEnded.Reason.CHUNK_UNLOADED : VisibilityEnded.Reason.LOST_VISIBILITY;
            emit(item.subject, Instant.now(), new VisibilityEnded(reason), UNKNOWN);
        }
    }

    private SubjectRef subject(String id) {
        return new SubjectRef(hub.session().orElseThrow().worldId(),
                Minecraft.getInstance().level.dimension().location().toString(), id);
    }

    private void emit(SubjectRef subject, Instant time, ObservationPayload payload, Evidence evidence) {
        sink.emit(new Observation(UUID.randomUUID(), time, subject, payload, evidence));
    }

    private void failure(String operation, RuntimeException error) {
        hub.session().ifPresent(session -> session.failure(operation + ": " + error.getClass().getSimpleName()));
        FactoryObservabilityClient.LOGGER.debug("Factory observation failed during {}", operation, error);
    }

    private void logout() {
        if (sink == null) { return; }
        Instant now = Instant.now();
        endMenu(now, VisibilityEnded.Reason.SESSION_ENDED);
        items.values().forEach(item -> emit(item.subject, now, new VisibilityEnded(VisibilityEnded.Reason.SESSION_ENDED), UNKNOWN));
        hub.session().ifPresent(session -> session.bus().drain(Integer.MAX_VALUE));
        items.clear();
        level = null;
        ticks = 0;
        hub.clear();
    }
}
