package com.ticuliro.forgeit.economy;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Adds one tier table to entity death loot, preserving all vanilla/mod loot. */
public final class CoinLootModifier extends LootModifier {
    public static final MapCodec<CoinLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, CoinLootModifier::new));
    public CoinLootModifier(LootItemCondition[] conditions) { super(conditions); }
    @Override public MapCodec<CoinLootModifier> codec() { return CODEC; }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Mob mob) || mob instanceof EnderDragon || !mob.isDeadOrDying()
            || !context.hasParam(LootContextParams.DAMAGE_SOURCE)
            || !context.getQueriedLootTableId().equals(mob.getLootTable().location())) return loot;
        if (!CoinDrops.allowed(mob, context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) != null)) return loot;
        loot.addAll(CoinDrops.roll(mob, context)); return loot;
    }
}
