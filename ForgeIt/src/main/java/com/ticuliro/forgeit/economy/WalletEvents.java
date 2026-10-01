package com.ticuliro.forgeit.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class WalletEvents {
    private WalletEvents() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pickup(ItemEntityPickupEvent.Pre event) {
        var player = event.getPlayer(); var entity = event.getItemEntity(); var stack = entity.getItem();
        long unitValue = Coin.unitValue(stack);
        if (unitValue == 0 || player.level().isClientSide) return;
        boolean denied = event.canPickup().isFalse();
        event.setCanPickup(TriState.FALSE);
        if (denied || entity.isRemoved() || entity.hasPickUpDelay() || !player.isAlive() || player.isSpectator()
            || (entity.getTarget() != null && !entity.getTarget().equals(player.getUUID()))) return;
        int count = stack.getCount();
        if (!Wallet.credit(player, unitValue * count)) return;
        player.take(entity, count); player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), count);
        stack.shrink(count); entity.discard();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP,
            SoundSource.PLAYERS, 0.2F, 1.3F + player.getRandom().nextFloat() * 0.3F);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { Wallet.depositInventory(player, true); WalletNetwork.sync(player); }
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WalletNetwork.sync(player);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WalletNetwork.sync(player);
    }
}
