package com.ticuliro.forgeit.client;

import com.ticuliro.forgeit.ForgeIt;
import com.ticuliro.forgeit.economy.Coin;
import com.ticuliro.forgeit.ReforgeModifier;
import com.ticuliro.forgeit.Reforging;
import com.ticuliro.forgeit.ReforgingMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ReforgingScreen extends AbstractContainerScreen<ReforgingMenu> {
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.fromNamespaceAndPath("forgeit", "textures/gui/reforging_table.png");
    private static final int TEXT = 0xE5DDD2;
    private static final int MUTED = 0xAAA394;
    private static final int GOLD = 0xEDB960;
    private Button reforge;
    private int pulseTicks;
    private int knownRolls;

    public ReforgingScreen(ReforgingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 225;
        inventoryLabelX = 47;
        inventoryLabelY = 130;
    }

    @Override protected void init() {
        super.init();
        // Center the entire 224px action group on the 256px workshop.
        reforge = addRenderableWidget(new ForgeButton(leftPos + 16, topPos + 106, 104, 22,
            Component.translatable("screen.forgeit.reforge"), button -> {
                if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }));
        reforge.setTooltip(Tooltip.create(Component.translatable("screen.forgeit.warning")));
        var help = addRenderableWidget(new ForgeButton(leftPos + 230, topPos + 8, 18, 18, Component.literal("?"), button -> {}));
        help.setTooltip(Tooltip.create(probabilities()));
        updateButton();
    }

    private Component probabilities() {
        var text = Component.translatable("screen.forgeit.odds").withStyle(ChatFormatting.GOLD);
        for (ReforgeModifier m : ReforgeModifier.values()) text.append(Component.literal("\n").append(m.displayName()).append(": " + m.weight + "%"));
        text.append(Component.literal("\n\n").append(Component.translatable("screen.forgeit.repeat").withStyle(ChatFormatting.GRAY)));
        return text;
    }

    private void updateButton() { reforge.active = Reforging.accepts(menu.item()) && menu.balance() >= menu.cost(); }

    @Override protected void containerTick() {
        super.containerTick();
        updateButton();
        int rolls = Reforging.data(menu.item()).getInt("rolls");
        if (rolls != knownRolls) { knownRolls = rolls; pulseTicks = 12; }
        if (pulseTicks > 0) pulseTicks--;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (isHovering(128, 106, 112, 21, mouseX, mouseY)) graphics.renderTooltip(font,
            List.of(Component.translatable("screen.forgeit.cost", ForgeTheme.money(menu.cost())),
                Component.translatable("screen.forgeit.balance", ForgeTheme.money(menu.balance())),
                Component.translatable("screen.forgeit.wallet_payment")), java.util.Optional.empty(), mouseX, mouseY);
        if (menu.item().isEmpty() && isHovering(25, 48, 28, 28, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("screen.forgeit.accepted"), mouseX, mouseY);
        }
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 225);
        if (menu.item().isEmpty()) g.drawString(font, "+", x + 31 + (16 - font.width("+")) / 2, y + 54 + (16 - font.lineHeight) / 2 + 1, 0xAAA394, false);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) ForgeTheme.slot(g, x + 47 + col * 18, y + 141 + row * 18);
        for (int col = 0; col < 9; col++) ForgeTheme.slot(g, x + 47 + col * 18, y + 199);
        g.fill(x + 128, y + 106, x + 240, y + 128, 0xFF4B4941);
        g.fill(x + 129, y + 107, x + 239, y + 127, ForgeTheme.INSET);
        Coin costCoin = Coin.COPPER;
        for (Coin coin : Coin.values()) if (menu.cost() >= coin.value) costCoin = coin;
        g.renderItem(costCoin.stack(1), x + 130, y + 109);
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, "ForgeIt!", 13, 12, GOLD, false);
        ForgeTheme.fit(g, font, Component.translatable("screen.forgeit.subtitle"), 69, 13, 155, MUTED);
        g.drawCenteredString(font, Component.translatable("screen.forgeit.item"), 39, 79, MUTED);
        ReforgeModifier m = Reforging.modifier(menu.item());
        Component label = m == null ? Component.translatable("screen.forgeit.unforged") : m.displayName();
        g.drawString(font, label, 78, 41, TEXT, false);
        stat(g, "damage", m == null ? 0 : m.damage, 55);
        stat(g, "speed", m == null ? 0 : m.speed, 67);
        stat(g, "durability", m == null ? 0 : m.durability, 79);
        g.drawCenteredString(font, Component.translatable("screen.forgeit.hint"), 128, 96, MUTED);
        ForgeTheme.fit(g, font, Component.translatable("screen.forgeit.cost", ForgeTheme.money(menu.cost())), 150, 108, 88, GOLD);
        ForgeTheme.fit(g, font, Component.translatable("screen.forgeit.balance", ForgeTheme.money(menu.balance())), 150, 118, 88,
            menu.balance() >= menu.cost() ? TEXT : 0xF08070);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    private void stat(GuiGraphics g, String key, int value, int y) {
        g.drawString(font, Component.translatable("stat.forgeit." + key), 78, y, MUTED, false);
        String text = ReforgeModifier.percent(value);
        g.drawString(font, text, 232 - font.width(text), y, value > 0 ? 0x83CE96 : value < 0 ? 0xED8B80 : 0xA5A59D, false);
    }
}
