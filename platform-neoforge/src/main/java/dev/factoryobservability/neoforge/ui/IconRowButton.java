package dev.factoryobservability.neoforge.ui;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A native keyboard-focusable button whose icon, label and quantity are visible together. */
final class IconRowButton extends Button {
    private final Supplier<ItemStack> icon;
    private final Component subtitle;
    private final String quantity;
    private final boolean selected;

    IconRowButton(int x, int y, int w, int h, Component label, Component subtitle, String quantity,
            Supplier<ItemStack> icon, boolean selected, OnPress action) {
        super(x, y, w, h, label, action, DEFAULT_NARRATION);
        this.icon = icon;
        this.subtitle = subtitle;
        this.quantity = quantity;
        this.selected = selected;
        setTooltip(Tooltip.create(label.copy().append("\n").append(subtitle)));
    }

    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        var font = Minecraft.getInstance().font;
        graphics.fill(getX(), getY(), getRight(), getBottom(), selected ? 0xFF28556A : isHoveredOrFocused() ? 0xFF2A4050 : 0xFF1D3040);
        if (selected) { graphics.fill(getX(), getY(), getX() + 3, getBottom(), 0xFF69DFC0); }
        if (isFocused()) { graphics.renderOutline(getX(), getY(), width, height, 0xFFA5DACC); }
        graphics.renderItem(icon.get(), getX() + 5, getY() + (height - 16) / 2);
        int reserved = quantity.isEmpty() ? 0 : font.width(quantity) + 8;
        int labelWidth = Math.max(1, width - 30 - reserved);
        String label = getMessage().getString();
        String clipped = font.plainSubstrByWidth(label, labelWidth);
        if (!clipped.equals(label) && labelWidth > 15) { clipped = font.plainSubstrByWidth(label, labelWidth - 9) + "..."; }
        graphics.drawString(font, clipped, getX() + 27, getY() + (height > 28 ? 5 : (height - 9) / 2), 0xF1F7FA);
        if (height > 28) {
            graphics.drawString(font, font.plainSubstrByWidth(subtitle.getString(), Math.max(1, width - 32)),
                    getX() + 27, getY() + 19, 0xA8BDCB);
        }
        if (!quantity.isEmpty()) { graphics.drawString(font, quantity, getRight() - font.width(quantity) - 6, getY() + (height - 9) / 2, 0x9EE5CA); }
    }
}
