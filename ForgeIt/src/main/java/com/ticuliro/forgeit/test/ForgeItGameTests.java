package com.ticuliro.forgeit.test;

import com.ticuliro.forgeit.*;
import com.ticuliro.forgeit.economy.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration tests run in the actual NeoForge GameTestServer, never during normal play. */
@GameTestHolder(ForgeIt.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForgeItGameTests {
    @GameTest(template = "empty")
    public static void dragonPaysOnceAcrossExperienceWaves(GameTestHelper h) {
        var dragon = EntityType.ENDER_DRAGON.create(h.getLevel());
        var pos = h.absolutePos(new BlockPos(2, 1, 2));
        dragon.setPos(pos.getX(), pos.getY(), pos.getZ());
        var event = new net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent(dragon, h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), 100);
        CoinDrops.dragon(event);
        CoinDrops.dragon(event);
        int coins = h.getLevel().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2),
            e -> e.getItem().is(ForgeIt.PLATINUM_COIN.get())).stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(coins >= 2 && coins <= 5, "Dragon must pay once, not once per XP wave");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void playerKillActuallyDropsCoins(GameTestHelper h) {
        Player player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
        zombie.hurt(zombie.damageSources().playerAttack(player), 1000.0F);
        h.runAfterDelay(2, () -> {
            int coins = h.getLevel().getEntitiesOfClass(ItemEntity.class, zombie.getBoundingBox().inflate(3),
                e -> e.getItem().is(ForgeIt.SILVER_COIN.get())).stream().mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(coins >= 1 && coins <= 3, "A real zombie death must produce coin entities");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void preservesItemAndReplacesModifier(GameTestHelper h) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        int base = stack.getMaxDamage();
        stack.setDamageValue(401);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Ticuliro"));
        stack.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 3);
        var enchantments = stack.get(DataComponents.ENCHANTMENTS);
        CompoundTag root = new CompoundTag(); root.putString("other_mod", "preserved");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        Reforging.apply(stack, ReforgeModifier.STURDY);
        h.assertTrue(stack.getMaxDamage() == Math.round(base * 1.25), "Actual maximum durability must increase");
        Reforging.apply(stack, ReforgeModifier.FRAGILE);
        h.assertTrue(stack.getMaxDamage() == Math.round(base * 0.7), "Rerolls must use original durability");
        h.assertTrue(stack.get(DataComponents.ENCHANTMENTS).equals(enchantments), "Enchantments must survive");
        h.assertTrue(stack.getHoverName().getString().equals("Ticuliro"), "Custom name must survive");
        h.assertTrue(Reforging.modifier(stack) == ReforgeModifier.FRAGILE, "Only latest modifier remains");
        h.assertTrue(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("other_mod").equals("preserved"), "Unrelated mod data survives");
        h.assertTrue(Reforging.data(stack).getInt("rolls") == 2, "Roll counter");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void itemSurvivesSerialization(GameTestHelper h) {
        ItemStack original = new ItemStack(Items.IRON_PICKAXE);
        Reforging.apply(original, ReforgeModifier.LEGENDARY);
        ItemStack loaded = ItemStack.parse(h.getLevel().registryAccess(), original.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(ItemStack.matches(original, loaded), "Item including modifier must persist through NBT save/load");
        h.assertTrue(Reforging.modifier(loaded) == ReforgeModifier.LEGENDARY, "Modifier survives reload");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void attributesDoNotAccumulate(GameTestHelper h) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        for (int i = 0; i < 100; i++) Reforging.apply(sword, ReforgeModifier.LEGENDARY);
        var mods = sword.getAttributeModifiers().modifiers();
        long own = mods.stream().filter(e -> e.modifier().id().getNamespace().equals(ForgeIt.MOD_ID)).count();
        h.assertTrue(own == 2, "Exactly damage and speed modifiers after 100 rerolls");
        h.assertTrue(mods.stream().anyMatch(e -> e.attribute().equals(Attributes.ATTACK_DAMAGE) && Math.abs(e.modifier().amount() - 0.15) < 0.00001), "Damage bonus is active");
        Reforging.apply(sword, ReforgeModifier.STURDY);
        h.assertTrue(sword.getAttributeModifiers().modifiers().stream().noneMatch(e -> e.modifier().id().getNamespace().equals(ForgeIt.MOD_ID)), "Sturdy removes previous attack bonuses");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chargesExactlyAndReturnsItem(GameTestHelper h) {
        Player player = h.makeMockServerPlayerInLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, ForgeIt.REFORGING_TABLE.get().defaultBlockState());
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.getAbilities().instabuild = false;
        int cost = ForgeItConfig.REFORGE_COST.get();
        Wallet.credit(player, cost - 1);
        var menu = new ReforgingMenu(7, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos));
        menu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        h.assertTrue(!menu.clickMenuButton(player, 0), "Insufficient balance rejects roll");
        h.assertTrue(Reforging.modifier(menu.item()) == null, "Rejected roll must not alter the item");
        h.assertTrue(Wallet.balance(player) == cost - 1, "Rejected roll must not charge");
        Wallet.credit(player, 2);
        h.assertTrue(menu.clickMenuButton(player, 0), "Valid roll succeeds");
        h.assertTrue(Wallet.balance(player) == 1, "Exact cost across split stacks");
        h.assertTrue(Reforging.modifier(menu.item()) != null, "Result has modifier");
        h.assertTrue(!menu.clickMenuButton(player, 0), "Repeated same-tick request is rejected");
        h.assertTrue(!menu.clickMenuButton(player, 9), "Unknown button is rejected");
        ItemStack expected = menu.item().copy();
        menu.removed(player);
        h.assertTrue(menu.item().isEmpty(), "Closing clears temporary work slot");
        boolean returned = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (ItemStack.matches(expected, player.getInventory().getItem(i))) returned = true;
        }
        h.assertTrue(returned, "Closing returns the exact item to the owner");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectsInvalidInputsAndRemoteUse(GameTestHelper h) {
        h.assertTrue(!Reforging.accepts(new ItemStack(Items.DIRT)), "Blocks are rejected");
        h.assertTrue(!Reforging.accepts(new ItemStack(Items.DIAMOND_CHESTPLATE)), "Armor is outside this release");
        for (var item : new net.minecraft.world.item.Item[]{Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_HOE})
            h.assertTrue(Reforging.accepts(new ItemStack(item)), "Every advertised tool is accepted");
        Player player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, ForgeIt.REFORGING_TABLE.get().defaultBlockState());
        player.setPos(pos.getX() + 100, pos.getY(), pos.getZ());
        var menu = new ReforgingMenu(1, player.getInventory(), ContainerLevelAccess.create(h.getLevel(), pos));
        menu.getSlot(0).set(new ItemStack(Items.IRON_AXE));
        h.assertTrue(!menu.clickMenuButton(player, 0), "Cannot reforge remotely");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rarityAndRewardRanges(GameTestHelper h) {
        RandomSource random = RandomSource.create(424242L);
        int[] count = new int[ReforgeModifier.values().length];
        for (int i = 0; i < 100000; i++) count[ReforgeModifier.roll(random).ordinal()]++;
        for (ReforgeModifier m : ReforgeModifier.values())
            h.assertTrue(Math.abs(count[m.ordinal()] / 1000.0 - m.weight) < 0.7, "Published probability holds: " + m.id);
        var cow = EntityType.COW.create(h.getLevel());
        var zombie = EntityType.ZOMBIE.create(h.getLevel());
        var ravager = EntityType.RAVAGER.create(h.getLevel());
        var wither = EntityType.WITHER.create(h.getLevel());
        h.assertTrue(CoinDrops.tier(cow).equals("common"), "Passive range");
        h.assertTrue(CoinDrops.tier(zombie).equals("hostile"), "Common hostile range");
        h.assertTrue(CoinDrops.tier(ravager).equals("rare"), "Miniboss range");
        h.assertTrue(CoinDrops.tier(wither).equals("boss"), "Boss range");
        h.succeed();
    }
}
