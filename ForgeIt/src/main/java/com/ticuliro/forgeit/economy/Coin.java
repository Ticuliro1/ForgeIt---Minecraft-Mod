package com.ticuliro.forgeit.economy;

import com.ticuliro.forgeit.ForgeIt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public enum Coin {
    COPPER(1, "copper"), SILVER(50, "silver"), GOLD(500, "gold"), PLATINUM(5000, "platinum");
    public final long value;
    public final String id;
    Coin(long value, String id) { this.value = value; this.id = id; }
    public Item item() {
        return switch (this) {
            case COPPER -> ForgeIt.COPPER_COIN.get();
            case SILVER -> ForgeIt.SILVER_COIN.get();
            case GOLD -> ForgeIt.GOLD_COIN.get();
            case PLATINUM -> ForgeIt.PLATINUM_COIN.get();
        };
    }
    public ItemStack stack(int count) { return new ItemStack(item(), count); }
    public Component displayName() { return Component.translatable("coin.forgeit." + id); }
    public long displayedCount(long balance) {
        return (balance / value) % switch (this) { case COPPER -> 50; case SILVER, GOLD -> 10; case PLATINUM -> Long.MAX_VALUE; };
    }
    public static long unitValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        for (Coin coin : values()) if (stack.is(coin.item())) return coin.value;
        return stack.is(ForgeIt.COIN.get()) ? 1 : 0;
    }
    public static Coin byIndex(int index) { return index >= 0 && index < values().length ? values()[index] : null; }
}
