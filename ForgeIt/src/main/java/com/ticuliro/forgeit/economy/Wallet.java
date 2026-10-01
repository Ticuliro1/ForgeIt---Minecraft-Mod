package com.ticuliro.forgeit.economy;

import com.ticuliro.forgeit.ForgeIt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Mutations run on the server thread. Value is always counted in copper units. */
public final class Wallet {
    public static final long MAX_BALANCE = 9_000_000_000_000_000L;
    public static final int MAX_WITHDRAW_COUNT = 2304;
    private Wallet() {}
    public static long balance(Player player) { return player.getData(ForgeIt.WALLET); }
    private static void set(Player player, long value) {
        player.setData(ForgeIt.WALLET, value);
        if (player instanceof ServerPlayer server) WalletNetwork.sync(server);
    }
    public static boolean credit(Player player, long amount) {
        if (player.level().isClientSide || amount < 0 || amount > MAX_BALANCE - balance(player)) return false;
        set(player, balance(player) + amount); return true;
    }
    public static boolean debit(Player player, long amount) {
        if (player.level().isClientSide || amount < 0 || balance(player) < amount) return false;
        set(player, balance(player) - amount); return true;
    }
    public enum Result { SUCCESS, INVALID, INSUFFICIENT, FULL, LIMIT, EMPTY }

    /** Preflight the entire main inventory: no partial withdrawal or spill onto the ground. */
    public static Result withdraw(Player player, Coin coin, int count) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator() || coin == null || count < 1 || count > MAX_WITHDRAW_COUNT) return Result.INVALID;
        long value = coin.value * count;
        if (balance(player) < value) return Result.INSUFFICIENT;
        var inventory = player.getInventory();
        ItemStack prototype = coin.stack(1);
        int capacity = 0;
        for (ItemStack slot : inventory.items) {
            if (slot.isEmpty()) capacity += prototype.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(slot, prototype)) capacity += Math.max(0, slot.getMaxStackSize() - slot.getCount());
        }
        if (capacity < count) return Result.FULL;
        debit(player, value);
        int left = count;
        for (ItemStack slot : inventory.items) {
            if (left == 0) break;
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, prototype)) {
                int add = Math.min(left, slot.getMaxStackSize() - slot.getCount());
                if (add > 0) { slot.grow(add); left -= add; }
            }
        }
        for (int i = 0; i < inventory.items.size() && left > 0; i++) if (inventory.items.get(i).isEmpty()) {
            int add = Math.min(left, prototype.getMaxStackSize());
            inventory.items.set(i, coin.stack(add)); left -= add;
        }
        inventory.setChanged(); player.containerMenu.broadcastChanges();
        return Result.SUCCESS;
    }

    /** Explicit deposit; withdrawn coins stay physical until deposited or picked up from the ground. */
    public static Result depositInventory(Player player, boolean legacyOnly) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()) return Result.INVALID;
        var inventory = player.getInventory(); long value = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!legacyOnly || stack.is(ForgeIt.COIN.get())) value += Coin.unitValue(stack) * stack.getCount();
        }
        if (value == 0) return Result.EMPTY;
        if (value > MAX_BALANCE - balance(player)) return Result.LIMIT;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (Coin.unitValue(stack) > 0 && (!legacyOnly || stack.is(ForgeIt.COIN.get()))) inventory.setItem(i, ItemStack.EMPTY);
        }
        credit(player, value); inventory.setChanged(); player.containerMenu.broadcastChanges();
        return Result.SUCCESS;
    }
}
