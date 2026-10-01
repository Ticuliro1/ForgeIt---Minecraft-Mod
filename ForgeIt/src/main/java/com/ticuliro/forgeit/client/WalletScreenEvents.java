package com.ticuliro.forgeit.client;

import com.ticuliro.forgeit.ForgeIt;
import com.ticuliro.forgeit.economy.WalletNetwork;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectUtil;
import net.neoforged.neoforge.client.ClientHooks;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ForgeIt.MOD_ID, value = Dist.CLIENT)
public final class WalletScreenEvents {
    private static boolean inventory(Screen screen) { return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen; }
    private static WalletPanel panel(Screen screen) {
        for (var child : screen.children()) if (child instanceof WalletPanel wallet) return wallet;
        return null;
    }
    @SubscribeEvent public static void init(ScreenEvent.Init.Post event) {
        if (!inventory(event.getScreen())) return;
        var screen = (AbstractContainerScreen<?>)event.getScreen();
        int width = Math.min(WalletPanel.PREFERRED_WIDTH, screen.width - screen.getXSize() - WalletPanel.GAP - 4);
        var panel = new WalletPanel(0, 0, Math.max(104, width));
        if (screen instanceof InventoryScreen inventory) {
            int available = screen.width - panel.getWidth() - WalletPanel.GAP;
            inventory.widthTooNarrow = available < 379;
            inventory.getRecipeBookComponent().init(available, screen.height, Minecraft.getInstance(), inventory.widthTooNarrow, inventory.getMenu());
        }
        event.addListener(panel); align(screen, panel);
        PacketDistributor.sendToServer(new WalletNetwork.Action(2, 0, 0));
    }
    private static void align(AbstractContainerScreen<?> screen, WalletPanel wallet) {
        int oldX = screen.getGuiLeft();
        int available = screen.width - wallet.getWidth() - WalletPanel.GAP;
        int preferred = screen instanceof InventoryScreen inventory
            ? inventory.getRecipeBookComponent().updateScreenPosition(available, screen.getXSize())
            : (available - screen.getXSize()) / 2;
        int newX = Math.max(2, Math.min(preferred, available - screen.getXSize() - 2));
        if (oldX != newX) {
            screen.leftPos = newX;
            for (var child : screen.children()) if (child instanceof AbstractWidget widget && child != wallet) widget.setX(widget.getX() + newX - oldX);
        }
        wallet.setPosition(newX + screen.getXSize() + WalletPanel.GAP, Math.max(2, Math.min(screen.getGuiTop(), screen.height - WalletPanel.HEIGHT - 2)));
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Pre event) {
        var wallet = panel(event.getScreen());
        if (wallet != null && event.getScreen() instanceof AbstractContainerScreen<?> screen) align(screen, wallet);
    }
    @SubscribeEvent public static void foreground(ScreenEvent.Render.Post event) {
        var wallet = panel(event.getScreen());
        if (wallet == null) return;
        wallet.renderPanel(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        // When both side margins are too small, keep status effects visible above the inventory.
        int right = wallet.getX() + wallet.getWidth() + 4;
        if (screen.width - right >= 32 || screen.getGuiLeft() >= 34) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var effects = mc.player.getActiveEffects().stream().filter(ClientHooks::shouldRenderEffect).sorted().toList();
        int step = Math.min(22, (screen.getXSize() - 22) / Math.max(1, effects.size() - 1));
        for (int i = 0; i < effects.size(); i++) {
            var effect = effects.get(i); int x = screen.getGuiLeft() + i * step, y = Math.max(1, screen.getGuiTop() - 23);
            var g = event.getGuiGraphics();
            g.fill(x, y, x + 21, y + 21, ForgeTheme.INSET);
            g.blit(x + 1, y + 1, 0, 18, 18, mc.getMobEffectTextures().get(effect.getEffect()));
            if (event.getMouseX() >= x && event.getMouseX() < x + step && event.getMouseY() >= y && event.getMouseY() < y + 21) {
                g.renderTooltip(mc.font, java.util.List.of(effect.getEffect().value().getDisplayName(),
                    MobEffectUtil.formatDuration(effect, 1F, mc.level.tickRateManager().tickrate())), java.util.Optional.empty(), event.getMouseX(), event.getMouseY());
            }
        }
    }
    @SubscribeEvent public static void effects(ScreenEvent.RenderInventoryMobEffects event) {
        var wallet = panel(event.getScreen()); if (wallet == null) return;
        var screen = (AbstractContainerScreen<?>)event.getScreen();
        int right = wallet.getX() + wallet.getWidth() + 4;
        if (screen.width - right >= 32) { event.setHorizontalOffset(right); event.setCompact(screen.width - right < 120); }
        else if (screen.getGuiLeft() >= 34) { event.setHorizontalOffset(screen.getGuiLeft() - 34); event.setCompact(true); }
        else event.setCanceled(true);
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        var wallet = panel(event.getScreen()); if (wallet == null) return;
        if (wallet.isMouseOver(event.getMouseX(), event.getMouseY())) { wallet.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton()); event.setCanceled(true); }
        else wallet.unfocus();
    }
    @SubscribeEvent public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        var wallet = panel(event.getScreen());
        if (wallet != null && wallet.isMouseOver(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
    @SubscribeEvent public static void key(ScreenEvent.KeyPressed.Pre event) {
        var wallet = panel(event.getScreen());
        if (wallet != null && wallet.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers())) event.setCanceled(true);
    }
    @SubscribeEvent public static void character(ScreenEvent.CharacterTyped.Pre event) {
        var wallet = panel(event.getScreen());
        if (wallet != null && wallet.charTyped(event.getCodePoint(), event.getModifiers())) event.setCanceled(true);
    }
}
