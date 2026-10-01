package com.ticuliro.forgeit.client;

import com.ticuliro.forgeit.economy.Coin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.text.NumberFormat;
import java.util.Locale;

/** Palette sampled from the user's editable workshop PNG. */
public final class ForgeTheme {
    public static final int TEXT = 0xE5DDD2, MUTED = 0xAAA394, GOLD = 0xEDB960;
    public static final int BACK = 0xFF252A2D, INSET = 0xFF191D20, BORDER = 0xFF6C5941;
    private ForgeTheme() {}
    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xFF111416);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, BORDER);
        g.fill(x + 3, y + 3, x + width - 3, y + height - 3, BACK);
        g.fill(x + 4, y + 4, x + width - 4, y + 25, 0xFF181C1F);
        g.fill(x + 9, y + 25, x + width - 9, y + 26, 0xFFAD8046);
    }
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF121619);
        g.fill(x, y, x + 17, y + 17, 0xFF656359);
        g.fill(x, y, x + 16, y + 16, 0xFF373E42);
    }
    public static void fit(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
        float scale = Math.min(1F, (float)width / Math.max(1, font.width(text)));
        g.pose().pushPose(); g.pose().translate(x, y, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, color, false); g.pose().popPose();
    }
    public static String number(long value) { return NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(value); }
    public static Component money(long value) {
        var text = Component.empty(); Coin[] coins = Coin.values();
        for (int i = coins.length - 1; i >= 0; i--) {
            Coin coin = coins[i]; long count = coin.displayedCount(value);
            if (count != 0 || (value == 0 && coin == Coin.COPPER)) {
                if (!text.getString().isEmpty()) text.append(" ");
                text.append(number(count)).append(Component.translatable("coin.forgeit.short." + coin.id));
            }
        }
        return text;
    }
}
