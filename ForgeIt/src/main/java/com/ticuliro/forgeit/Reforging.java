package com.ticuliro.forgeit;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;

/** Item data lives in vanilla custom_data and is saved/synchronized with the stack. */
public final class Reforging {
    public static final String DATA_KEY = "forgeit";
    public static final TagKey<Item> REFORGEABLE = TagKey.create(Registries.ITEM, ForgeIt.id("reforgeable"));

    private Reforging() {}

    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() == 1 && stack.isDamageableItem()
            && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof DiggerItem || stack.is(REFORGEABLE));
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(DATA_KEY);
    }

    public static ReforgeModifier modifier(ItemStack stack) {
        return ReforgeModifier.byId(data(stack).getString("modifier"));
    }

    public static void apply(ItemStack stack, ReforgeModifier modifier) {
        if (!accepts(stack)) throw new IllegalArgumentException("Unsupported reforge item");
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag old = root.getCompound(DATA_KEY);
        int oldMax = stack.getMaxDamage();
        int base = old.contains("base_durability") ? old.getInt("base_durability") : oldMax;
        base = Math.max(1, base);
        int newMax = Math.max(1, (int)Math.round(base * (1.0 + modifier.durability / 100.0)));
        // Round wear upward: rerolling must not be an unlimited repair loop.
        int newDamage = (int)Math.ceil((double)stack.getDamageValue() * newMax / oldMax);
        CompoundTag value = new CompoundTag();
        value.putInt("version", 1);
        value.putString("modifier", modifier.id);
        value.putInt("base_durability", base);
        value.putInt("rolls", Math.min(Integer.MAX_VALUE - 1, Math.max(0, old.getInt("rolls"))) + 1);
        root.put(DATA_KEY, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.MAX_DAMAGE, newMax);
        stack.setDamageValue(Math.min(newMax - 1, newDamage));
    }
}
