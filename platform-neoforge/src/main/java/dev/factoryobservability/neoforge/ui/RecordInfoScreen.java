package dev.factoryobservability.neoforge.ui;

import java.util.ArrayList;
import java.util.List;

import dev.factoryobservability.core.evidence.DataQuality;
import dev.factoryobservability.core.graph.Node;
import dev.factoryobservability.minecraft.observation.ObservationIndex;
import dev.factoryobservability.neoforge.ClientObservationHub;
import net.minecraft.client.gui.GuiGraphics;

public final class RecordInfoScreen extends AssistantScreen {
    private final ClientObservationHub hub;
    private final ObservationIndex.View view;

    public RecordInfoScreen(ObservatoryScreen parent, ClientObservationHub hub, ObservationIndex.View view) {
        super(text("info.title"), parent);
        this.hub = hub;
        this.view = view;
    }

    @Override protected void init() { button(text("back"), width / 2 - 50, height - 26, 100, 20, value -> onClose()); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        background(graphics);
        int x = Math.max(16, (width - 440) / 2);
        int w = Math.min(440, width - 32);
        List<String> lines = new ArrayList<>();
        lines.add(text(view.node().kind() == Node.Kind.STORAGE ? "info.menu" : "info.entity").getString());
        lines.add(text(view.evidence().quality() == DataQuality.UNKNOWN ? "info.waiting"
                : view.evidence().coverage() < 1 ? "info.partial" : "info.observed").getString());
        lines.add(text("info.when", ObservatoryScreen.age(view.lastSeen())).getString());
        lines.add(text("info.refresh").getString());
        lines.add(text("session.only").getString());
        int y = 38;
        for (String line : lines) {
            graphics.drawWordWrap(font, net.minecraft.network.chat.Component.literal(line), x, y, w, 0xCBD9E2);
            y += font.split(net.minecraft.network.chat.Component.literal(line), w).size() * 9 + 9;
        }
        super.render(graphics, mouseX, mouseY, delta);
    }
}
