package dev.factoryobservability.neoforge.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class AssistantScreen extends Screen {
    protected final Screen parent;

    protected AssistantScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected static Component text(String key, Object... args) {
        return Component.translatable("factory_observability." + key, args);
    }

    protected void background(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xFF101A25);
        graphics.drawString(font, title, 12, 12, 0xECF5F9);
    }

    protected Button button(Component label, int x, int y, int w, int h, Button.OnPress action) {
        return addRenderableWidget(Button.builder(label, action).bounds(x, y, w, h).build());
    }

    protected void line(GuiGraphics graphics, String value, int x, int y, int w, int color) {
        String clipped = font.plainSubstrByWidth(value, Math.max(1, w));
        if (!clipped.equals(value) && w > 15) { clipped = font.plainSubstrByWidth(value, w - 9) + "..."; }
        graphics.drawString(font, clipped, x, y, color);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
