package com.ticuliro.forgeit;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class ForgeItEvents {
    private ForgeItEvents() {}

    public static void attributes(ItemAttributeModifierEvent event) {
        ReforgeModifier m = Reforging.modifier(event.getItemStack());
        if (m == null) return;
        if (m.damage != 0) event.replaceModifier(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(ForgeIt.id("reforge_damage"), m.damage / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
        if (m.speed != 0) event.replaceModifier(Attributes.ATTACK_SPEED,
            new AttributeModifier(ForgeIt.id("reforge_speed"), m.speed / 100.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.MAINHAND);
    }

    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (com.ticuliro.forgeit.economy.Coin.unitValue(stack) > 0) {
            event.getToolTip().add(Component.translatable("tooltip.forgeit.coin_value", com.ticuliro.forgeit.economy.Coin.unitValue(stack)).withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable("tooltip.forgeit.coin").withStyle(ChatFormatting.GRAY));
            return;
        }
        ReforgeModifier m = Reforging.modifier(stack);
        if (m == null) return;
        event.getToolTip().add(1, Component.translatable("tooltip.forgeit.modifier", m.displayName()).withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable("tooltip.forgeit.bonuses").withStyle(ChatFormatting.DARK_GRAY));
        addStat(event, "damage", m.damage);
        addStat(event, "speed", m.speed);
        addStat(event, "durability", m.durability);
        if (event.getFlags().isAdvanced()) event.getToolTip().add(Component.translatable("tooltip.forgeit.rolls", Reforging.data(stack).getInt("rolls")).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void addStat(ItemTooltipEvent e, String key, int value) {
        if (value != 0) e.getToolTip().add(Component.translatable("stat.forgeit." + key)
            .append(": " + ReforgeModifier.percent(value)).withStyle(value > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
