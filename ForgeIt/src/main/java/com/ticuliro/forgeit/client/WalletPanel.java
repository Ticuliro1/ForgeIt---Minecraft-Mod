package com.ticuliro.forgeit.client;

import com.ticuliro.forgeit.economy.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WalletPanel extends AbstractWidget {
    public static final int PREFERRED_WIDTH = 132, HEIGHT = 202, GAP = 4;
    private final Minecraft mc = Minecraft.getInstance();
    private final EditBox quantity;
    private final ForgeButton withdraw, deposit;
    private Coin selected = Coin.COPPER;
    public WalletPanel(int x, int y, int width) {
        super(x, y, width, HEIGHT, Component.translatable("screen.forgeit.wallet"));
        quantity = new EditBox(mc.font, 0, 0, width - 53, 17, Component.translatable("screen.forgeit.quantity"));
        quantity.setMaxLength(4);
        quantity.setFilter(value -> value.isEmpty() || value.chars().allMatch(c -> c >= '0' && c <= '9'));
        quantity.setValue("1");
        withdraw = new ForgeButton(0, 0, width - 16, 20, Component.translatable("screen.forgeit.withdraw"),
            b -> PacketDistributor.sendToServer(new WalletNetwork.Action(0, selected.ordinal(), count())));
        deposit = new ForgeButton(0, 0, width - 16, 18, Component.translatable("screen.forgeit.deposit"),
            b -> PacketDistributor.sendToServer(new WalletNetwork.Action(1, 0, 0)));
    }
    private int count() { try { return Integer.parseInt(quantity.getValue()); } catch (NumberFormatException ex) { return 0; } }
    private long balance() { return mc.player == null ? 0 : Wallet.balance(mc.player); }
    private void positionControls() {
        quantity.setPosition(getX() + 45, getY() + 119);
        withdraw.setPosition(getX() + 8, getY() + 141); deposit.setPosition(getX() + 8, getY() + 164);
        boolean usable = mc.player != null && !mc.player.isSpectator() && mc.player.containerMenu.getCarried().isEmpty();
        withdraw.active = usable && count() > 0 && count() <= Wallet.MAX_WITHDRAW_COUNT && (long)count() * selected.value <= balance();
        deposit.active = usable;
    }
    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}
    public void renderPanel(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        positionControls(); int x = getX(), y = getY(); long balance = balance();
        ForgeTheme.panel(g, x, y, width, height);
        g.drawCenteredString(mc.font, getMessage(), x + width / 2, y + 11, ForgeTheme.GOLD);
        for (Coin coin : Coin.values()) {
            int rowY = y + 29 + coin.ordinal() * 22;
            if (coin == selected) {
                g.fill(x + 6, rowY - 1, x + width - 6, rowY + 20, 0xFFAD8046);
                g.fill(x + 7, rowY, x + width - 7, rowY + 19, 0xFF303638);
            }
            g.renderItem(coin.stack(1), x + 9, rowY + 1);
            g.drawString(mc.font, coin.displayName(), x + 29, rowY + 1, ForgeTheme.MUTED, false);
            ForgeTheme.fit(g, mc.font, Component.literal(ForgeTheme.number(coin.displayedCount(balance))), x + 29, rowY + 11, width - 38, ForgeTheme.TEXT);
        }
        g.drawString(mc.font, Component.translatable("screen.forgeit.quantity"), x + 8, y + 123, ForgeTheme.MUTED, false);
        quantity.render(g, mouseX, mouseY, partialTick); withdraw.render(g, mouseX, mouseY, partialTick); deposit.render(g, mouseX, mouseY, partialTick);
        ForgeTheme.fit(g, mc.font, Component.translatable("screen.forgeit.total_copper", ForgeTheme.number(balance)), x + 8, y + 189, width - 16, ForgeTheme.MUTED);
        if (isMouseOver(mouseX, mouseY)) {
            Component tooltip = null;
            if (mouseY >= y + 29 && mouseY < y + 117) {
                Coin coin = Coin.values()[Math.min(3, (mouseY - y - 29) / 22)];
                tooltip = Component.translatable("screen.forgeit.coin_select", coin.displayName(), ForgeTheme.number(coin.value), ForgeTheme.number(balance / coin.value));
            } else if (withdraw.isMouseOver(mouseX, mouseY)) tooltip = Component.translatable("screen.forgeit.withdraw_hint", count(), selected.displayName(), ForgeTheme.number((long)count() * selected.value));
            else if (deposit.isMouseOver(mouseX, mouseY)) tooltip = Component.translatable("screen.forgeit.deposit_hint");
            else if (mouseY >= y + 184) tooltip = Component.translatable("screen.forgeit.total_copper", ForgeTheme.number(balance));
            if (tooltip != null) g.renderTooltip(mc.font, mc.font.split(tooltip, 190), mouseX, mouseY);
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        positionControls();
        if (!isMouseOver(mouseX, mouseY)) { unfocus(); return false; }
        if (mc.player == null || !mc.player.containerMenu.getCarried().isEmpty()) return true;
        quantity.setFocused(quantity.isMouseOver(mouseX, mouseY));
        if (quantity.mouseClicked(mouseX, mouseY, button)) return true;
        if (withdraw.mouseClicked(mouseX, mouseY, button) || deposit.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && mouseY >= getY() + 29 && mouseY < getY() + 117) selected = Coin.values()[Math.min(3, (int)(mouseY - getY() - 29) / 22)];
        return true;
    }
    public void unfocus() { quantity.setFocused(false); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!quantity.isFocused()) return false;
        if (key == 256 || (mc.options.keyInventory.matches(key, scan))) { unfocus(); return false; }
        quantity.keyPressed(key, scan, modifiers); return true;
    }
    @Override public boolean charTyped(char character, int modifiers) { return quantity.isFocused() && quantity.charTyped(character, modifiers); }
    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); quantity.updateNarration(output); }
}
