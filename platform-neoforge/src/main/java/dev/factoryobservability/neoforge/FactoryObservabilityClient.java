package dev.factoryobservability.neoforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiFunction;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;

import dev.factoryobservability.compat.api.ProviderRegistry;
import dev.factoryobservability.minecraft.observation.WorldIdentityStore;
import dev.factoryobservability.neoforge.ui.ObservatoryScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod(value = FactoryObservabilityClient.MOD_ID, dist = Dist.CLIENT)
public final class FactoryObservabilityClient {
    public static final String MOD_ID = "factory_observability";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final KeyMapping OPEN = new KeyMapping("key.factory_observability.open",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.factory_observability");
    private final ClientObservationHub hub;
    private final ProviderRegistry providers;
    private final GenericObservationProvider genericProvider;
    private static FactoryObservabilityClient instance;

    /** Internal integration seam; not a stable third-party API. */
    public static ClientObservationHub observations() {
        if (instance == null) { throw new IllegalStateException("Client has not initialized"); }
        return instance.hub;
    }

    public FactoryObservabilityClient(IEventBus modBus) {
        ClientSettings settings;
        try { settings = ClientSettings.load(FMLPaths.CONFIGDIR.get().resolve("factory-observability.json")); }
        catch (IOException | RuntimeException error) {
            LOGGER.warn("Cannot read Factory Observability config; using bounded defaults", error);
            settings = ClientSettings.defaults();
        }
        hub = new ClientObservationHub(settings);
        BiFunction<String, String, String> identities;
        try {
            var store = new WorldIdentityStore(FMLPaths.GAMEDIR.get().resolve("factory-observability"));
            identities = store::identify;
        } catch (IOException error) {
            LOGGER.warn("World identity storage is unavailable; using temporary session identities", error);
            String salt = UUID.randomUUID().toString();
            identities = (kind, location) -> UUID.nameUUIDFromBytes((salt + kind + location).getBytes(StandardCharsets.UTF_8)).toString();
        }
        providers = new ProviderRegistry((id, error) -> LOGGER.warn("Observation provider {} failed", id, error));
        genericProvider = new GenericObservationProvider(hub, settings, identities);
        providers.register(genericProvider);
        providers.start(hub);
        modBus.addListener(this::registerKeys);
        NeoForge.EVENT_BUS.addListener(this::keys);
        NeoForge.EVENT_BUS.addListener(this::containerButton);
        NeoForge.EVENT_BUS.addListener(this::commands);
        instance = this;
        LOGGER.info("Factory Observability client initialized; server telemetry is optional and absent in V0");
    }

    private void registerKeys(RegisterKeyMappingsEvent event) { event.register(OPEN); }

    private void keys(ClientTickEvent.Post event) {
        while (OPEN.consumeClick()) {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) { open(); }
        }
    }

    private void containerButton(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> container && Minecraft.getInstance().player != null
                && container.getMenu() != Minecraft.getInstance().player.inventoryMenu) {
            event.addListener(Button.builder(Component.translatable("factory_observability.container.open"), button -> open())
                    .bounds(8, 8, 112, 20)
                    .tooltip(Tooltip.create(Component.translatable("factory_observability.container.tip"))).build());
        }
    }

    public static Component openKeyLabel() { return OPEN.getTranslatedKeyMessage().copy(); }

    private void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("factoryobs").executes(context -> {
            Minecraft.getInstance().execute(this::open);
            return 1;
        }));
    }

    private void open() {
        Minecraft mc = Minecraft.getInstance();
        genericProvider.captureCurrentMenu();
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) { mc.player.closeContainer(); }
        mc.setScreen(new ObservatoryScreen(hub));
    }
}
