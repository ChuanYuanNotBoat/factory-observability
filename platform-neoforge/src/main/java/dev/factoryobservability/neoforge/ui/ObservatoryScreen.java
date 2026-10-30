package dev.factoryobservability.neoforge.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.graph.Edge;
import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.resource.ResourceId;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.minecraft.observation.ObservationIndex;
import dev.factoryobservability.neoforge.ClientObservationHub;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A player-facing inventory helper. Technical evidence is available only on request. */
public final class ObservatoryScreen extends AssistantScreen {
    private enum Mode { CONTAINERS, ITEMS, FLOW }
    private final ClientObservationHub hub;
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();
    private EditBox search;
    private Button sort;
    private Mode mode = Mode.CONTAINERS;
    private boolean byQuantity;
    private int sourcePage;
    private int resourcePage;
    private int ticks;
    private UUID selected;
    private ResourceId selectedResource;
    private Edge removed;
    private String status = "";

    public ObservatoryScreen(ClientObservationHub hub) { super(text("title"), null); this.hub = hub; }

    @Override protected void init() {
        String query = search == null ? "" : search.getValue();
        button(text("help.open"), width - 118, 6, 62, 20, value -> minecraft.setScreen(new GettingStartedScreen(this)));
        button(text("close"), width - 52, 6, 40, 20, value -> onClose());
        button(text("tab.containers"), 12, 32, 94, 20, value -> changeMode(Mode.CONTAINERS));
        button(text("tab.items"), 110, 32, 76, 20, value -> changeMode(Mode.ITEMS));
        button(text("tab.flow"), 190, 32, 76, 20, value -> changeMode(Mode.FLOW));
        sort = button(text(byQuantity ? "sort.quantity" : "sort.recent"), width - 126, 58, 114, 20, value -> {
            byQuantity = !byQuantity;
            value.setMessage(text(byQuantity ? "sort.quantity" : "sort.recent"));
            sourcePage = 0;
            refreshRows();
        });
        sort.setTooltip(Tooltip.create(text("sort.tip")));
        search = new EditBox(font, 12, 58, width - 144, 20, text("filter"));
        search.setHint(text("filter"));
        search.setValue(query);
        search.setResponder(value -> { sourcePage = 0; resourcePage = 0; refreshRows(); });
        addRenderableWidget(search);
        layoutMode();
        refreshRows();
    }

    private int split() { return Math.max(155, (int) (width * 0.40)); }
    private int bottom() { return height - 28; }
    private int sourceSize() { return Math.max(1, (bottom() - 22 - 102) / 34); }
    private int resourceSize() { return Math.max(1, (bottom() - 27 - 130) / 24); }
    private int flowSize() { return Math.max(1, (bottom() - 24 - 116) / 46); }

    private void changeMode(Mode value) {
        mode = value;
        sourcePage = 0;
        resourcePage = 0;
        selected = null;
        selectedResource = null;
        status = "";
        search.setValue("");
        layoutMode();
        refreshRows();
    }

    private void layoutMode() {
        sort.visible = mode != Mode.FLOW;
        search.setWidth(width - (mode == Mode.FLOW ? 24 : 144));
    }

    private List<ObservationIndex.View> allRows() { return hub.session().map(session -> session.index().views()).orElse(List.of()); }

    private List<ObservationIndex.View> rows() {
        String query = search.getValue().toLowerCase(Locale.ROOT);
        return allRows().stream().filter(row -> mode == Mode.FLOW || row.node().kind() ==
                        (mode == Mode.ITEMS ? Node.Kind.ITEM_ENTITY : Node.Kind.STORAGE))
                .filter(row -> matches(row, query)).sorted(order()).toList();
    }

    private Comparator<ObservationIndex.View> order() {
        if (byQuantity) {
            return Comparator.<ObservationIndex.View>comparingDouble(row -> row.resources().values().stream()
                    .mapToDouble(ResourceStack::amount).sum()).reversed().thenComparing(this::recordName);
        }
        return Comparator.comparing(ObservationIndex.View::lastSeen).reversed().thenComparing(this::recordName);
    }

    private boolean matches(ObservationIndex.View row, String query) {
        return recordName(row).toLowerCase(Locale.ROOT).contains(query) || row.resources().keySet().stream().anyMatch(resource ->
                hub.name(resource).toLowerCase(Locale.ROOT).contains(query)
                || (resource.namespace() + ":" + resource.path()).contains(query));
    }

    String recordName(ObservationIndex.View row) {
        if (row.node().kind() == Node.Kind.ITEM_ENTITY) {
            return row.resources().isEmpty() ? text("waiting").getString() : hub.name(row.resources().keySet().iterator().next());
        }
        return row.node().labels().get("name");
    }

    static Component age(Instant time) {
        long seconds = Math.max(0, Duration.between(time, Instant.now()).toSeconds());
        return seconds < 5 ? text("age.now") : seconds < 60 ? text("age.seconds", seconds)
                : text("age.minutes", seconds / 60);
    }

    private Component state(ObservationIndex.View row) {
        if (row.evidence().quality() == DataQuality.UNKNOWN) { return text("waiting"); }
        if (row.evidence().coverage() < 1) { return text("state.partial"); }
        return text(row.visible() ? "state.visible" : "state.saved");
    }

    private ObservationIndex.View selectedView() {
        return allRows().stream().filter(row -> row.node().id().equals(selected)).findFirst().orElse(null);
    }

    private List<ResourceStack> resources(ObservationIndex.View row) {
        return row == null ? List.of() : row.resources().values().stream()
                .sorted(Comparator.comparing(stack -> hub.name(stack.resource()))).toList();
    }

    private <T extends AbstractWidget> T rowWidget(T widget) { rowWidgets.add(widget); return addRenderableWidget(widget); }

    private Button rowButton(Component label, int x, int y, int w, int h, Button.OnPress action) {
        return rowWidget(Button.builder(label, action).bounds(x, y, w, h).build());
    }

    private void refreshRows() {
        rowWidgets.forEach(this::removeWidget);
        rowWidgets.clear();
        if (search == null) { return; }
        if (mode == Mode.FLOW) { flowWidgets(); return; }
        List<ObservationIndex.View> rows = rows();
        sourcePage = Math.min(sourcePage, Math.max(0, (rows.size() - 1) / sourceSize()));
        if (rows.stream().noneMatch(row -> row.node().id().equals(selected))) {
            selected = rows.isEmpty() ? null : rows.get(sourcePage * sourceSize()).node().id();
            selectedResource = null;
            resourcePage = 0;
        }
        if (rows.isEmpty()) {
            rowButton(text(search.getValue().isEmpty() ? "help.go" : "filter.clear"), width / 2 - 94, bottom() - 26, 188, 20,
                    value -> { if (search.getValue().isEmpty()) { onClose(); } else { search.setValue(""); } });
            return;
        }
        int from = sourcePage * sourceSize();
        for (int i = from; i < Math.min(rows.size(), from + sourceSize()); i++) {
            var row = rows.get(i);
            Component subtitle = state(row).copy().append(" · ").append(age(row.lastSeen()));
            rowWidget(new IconRowButton(18, 102 + (i - from) * 34, split() - 28, 31,
                    Component.literal(recordName(row)), subtitle, "", () -> row.node().kind() == Node.Kind.STORAGE
                            ? new ItemStack(Items.CHEST) : hub.preview(row.resources().keySet().stream().findFirst().orElse(null)),
                    row.node().id().equals(selected), value -> {
                        selected = row.node().id(); selectedResource = null; resourcePage = 0; refreshRows();
                    }));
        }
        pager(18, bottom() - 19, split() - 28, sourcePage, rows.size(), sourceSize(), value -> { sourcePage = value; refreshRows(); });
        var current = selectedView();
        if (current == null) { return; }
        int right = split() + 10;
        rowButton(Component.literal("?"), width - 34, 88, 16, 16,
                value -> minecraft.setScreen(new RecordInfoScreen(this, hub, current))).setTooltip(Tooltip.create(text("info.title")));
        if (current.node().kind() == Node.Kind.STORAGE) {
            rowButton(text("rename"), width - 76, 88, 38, 16, value -> minecraft.setScreen(
                    new RenameRecordScreen(this, hub, selected, recordName(current))));
        }
        List<ResourceStack> resources = resources(current);
        resourcePage = Math.min(resourcePage, Math.max(0, (resources.size() - 1) / resourceSize()));
        int offset = resourcePage * resourceSize();
        for (int i = offset; i < Math.min(resources.size(), offset + resourceSize()); i++) {
            ResourceStack stack = resources.get(i);
            Component tip = text(stack.resource().qualifiers().isEmpty() ? "item.choose" : "item.variant");
            rowWidget(new IconRowButton(right, 130 + (i - offset) * 24, width - right - 18, 22,
                    Component.literal(hub.name(stack.resource())), tip, String.format(Locale.ROOT, "×%.0f", stack.amount()),
                    () -> hub.preview(stack.resource()), stack.resource().equals(selectedResource), value -> {
                        selectedResource = stack.resource(); refreshRows();
                    }));
        }
        pager(width - 100, 112, 82, resourcePage, resources.size(), resourceSize(), value -> { resourcePage = value; refreshRows(); });
        Button link = rowButton(text("link.start"), right, bottom() - 23, width - right - 18, 20, value -> {
            ResourceStack stack = current.resources().get(selectedResource);
            if (stack != null) { minecraft.setScreen(new LinkTargetScreen(this, hub, current, stack)); }
        });
        link.active = selectedResource != null && current.resources().containsKey(selectedResource);
        link.setTooltip(Tooltip.create(text(link.active ? "link.tip" : "link.select_item")));
    }

    private void pager(int x, int y, int w, int page, int count, int size, java.util.function.IntConsumer change) {
        rowButton(Component.literal("<"), x, y, 18, 16, value -> change.accept(page - 1)).active = page > 0;
        rowButton(Component.literal(">"), x + w - 18, y, 18, 16, value -> change.accept(page + 1)).active = (page + 1) * size < count;
    }

    private List<Edge> edges() {
        String query = search.getValue().toLowerCase(Locale.ROOT);
        return hub.session().map(session -> session.index().graph(session.worldId()).edges()).orElse(List.of()).stream()
                .filter(edge -> hub.name(edge.resource()).toLowerCase(Locale.ROOT).contains(query)
                        || allRows().stream().filter(row -> row.node().id().equals(edge.from()) || row.node().id().equals(edge.to()))
                                .anyMatch(row -> recordName(row).toLowerCase(Locale.ROOT).contains(query))).toList();
    }

    private void flowWidgets() {
        List<Edge> edges = edges();
        sourcePage = Math.min(sourcePage, Math.max(0, (edges.size() - 1) / flowSize()));
        if (edges.isEmpty()) {
            rowButton(text("flow.start"), width / 2 - 75, bottom() - 26, 150, 20, value -> changeMode(Mode.CONTAINERS));
        }
        int offset = sourcePage * flowSize();
        for (int i = offset; i < Math.min(edges.size(), offset + flowSize()); i++) {
            Edge edge = edges.get(i);
            ObservationIndex.View source = find(edge.from());
            ObservationIndex.View target = find(edge.to());
            if (source == null || target == null) { continue; }
            int y = 116 + (i - offset) * 46;
            Component label = Component.literal(recordName(source) + " → " + recordName(target));
            rowWidget(new IconRowButton(18, y, width - 70, 42, label,
                    text("flow.item", hub.name(edge.resource())), "", () -> hub.preview(edge.resource()), false, value -> {
                        changeMode(source.node().kind() == Node.Kind.STORAGE ? Mode.CONTAINERS : Mode.ITEMS);
                        selected = source.node().id(); selectedResource = edge.resource(); refreshRows();
                    }));
            rowButton(Component.literal("×"), width - 46, y + 10, 24, 22, value -> {
                hub.session().ifPresent(session -> session.index().removeEdge(edge.id()));
                removed = edge; status = text("flow.removed").getString(); refreshRows();
            }).setTooltip(Tooltip.create(text("flow.remove")));
        }
        pager(18, bottom() - 19, width - 36, sourcePage, edges.size(), flowSize(), value -> { sourcePage = value; refreshRows(); });
        if (removed != null) {
            rowButton(text("undo"), width - 70, height - 23, 58, 18, value -> {
                try { hub.session().orElseThrow().index().declareEdge(removed.from(), removed.to(), removed.resource());
                    status = text("flow.restored").getString(); }
                catch (IllegalArgumentException | IllegalStateException error) { status = text("link.unavailable").getString(); }
                removed = null; refreshRows();
            });
        }
    }

    private ObservationIndex.View find(UUID id) { return allRows().stream().filter(row -> row.node().id().equals(id)).findFirst().orElse(null); }

    void linked() { changeMode(Mode.FLOW); status = text("link.saved").getString(); }

    @Override public void tick() { if (++ticks % 20 == 0) { refreshRows(); } }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        background(graphics);
        int tabX = mode == Mode.CONTAINERS ? 12 : mode == Mode.ITEMS ? 110 : 190;
        graphics.fill(tabX, 52, tabX + (mode == Mode.CONTAINERS ? 94 : 76), 54, 0xFF70DCC0);
        List<ObservationIndex.View> rows = rows();
        if (mode == Mode.FLOW) {
            graphics.drawWordWrap(font, text(edges().isEmpty() ? "flow.empty" : "flow.hint"), 18, 88, width - 36, 0xBED1DD);
            for (int i = 0; i < Math.min(flowSize(), edges().size() - sourcePage * flowSize()); i++) {
                graphics.drawString(font, "→", width - 68, 131 + i * 46, 0x81D9BD);
            }
            graphics.drawCenteredString(font, text("page", sourcePage + 1, pages(edges().size(), flowSize())), width / 2, bottom() - 15, 0x9CAFBF);
        } else if (rows.isEmpty()) {
            graphics.fill(12, 84, width - 12, bottom(), 0xFF192B3A);
            if (!search.getValue().isEmpty()) {
                graphics.drawCenteredString(font, text("filter.none"), width / 2, 103, 0xCBD9E2);
            } else if (mode == Mode.ITEMS) {
                graphics.drawWordWrap(font, text("items.empty"), 28, 100, width - 56, 0xCBD9E2);
            } else {
                graphics.drawString(font, text("empty.title"), 24, 91, 0x9EE5CA);
                for (int i = 0; i < 3; i++) {
                    graphics.renderItem(new ItemStack(i == 0 ? Items.CHEST : i == 1 ? Items.BOOK : Items.IRON_INGOT), 24, 111 + i * 22);
                    graphics.drawString(font, text("help.step" + (i + 1)), 48, 115 + i * 22, 0xCBD9E2);
                }
            }
        } else {
            graphics.fill(12, 84, split() - 4, bottom(), 0xFF172A39);
            graphics.fill(split() + 4, 84, width - 12, bottom(), 0xFF172A39);
            graphics.drawString(font, text("records", rows.size()), 18, 90, 0x9FB7C7);
            var row = selectedView();
            if (row != null) {
                int right = split() + 10;
                line(graphics, recordName(row), right, 91, width - right - 82, 0xEDF6F9);
                line(graphics, state(row).copy().append(" · ").append(age(row.lastSeen())).getString(), right, 106,
                        width - right - 18, 0xAFCCDA);
                graphics.drawString(font, text("inventory.count", row.resources().size()), right, 117, 0xA7BECD);
                graphics.drawCenteredString(font, text("page", resourcePage + 1, pages(row.resources().size(), resourceSize())),
                        width - 59, 116, 0x9CAFBF);
                if (row.resources().isEmpty()) { graphics.drawWordWrap(font,
                        text(row.evidence().quality() == DataQuality.UNKNOWN ? "waiting" : "inventory.empty"), right,
                        143, width - right - 24, 0xCBD9E2); }
            }
            graphics.drawCenteredString(font, text("page", sourcePage + 1, pages(rows.size(), sourceSize())),
                    split() / 2 + 4, bottom() - 15, 0x9CAFBF);
        }
        String footer = status.isEmpty() ? text("session.only").getString() : status;
        line(graphics, footer, 12, height - 15, width - (removed == null ? 24 : 92), 0xA5BECD);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private static int pages(int count, int size) { return Math.max(1, (count + size - 1) / size); }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (y < 84) { return super.mouseScrolled(x, y, horizontal, vertical); }
        int direction = vertical > 0 ? -1 : 1;
        if (mode == Mode.FLOW || x < split()) {
            int count = mode == Mode.FLOW ? edges().size() : rows().size();
            int size = mode == Mode.FLOW ? flowSize() : sourceSize();
            sourcePage = Math.max(0, Math.min(pages(count, size) - 1, sourcePage + direction));
        } else {
            resourcePage = Math.max(0, Math.min(pages(resources(selectedView()).size(), resourceSize()) - 1, resourcePage + direction));
        }
        refreshRows();
        return true;
    }
}
