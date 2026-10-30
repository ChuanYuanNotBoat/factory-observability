package dev.factoryobservability.neoforge.ui;

import java.util.UUID;

import dev.factoryobservability.neoforge.ClientObservationHub;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

public final class RenameRecordScreen extends AssistantScreen {
    private final ClientObservationHub hub;
    private final UUID record;
    private final String initialName;
    private EditBox name;
    private Button save;

    public RenameRecordScreen(ObservatoryScreen parent, ClientObservationHub hub, UUID record, String initialName) {
        super(text("rename.title"), parent);
        this.hub = hub;
        this.record = record;
        this.initialName = initialName;
    }

    @Override protected void init() {
        int w = Math.min(300, width - 40);
        int x = (width - w) / 2;
        name = new EditBox(font, x, height / 2 - 12, w, 20, text("rename.title"));
        name.setMaxLength(64);
        name.setValue(initialName);
        addRenderableWidget(name);
        setInitialFocus(name);
        save = button(text("save"), x + w - 90, height / 2 + 20, 90, 20, value -> {
            hub.session().ifPresent(session -> session.index().views().stream().filter(row -> row.node().id().equals(record))
                    .findFirst().ifPresent(row -> session.index().name(row.node().subject(), name.getValue().strip())));
            onClose();
        });
        name.setResponder(value -> save.active = !value.isBlank());
        save.active = !name.getValue().isBlank();
        button(text("cancel"), x, height / 2 + 20, 90, 20, value -> onClose());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        background(graphics);
        graphics.drawCenteredString(font, text("rename.hint"), width / 2, height / 2 - 35, 0xB5CAD7);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if ((key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) && save.active) {
            save.onPress();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
