package dev.factoryobservability.neoforge.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.core.metric.TimeRange;
import dev.factoryobservability.core.resource.ResourceStack;
import dev.factoryobservability.minecraft.observation.ObservationIndex;
import dev.factoryobservability.minecraft.observation.ObservationSession;
import dev.factoryobservability.neoforge.ClientObservationHub;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** List and graph views share exactly the same node IDs and selected resource. */
public final class ObservatoryScreen extends Screen {
    private final ClientObservationHub hub;
    private final Map<UUID, int[]> hitboxes = new HashMap<>();
    private EditBox search;
    private boolean graph;
    private boolean sortByQuantity;
    private int page;
    private int resourceIndex;
    private int detailScroll;
    private UUID selected;
    private UUID linkFrom;
    private ResourceStack linkResource;
    private String status = "";

    public ObservatoryScreen(ClientObservationHub hub) {
        super(text("title"));
        this.hub = hub;
    }

    private static Component text(String key, Object... args) { return Component.translatable("factory_observability." + key, args); }

    @Override protected void init() {
        String previousSearch = search == null ? "" : search.getValue();
        search = new EditBox(font, 12, 32, Math.max(70, width - 190), 20, text("filter"));
        search.setHint(text("filter"));
        search.setValue(previousSearch);
        search.setResponder(value -> { page = 0; selected = null; resourceIndex = 0; detailScroll = 0; });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(text(graph ? "view.graph" : "view.list"), button -> {
            graph = !graph;
            page = 0;
            button.setMessage(text(graph ? "view.graph" : "view.list"));
        }).bounds(width - 172, 32, 78, 20).build());
        addRenderableWidget(Button.builder(text("sort"), button -> { sortByQuantity = !sortByQuantity; page = 0; })
                .bounds(width - 88, 32, 76, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> page = Math.max(0, page - 1))
                .bounds(12, height - 48, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            if ((page + 1) * pageSize() < rows().size()) { page++; }
        }).bounds(40, height - 48, 24, 20).build());
        addRenderableWidget(Button.builder(text("resource.next"), button -> resourceIndex++)
                .bounds(split() + 8, height - 48, width - split() - 20, 20).build());
        addRenderableWidget(Button.builder(text("close"), button -> onClose()).bounds(12, height - 24, 70, 20).build());
        addRenderableWidget(Button.builder(text("link"), button -> {
            var view = selectedView();
            List<ResourceStack> resources = resources(view);
            if (view != null && !resources.isEmpty()) {
                linkFrom = view.node().id();
                linkResource = resources.get(Math.floorMod(resourceIndex, resources.size()));
                status = text("link.choose").getString();
            } else { status = text("link.no_resource").getString(); }
        }).bounds(split() + 8, height - 24, width - split() - 20, 20).build());
    }

    private int split() { return (int) (width * 0.54); }
    private int pageSize() { return Math.max(1, (height - 122) / (graph ? 42 : 28)) * (graph ? 2 : 1); }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // This screen paints its own opaque background before content; Screen.render must not blur it afterwards.
    }

    private List<ObservationIndex.View> rows() {
        String filter = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        List<ObservationIndex.View> rows = new ArrayList<>(hub.session().map(session -> session.index().views()).orElse(List.of()));
        rows.removeIf(view -> !nodeName(view).toLowerCase(Locale.ROOT).contains(filter)
                && view.resources().keySet().stream().noneMatch(resource ->
                        hub.name(resource).toLowerCase(Locale.ROOT).contains(filter)
                        || (resource.namespace() + ":" + resource.path()).contains(filter)));
        Comparator<ObservationIndex.View> comparator = Comparator.comparing(ObservationIndex.View::visible).reversed();
        if (sortByQuantity) {
            comparator = Comparator.<ObservationIndex.View>comparingDouble(view ->
                    view.resources().values().stream().mapToDouble(ResourceStack::amount).sum()).reversed();
        }
        rows.sort(comparator.thenComparing(this::nodeName)
                .thenComparing(view -> view.node().id()));
        return rows;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF101721);
        graphics.drawString(font, title, 12, 12, 0xF1F5F9);
        String summary = hub.session().map(session -> text("summary", session.index().views().size(),
                session.bus().droppedCount(), session.failures().size(), hub.omittedEntities()).getString()).orElse(text("offline").getString());
        graphics.drawString(font, font.plainSubstrByWidth(summary, Math.max(0, width - 180)), 174, 13, 0x93AABD);
        graphics.fill(12, 60, split() - 4, height - 56, 0xFF172330);
        graphics.fill(split() + 4, 60, width - 12, height - 56, 0xFF172330);
        hitboxes.clear();
        List<ObservationIndex.View> rows = rows();
        page = Math.min(page, Math.max(0, (rows.size() - 1) / pageSize()));
        List<ObservationIndex.View> shown = rows.subList(Math.min(page * pageSize(), rows.size()), Math.min((page + 1) * pageSize(), rows.size()));
        if (rows.isEmpty()) {
            graphics.drawWordWrap(font, text("empty"), 20, 72, split() - 32, 0xB8C9D7);
        } else {
            if (selected == null) { selected = shown.getFirst().node().id(); }
            if (graph) { renderGraph(graphics, shown); }
            else { renderList(graphics, shown); }
        }
        graphics.drawString(font, text("page", page + 1, Math.max(1, (rows.size() + pageSize() - 1) / pageSize())), 72, height - 42, 0x93AABD);
        renderDetails(graphics);
        if (!status.isEmpty()) { graphics.drawString(font, font.plainSubstrByWidth(status, split() - 110), 90, height - 18, 0xF4C779); }
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderList(GuiGraphics graphics, List<ObservationIndex.View> rows) {
        int y = 66;
        for (var view : rows) {
            drawNode(graphics, view, 18, y, split() - 28, 25);
            y += 28;
        }
    }

    private void drawNode(GuiGraphics graphics, ObservationIndex.View view, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, view.node().id().equals(selected) ? 0xFF284A61 : 0xFF203342);
        graphics.fill(x, y, x + 3, y + h, view.visible() ? 0xFF54D5B5 : 0xFF6B7A89);
        graphics.drawString(font, font.plainSubstrByWidth(nodeName(view), w - 12), x + 7, y + 4, 0xEDF4F8);
        String line = text(view.node().kind() == Node.Kind.ITEM_ENTITY ? "stage.entities" : "stage.menus").getString()
                + " · " + text(view.visible() ? "live" : "stale").getString();
        graphics.drawString(font, font.plainSubstrByWidth(line, w - 12), x + 7, y + 15, 0x93AABD);
        hitboxes.put(view.node().id(), new int[] {x, y, w, h});
    }

    private void renderGraph(GuiGraphics graphics, List<ObservationIndex.View> rows) {
        int boxWidth = Math.max(55, (split() - 48) / 2);
        for (int i = 0; i < rows.size(); i++) {
            int x = 18 + (i % 2) * (boxWidth + 16);
            int y = 66 + (i / 2) * 42;
            hitboxes.put(rows.get(i).node().id(), new int[] {x, y, boxWidth, 32});
        }
        hub.session().ifPresent(session -> {
            for (var edge : session.index().graph(session.worldId()).edges()) {
                int[] from = hitboxes.get(edge.from());
                int[] to = hitboxes.get(edge.to());
                if (from != null && to != null) {
                    int fromX = from[0] + from[2] / 2;
                    int toX = to[0] + to[2] / 2;
                    int fromY = from[1] + from[3] / 2;
                    int toY = to[1] + to[3] / 2;
                    graphics.hLine(Math.min(fromX, toX), Math.max(fromX, toX), fromY, 0xFFD5AD65);
                    graphics.vLine(toX, Math.min(fromY, toY), Math.max(fromY, toY), 0xFFD5AD65);
                }
            }
        });
        for (var view : rows) {
            int[] box = hitboxes.get(view.node().id());
            drawNode(graphics, view, box[0], box[1], box[2], box[3]);
        }
    }

    private ObservationIndex.View selectedView() {
        return hub.session().stream().flatMap(session -> session.index().views().stream())
                .filter(view -> view.node().id().equals(selected)).findFirst().orElse(null);
    }

    private String nodeName(ObservationIndex.View view) {
        if (view.node().kind() == Node.Kind.ITEM_ENTITY && !view.resources().isEmpty()) {
            return hub.name(view.resources().keySet().iterator().next());
        }
        return view.node().labels().get("name");
    }

    private List<ResourceStack> resources(ObservationIndex.View view) {
        return view == null ? List.of() : view.resources().values().stream()
                .sorted(Comparator.comparing(stack -> hub.name(stack.resource()))).toList();
    }

    private void renderDetails(GuiGraphics graphics) {
        int x = split() + 12;
        int w = width - x - 20;
        int y = 68 - detailScroll;
        graphics.enableScissor(x, 64, width - 16, height - 58);
        try {
            var view = selectedView();
            if (view == null) {
                graphics.drawWordWrap(font, text("details.empty"), x, y, w, 0xB8C9D7);
                return;
            }
            line(graphics, nodeName(view), x, y, w, 0xEDF4F8); y += 13;
            String state = view.visible() ? text("live").getString()
                    : text("ended." + view.ended().map(reason -> reason.name().toLowerCase(Locale.ROOT)).orElse("lost_visibility")).getString();
            line(graphics, state, x, y, w, view.visible() ? 0x54D5B5 : 0xF4C779); y += 13;
            line(graphics, text("last_seen", Math.max(0, Duration.between(view.lastSeen(), Instant.now()).toSeconds())).getString(), x, y, w, 0x93AABD); y += 13;
            List<ResourceStack> resources = resources(view);
            if (resources.isEmpty()) {
                line(graphics, text(view.evidence().quality() == dev.factoryobservability.core.evidence.DataQuality.UNKNOWN
                        ? "inventory.unknown" : "inventory.empty").getString(), x, y, w, 0xB8C9D7); y += 13;
            }
            else {
                ResourceStack stack = resources.get(Math.floorMod(resourceIndex, resources.size()));
                line(graphics, hub.name(stack.resource()), x, y, w, 0xEDF4F8); y += 12;
                line(graphics, String.format(Locale.ROOT, "%.0f %s  (%d/%d)", stack.amount(), stack.unit().symbol(),
                        Math.floorMod(resourceIndex, resources.size()) + 1, resources.size()), x, y, w, 0xEDF4F8); y += 12;
                line(graphics, stack.resource().namespace() + ":" + stack.resource().path(), x, y, w, 0x93AABD); y += 12;
                if (!stack.resource().qualifiers().isEmpty()) { line(graphics, text("variant").getString(), x, y, w, 0x93AABD); y += 12; }
            }
            line(graphics, view.evidence().provenance().name(), x, y, w, 0x93AABD); y += 12;
            line(graphics, view.evidence().quality().name(), x, y, w, 0x93AABD); y += 12;
            line(graphics, text("coverage", Math.round(view.evidence().coverage() * 100)).getString(), x, y, w, 0x93AABD); y += 12;
            Instant now = Instant.now();
            ObservationSession session = hub.session().orElseThrow();
            double temporal = session.timeline().coverage(view.node().subject(), new TimeRange(now.minusSeconds(60), now));
            line(graphics, text("visibility", Math.round(temporal * 100)).getString(), x, y, w, 0x93AABD); y += 12;
            line(graphics, text("confidence", Math.round(view.evidence().confidence() * 100)).getString(), x, y, w, 0x93AABD); y += 12;
            if (view.position().isPresent()) {
                var position = view.position().orElseThrow();
                line(graphics, String.format(Locale.ROOT, "%.1f, %.1f, %.1f", position.x(), position.y(), position.z()), x, y, w, 0x93AABD); y += 12;
            }
            line(graphics, view.node().subject().dimensionId(), x, y, w, 0x93AABD); y += 12;
            graphics.drawWordWrap(font, text("no_flow"), x, y + 4, w, 0xF4C779);
        } finally {
            graphics.disableScissor();
            int viewport = height - 122;
            if (viewport < 260) {
                int thumb = Math.max(16, viewport * viewport / 260);
                int top = 64 + detailScroll * (viewport - thumb) / Math.max(1, 260 - viewport);
                graphics.fill(width - 17, 64, width - 15, height - 58, 0xFF314958);
                graphics.fill(width - 17, top, width - 15, top + thumb, 0xFF93AABD);
            }
        }
    }

    private void line(GuiGraphics graphics, String label, int x, int y, int w, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(label, Math.max(1, w)), x, y, color);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) { return true; }
        if (button == 0) {
            for (var entry : hitboxes.entrySet()) {
                int[] box = entry.getValue();
                if (x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3]) {
                    selected = entry.getKey();
                    resourceIndex = 0;
                    detailScroll = 0;
                    if (linkFrom != null) {
                        try {
                            hub.session().orElseThrow().index().declareEdge(linkFrom, selected, linkResource.resource());
                            status = text("link.saved").getString();
                        } catch (IllegalArgumentException | IllegalStateException error) { status = text("link.invalid").getString(); }
                        linkFrom = null;
                        linkResource = null;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x < split()) {
            if (vertical > 0) { page = Math.max(0, page - 1); }
            else if (vertical < 0 && (page + 1) * pageSize() < rows().size()) { page++; }
            return true;
        }
        detailScroll = Math.max(0, Math.min(Math.max(0, 260 - (height - 122)), detailScroll - (int) (vertical * 16)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
