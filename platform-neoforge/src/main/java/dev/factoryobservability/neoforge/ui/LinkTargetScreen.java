package dev.factoryobservability.neoforge.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.minecraft.observation.ObservationIndex;
import dev.factoryobservability.neoforge.ClientObservationHub;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LinkTargetScreen extends AssistantScreen {
    private final ObservatoryScreen owner;
    private final ClientObservationHub hub;
    private final ObservationIndex.View source;
    private final ResourceStack resource;
    private final List<AbstractWidget> rows = new ArrayList<>();
    private UUID selected;
    private int page;
    private String error = "";

    public LinkTargetScreen(ObservatoryScreen parent, ClientObservationHub hub, ObservationIndex.View source, ResourceStack resource) {
        super(text("link.title"), parent);
        owner = parent;
        this.hub = hub;
        this.source = source;
        this.resource = resource;
    }

    private List<ObservationIndex.View> targets() {
        return hub.session().stream().flatMap(session -> session.index().views().stream())
                .filter(row -> row.node().kind() == Node.Kind.STORAGE && !row.node().id().equals(source.node().id()))
                .sorted(Comparator.comparing(ObservationIndex.View::lastSeen).reversed()).toList();
    }

    private int size() { return Math.max(1, (height - 145) / 34); }

    @Override protected void init() {
        button(text("cancel"), 12, height - 26, 76, 20, value -> onClose());
        rebuildChoices();
    }

    private void rebuildChoices() {
        rows.forEach(this::removeWidget);
        rows.clear();
        List<ObservationIndex.View> targets = targets();
        page = Math.min(page, Math.max(0, (targets.size() - 1) / size()));
        for (int i = page * size(); i < Math.min(targets.size(), (page + 1) * size()); i++) {
            var target = targets.get(i);
            rows.add(addRenderableWidget(new IconRowButton(18, 86 + (i - page * size()) * 34, width - 36, 31,
                    net.minecraft.network.chat.Component.literal(owner.recordName(target)), ObservatoryScreen.age(target.lastSeen()),
                    "", () -> new ItemStack(Items.CHEST), target.node().id().equals(selected), value -> {
                        selected = target.node().id(); rebuildChoices();
                    })));
        }
        Button confirm = Button.builder(text("link.confirm"), value -> {
            try {
                hub.session().orElseThrow().index().declareEdge(source.node().id(), selected, resource.resource());
                owner.linked();
                minecraft.setScreen(owner);
            } catch (IllegalArgumentException | IllegalStateException problem) { error = text("link.unavailable").getString(); }
        }).bounds(width - 180, height - 26, 168, 20).build();
        confirm.active = selected != null && targets.stream().anyMatch(row -> row.node().id().equals(selected));
        rows.add(addRenderableWidget(confirm));
        rows.add(addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal("<"), value -> { page--; rebuildChoices(); })
                .bounds(width / 2 - 52, height - 49, 22, 16).build()));
        rows.get(rows.size() - 1).active = page > 0;
        rows.add(addRenderableWidget(Button.builder(net.minecraft.network.chat.Component.literal(">"), value -> { page++; rebuildChoices(); })
                .bounds(width / 2 + 30, height - 49, 22, 16).build()));
        rows.get(rows.size() - 1).active = (page + 1) * size() < targets.size();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        background(graphics);
        graphics.renderItem(hub.preview(resource.resource()), 18, 36);
        line(graphics, text("link.from", owner.recordName(source), hub.name(resource.resource())).getString(), 42, 40, width - 60, 0x9EE5CA);
        graphics.drawWordWrap(font, text(targets().isEmpty() ? "link.need_box" : "link.choose"), 18, 62, width - 36, 0xBED1DD);
        graphics.drawCenteredString(font, text("page", page + 1, Math.max(1, (targets().size() + size() - 1) / size())),
                width / 2, height - 45, 0x9CAFBF);
        if (!error.isEmpty()) { line(graphics, error, 100, height - 18, width - 290, 0xF0BD83); }
        super.render(graphics, mouseX, mouseY, delta);
    }
}
