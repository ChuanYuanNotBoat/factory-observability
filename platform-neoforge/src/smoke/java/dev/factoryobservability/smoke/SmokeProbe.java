package dev.factoryobservability.smoke;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.metric.MetricKey;
import dev.factoryobservability.core.metric.TimeRange;
import dev.factoryobservability.core.observation.VisibilityEnded;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceKind;
import dev.factoryobservability.core.resource.ResourceUnit;
import dev.factoryobservability.minecraft.observation.ObservationIndex;
import dev.factoryobservability.neoforge.FactoryObservabilityClient;
import dev.factoryobservability.neoforge.ClientObservationHub;
import dev.factoryobservability.neoforge.ClientSettings;
import dev.factoryobservability.neoforge.ui.GettingStartedScreen;
import dev.factoryobservability.neoforge.ui.LinkTargetScreen;
import dev.factoryobservability.neoforge.ui.ObservatoryScreen;
import dev.factoryobservability.neoforge.ui.RenameRecordScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Separate development addon, never shipped in the installable mod. */
@Mod(value = "factory_observability_smoke", dist = Dist.CLIENT)
public final class SmokeProbe {
    private final long deadline = System.nanoTime() + 240_000_000_000L;
    private int stage;
    private int waitTicks;
    private int entryTicks;
    private int capture;
    private UUID entityId;
    private BlockPos chestPosition;
    private ObservationIndex.View menu;
    private ObservationIndex.View entity;
    private ObservationIndex.View destination;
    private boolean reloading;
    private final ResourceId iron = new ResourceId("minecraft", "iron_ingot", ResourceKind.ITEM);
    private final ResourceId gold = new ResourceId("minecraft", "gold_ingot", ResourceKind.ITEM);

    public SmokeProbe() { NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::tick); }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (System.nanoTime() > deadline) { throw new IllegalStateException("Factory smoke timed out at stage " + stage); }
        var hub = FactoryObservabilityClient.observations();
        if (stage >= 6) { mc.getToasts().clear(); }
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            require(FactoryObservabilityClient.class.getProtectionDomain().getCodeSource().getLocation().toString().contains(".jar"),
                    "The production client must load from the packaged mod JAR");
            stage = 1;
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
            mc.createWorldOpenFlows().createFreshLevel("factoryobs-smoke-" + System.currentTimeMillis(),
                    new LevelSettings("Factory observation QA", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                            WorldDataConfiguration.DEFAULT), new WorldOptions(42, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), new TitleScreen());
        } else if (stage == 1 && mc.player != null && mc.level != null && mc.getSingleplayerServer() != null) {
            stage = 2;
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer();
                var player = server.getPlayerList().getPlayers().getFirst();
                var level = player.serverLevel();
                BlockPos position = player.blockPosition().offset(2, 0, 0);
                level.setBlockAndUpdate(position, Blocks.CHEST.defaultBlockState());
                var chest = (ChestBlockEntity) level.getBlockEntity(position);
                chest.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
                chest.setItem(1, new ItemStack(Items.IRON_INGOT, 16));
                var named = new ItemStack(Items.IRON_INGOT, 4);
                named.set(DataComponents.CUSTOM_NAME, Component.literal("QA component variant"));
                chest.setItem(2, named);
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 64));
                var dropped = new ItemEntity(level, player.getX() + 3, player.getY() + 1, player.getZ() + 3,
                        new ItemStack(Items.COBBLESTONE, 5));
                dropped.setNoGravity(true);
                dropped.setPickUpDelay(32767);
                level.addFreshEntity(dropped);
                player.openMenu(new SimpleMenuProvider((id, inventory, who) -> ChestMenu.threeRows(id, inventory, chest),
                        Component.literal("Factory smoke chest")));
                mc.execute(() -> { entityId = dropped.getUUID(); chestPosition = position; stage = 3; });
            });
        } else if (stage == 3 && hub.session().isPresent()) {
            List<ObservationIndex.View> rows = hub.session().orElseThrow().index().views();
            menu = rows.stream().filter(row -> row.node().kind() == Node.Kind.STORAGE && row.visible()
                    && row.resources().containsKey(iron) && row.resources().get(iron).amount() == 48
                    && row.resources().size() == 2).findFirst().orElse(null);
            entity = rows.stream().filter(row -> row.node().kind() == Node.Kind.ITEM_ENTITY && row.visible()
                    && row.node().subject().id().contains(entityId.toString())).findFirst().orElse(null);
            if (menu != null && entity != null) {
                require(entity.node().labels().get("name").equals(new ItemStack(Items.COBBLESTONE).getHoverName().getString()),
                        "An item entity kept its pre-synchronization empty-stack name");
                require(menu.resources().keySet().stream().noneMatch(id -> id.path().equals("diamond")), "Player inventory was included");
                FactoryObservabilityClient.LOGGER.info("FACTORY SMOKE: menu inventory verified; components separated and player slots excluded");
                stage = 4;
                mc.getSingleplayerServer().execute(() -> {
                    var chest = (ChestBlockEntity) mc.getSingleplayerServer().overworld().getBlockEntity(chestPosition);
                    chest.setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
                    chest.setItem(1, ItemStack.EMPTY);
                    chest.setChanged();
                });
            }
        } else if (stage == 4) {
            var session = hub.session().orElseThrow();
            var row = session.index().views().stream().filter(value -> value.node().id().equals(menu.node().id())).findFirst().orElseThrow();
            if (row.resources().containsKey(gold) && !row.resources().containsKey(iron)) {
                Instant now = Instant.now();
                var samples = session.metrics().query(menu.node().subject(), MetricKey.inventory(iron, ResourceUnit.ITEMS),
                        new TimeRange(now.minusSeconds(300), now.plusSeconds(1)));
                require(samples.getLast().value() == 0, "Replaced resource was not explicitly cleared");
                FactoryObservabilityClient.LOGGER.info("FACTORY SMOKE: replacement verified");
                mc.player.closeContainer();
                mc.setScreen(null);
                stage = 5;
                mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().overworld().getEntity(entityId).discard());
            }
        } else if (stage == 5) {
            var session = hub.session().orElseThrow();
            var rows = session.index().views();
            var chest = rows.stream().filter(value -> value.node().id().equals(menu.node().id())).findFirst().orElseThrow();
            var dropped = rows.stream().filter(value -> value.node().id().equals(entity.node().id())).findFirst().orElseThrow();
            if (!chest.visible() && !dropped.visible() && chest.ended().orElse(null) == VisibilityEnded.Reason.MENU_CLOSED) {
                require(chest.resources().get(gold).amount() == 3, "Closing menu manufactured a zero");
                require(dropped.resources().values().iterator().next().amount() == 5, "Entity disappearance manufactured a zero");
                require(dropped.ended().isPresent(), "Entity disappearance was not censored");
                require(session.failures().isEmpty() && session.bus().droppedCount() == 0, "Observation pipeline lost data");
                FactoryObservabilityClient.LOGGER.info("FACTORY SMOKE: censored lifecycle verified");
                stage = 9;
                mc.getSingleplayerServer().execute(() -> {
                    var server = mc.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    BlockPos position = chestPosition.offset(2, 0, 0);
                    server.overworld().setBlockAndUpdate(position, Blocks.CHEST.defaultBlockState());
                    var target = (ChestBlockEntity) server.overworld().getBlockEntity(position);
                    target.setItem(0, new ItemStack(Items.COAL, 8));
                    player.openMenu(new SimpleMenuProvider((id, inventory, who) -> ChestMenu.threeRows(id, inventory, target),
                            Component.literal("Destination smoke chest")));
                });
            }
        } else if (stage == 9 && mc.screen instanceof AbstractContainerScreen<?> container
                && container.getTitle().getString().equals("Destination smoke chest")
                && container.getMenu().getSlot(0).getItem().is(Items.COAL)) {
            if (++entryTicks < 8) { return; }
            Screenshot.grab(FMLPaths.GAMEDIR.get().toFile(), "factoryobs-container-entry.png", mc.getMainRenderTarget(), component -> {});
            press("container.open");
            require(mc.screen instanceof ObservatoryScreen, "Container shortcut did not open the helper");
            destination = hub.session().orElseThrow().index().views().stream()
                    .filter(row -> row.node().labels().get("name").equals("Destination smoke chest")).findFirst().orElseThrow();
            require(destination.resources().values().stream().anyMatch(stack -> stack.resource().path().equals("coal") && stack.amount() == 8),
                    "Container shortcut did not capture the visible contents");
            pressLiteral("Factory smoke chest");
            press("rename");
            require(mc.screen instanceof RenameRecordScreen, "Rename action did not open the name editor");
            var keyEvent = new ScreenEvent.KeyPressed.Pre(mc.screen, org.lwjgl.glfw.GLFW.GLFW_KEY_O, 0, 0);
            NeoForge.EVENT_BUS.post(keyEvent);
            require(!keyEvent.isCanceled() && mc.screen instanceof RenameRecordScreen, "The O shortcut hijacked text input");
            editBox().setValue("Furnace output");
            press("save");
            menu = hub.session().orElseThrow().index().views().stream()
                    .filter(row -> row.node().id().equals(menu.node().id())).findFirst().orElseThrow();
            require(menu.node().labels().get("name").equals("Furnace output") && menu.resources().containsKey(gold),
                    "Renaming lost record identity or its inventory");
            editBox().setValue("does-not-match-any-record");
            require(findButton("filter.clear").active, "No-results search did not offer a clear action");
            press("filter.clear");
            pressLiteral("Furnace output");
            pressLiteral(hub.name(gold));
            press("link.start");
            require(mc.screen instanceof LinkTargetScreen && !findButton("link.confirm").active,
                    "Destination flow must require selecting a destination");
            press("cancel");
            require(hub.session().orElseThrow().index().graph(hub.session().orElseThrow().worldId()).edges().isEmpty(),
                    "Cancel unexpectedly created a destination");
            press("link.start");
            pressLiteral("Destination smoke chest");
            require(findButton("link.confirm").active, "Selecting a target did not enable confirmation");
            press("link.confirm");
            var session = hub.session().orElseThrow();
            var edge = session.index().graph(session.worldId()).edges().getFirst();
            require(edge.from().equals(menu.node().id()) && edge.to().equals(destination.node().id()) && edge.resource().equals(gold),
                    "Guided destination flow selected the wrong source, item or destination");
            pressLiteral("×");
            require(session.index().graph(session.worldId()).edges().isEmpty(), "Removing a destination failed");
            press("undo");
            require(session.index().graph(session.worldId()).edges().size() == 1, "Undo did not restore the destination");
            FactoryObservabilityClient.LOGGER.info("FACTORY SMOKE: helper interactions verified; shortcut, rename, text input, search, guided destinations and undo");
            stage = 6;
        } else if (stage == 6 && !reloading) {
            if (capture == 11) {
                FactoryObservabilityClient.LOGGER.info("FACTORY SMOKE PASSED; twelve helper screenshots captured");
                stage = 7;
                mc.stop();
                return;
            }
            reloading = true;
            String language = capture < 4 || capture >= 8 ? "zh_cn" : "en_us";
            int scale = capture >= 8 ? 3 : capture % 4 < 2 ? 2 : 3;
            mc.options.languageCode = language;
            mc.getLanguageManager().setSelected(language);
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
            mc.reloadResourcePacks().thenRun(() -> mc.execute(() -> {
                var screen = new ObservatoryScreen(hub);
                if (capture == 8) { mc.setScreen(new ObservatoryScreen(new ClientObservationHub(ClientSettings.defaults()))); }
                else if (capture == 9) { mc.setScreen(new GettingStartedScreen(screen)); }
                else if (capture == 10) { mc.setScreen(new LinkTargetScreen(screen, hub, menu, menu.resources().get(gold))); }
                else {
                    mc.setScreen(screen);
                    if (capture % 2 == 1) { press("tab.flow"); }
                    else { pressLiteral("Furnace output"); }
                }
                waitTicks = 15;
                stage = 8;
            }));
        } else if (stage == 8 && --waitTicks <= 0 && mc.getOverlay() == null) {
            String name = capture == 8 ? "factoryobs-onboarding.png" : capture == 9 ? "factoryobs-help.png"
                    : capture == 10 ? "factoryobs-destination.png" : "factoryobs-" + (capture < 4 ? "zh_cn" : "en_us")
                            + "-scale" + (capture % 4 < 2 ? 2 : 3) + (capture % 2 == 0 ? "-list.png" : "-graph.png");
            mc.getToasts().clear();
            Screenshot.grab(FMLPaths.GAMEDIR.get().toFile(), name, mc.getMainRenderTarget(), component -> {});
            capture++;
            reloading = false;
            stage = 6;
        }
    }

    private static Button findButton(String key) {
        return findLiteral(Component.translatable("factory_observability." + key).getString());
    }

    private static Button findLiteral(String label) {
        return Minecraft.getInstance().screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(label)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing helper button: " + label));
    }

    private static void press(String key) { require(findButton(key).active, "Inactive helper button: " + key); findButton(key).onPress(); }
    private static void pressLiteral(String label) { require(findLiteral(label).active, "Inactive helper button: " + label); findLiteral(label).onPress(); }
    private static EditBox editBox() {
        return Minecraft.getInstance().screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
    }

    private static void require(boolean valid, String message) {
        if (!valid) { throw new IllegalStateException("Factory smoke failed: " + message); }
    }
}
