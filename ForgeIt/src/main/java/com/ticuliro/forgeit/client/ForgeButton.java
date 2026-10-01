package com.ticuliro.forgeit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ForgeButton extends Button {
    public ForgeButton(int x, int y, int width, int height, Component text, OnPress onPress) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
    }
    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX(), y = getY();
        g.fill(x, y, x + width, y + height, isHoveredOrFocused() && active ? 0xFFAD8046 : 0xFF656359);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, active ? (isHoveredOrFocused() ? 0xFF42494A : 0xFF373E42) : 0xFF292D2E);
        var font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), x + (width - font.width(getMessage())) / 2,
            y + (height - font.lineHeight) / 2 + 1, active ? ForgeTheme.TEXT : 0x77776F, false);
    }
}
