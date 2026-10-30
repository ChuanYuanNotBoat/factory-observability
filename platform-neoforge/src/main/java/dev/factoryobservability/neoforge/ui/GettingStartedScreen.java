package dev.factoryobservability.neoforge.ui;

import dev.factoryobservability.neoforge.FactoryObservabilityClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GettingStartedScreen extends AssistantScreen {
    public GettingStartedScreen(Screen parent) { super(text("help.title"), parent); }

    @Override protected void init() {
        button(text("back"), 12, height - 26, 64, 20, value -> onClose());
        button(text("help.go"), width - 166, height - 26, 154, 20, value -> minecraft.setScreen(null));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        background(graphics);
        int x = Math.max(12, (width - 500) / 2);
        int w = Math.min(500, width - 24);
        int cardHeight = Math.max(46, Math.min(65, (height - 90) / 3));
        var icons = new ItemStack[] {new ItemStack(Items.CHEST), new ItemStack(Items.BOOK), new ItemStack(Items.IRON_INGOT)};
        for (int i = 0; i < 3; i++) {
            int y = 36 + i * (cardHeight + 4);
            graphics.fill(x, y, x + w, y + cardHeight, 0xFF1B2E3D);
            graphics.renderItem(icons[i], x + 10, y + 12);
            graphics.drawString(font, text("help.step" + (i + 1)), x + 38, y + 7, 0x9EE5CA);
            graphics.drawWordWrap(font, text("help.body" + (i + 1)), x + 38, y + 21, w - 50, 0xCBD9E2);
        }
        line(graphics, text("help.shortcut", FactoryObservabilityClient.openKeyLabel()).getString(), x,
                height - 42, w, 0x9CAFBF);
        super.render(graphics, mouseX, mouseY, delta);
    }
}
