package com.ticuliro.forgeit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/** The weights sum to 100. A roll is independent and may repeat the previous result. */
public enum ReforgeModifier {
    SHARP("sharp", 10, 0, 0, 22, ChatFormatting.GREEN),
    SWIFT("swift", 0, 15, 0, 20, ChatFormatting.AQUA),
    HEAVY("heavy", 20, -15, 0, 16, ChatFormatting.GOLD),
    STURDY("sturdy", 0, 0, 25, 18, ChatFormatting.GREEN),
    FRAGILE("fragile", 15, 0, -30, 10, ChatFormatting.YELLOW),
    LEGENDARY("legendary", 15, 10, 10, 4, ChatFormatting.LIGHT_PURPLE),
    RUSTY("rusty", -15, -10, -10, 10, ChatFormatting.RED);

    public final String id;
    public final int damage, speed, durability, weight;
    public final ChatFormatting color;

    ReforgeModifier(String id, int damage, int speed, int durability, int weight, ChatFormatting color) {
        this.id = id; this.damage = damage; this.speed = speed;
        this.durability = durability; this.weight = weight; this.color = color;
    }

    public Component displayName() {
        return Component.translatable("modifier.forgeit." + id).withStyle(color);
    }

    public static ReforgeModifier byId(String id) {
        for (ReforgeModifier m : values()) if (m.id.equals(id)) return m;
        return null;
    }

    public static ReforgeModifier roll(RandomSource random) {
        int n = random.nextInt(100);
        for (ReforgeModifier m : values()) {
            n -= m.weight;
            if (n < 0) return m;
        }
        throw new IllegalStateException("Modifier weights must sum to 100");
    }

    public static String percent(int value) {
        return (value > 0 ? "+" : "") + value + "%";
    }
}
