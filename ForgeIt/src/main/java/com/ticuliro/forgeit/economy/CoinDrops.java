package com.ticuliro.forgeit.economy;

import com.ticuliro.forgeit.ForgeIt;
import com.ticuliro.forgeit.ForgeItConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

public final class CoinDrops {
    public static final TagKey<EntityType<?>> BOSSES = tag("coin_bosses"), RARE = tag("coin_rare"), INTERMEDIATE = tag("coin_intermediate"), HOSTILE = tag("coin_hostile");
    private static final String PAID = "forgeit_reward_paid_v2";
    private CoinDrops() {}
    private static TagKey<EntityType<?>> tag(String path) { return TagKey.create(Registries.ENTITY_TYPE, ForgeIt.id(path)); }
    public static String tier(Mob mob) {
        if (mob.getType().is(BOSSES)) return "boss";
        if (mob.getType().is(RARE)) return "rare";
        if (mob.getType().is(INTERMEDIATE)) return "intermediate";
        if (mob.getType().is(HOSTILE)) return "hostile";
        if (mob.getMaxHealth() >= 200) return "boss";
        if (mob.getMaxHealth() >= 80) return "rare";
        if (mob instanceof Enemy) return mob.getMaxHealth() >= 40 ? "intermediate" : "hostile";
        return "common";
    }
    public static boolean allowed(Mob mob, boolean playerKill) {
        return !mob.level().isClientSide && mob.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
            && (!ForgeItConfig.REQUIRE_PLAYER_KILL.get() || playerKill)
            && (ForgeItConfig.PASSIVE_DROPS.get() || !tier(mob).equals("common"))
            && ForgeItConfig.DROP_MULTIPLIER.get() > 0 && !mob.getPersistentData().getBoolean(PAID);
    }
    public static List<ItemStack> roll(Mob mob, LootContext context) {
        mob.getPersistentData().putBoolean(PAID, true);
        var key = ResourceKey.create(Registries.LOOT_TABLE, ForgeIt.id("coins/" + tier(mob)));
        var table = context.getLevel().getServer().reloadableRegistries().getLootTable(key);
        List<ItemStack> raw = new ArrayList<>(); table.getRandomItemsRaw(context, raw::add);
        long base = 0;
        for (ItemStack stack : raw) base += Coin.unitValue(stack) * stack.getCount();
        long total = Math.round(base * ForgeItConfig.DROP_MULTIPLIER.get());
        List<ItemStack> result = new ArrayList<>();
        for (int i = Coin.values().length - 1; i >= 0; i--) {
            Coin coin = Coin.values()[i]; long count = total / coin.value; total %= coin.value;
            while (count > 0) { int size = (int)Math.min(64, count); result.add(coin.stack(size)); count -= size; }
        }
        return result;
    }
    /** The dragon bypasses normal death loot; the same new tier table pays once across XP waves. */
    public static void dragon(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)
            || !allowed(dragon, event.getAttackingPlayer() != null)) return;
        var params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, dragon)
            .withParameter(LootContextParams.ORIGIN, dragon.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, dragon.damageSources().generic())
            .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, event.getAttackingPlayer()).create(LootContextParamSets.ENTITY);
        var context = new LootContext.Builder(params).create(java.util.Optional.empty());
        for (ItemStack stack : roll(dragon, context)) dragon.spawnAtLocation(stack);
    }
}
