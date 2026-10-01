package com.ticuliro.forgeit.test;

import com.ticuliro.forgeit.*;
import com.ticuliro.forgeit.economy.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ForgeIt.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WalletGameTests {
    private static Player player(GameTestHelper h) { return h.makeMockPlayer(GameType.SURVIVAL); }
    private static long physical(Player player) {
        long value = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i); value += Coin.unitValue(stack) * stack.getCount();
        }
        return value;
    }
    @GameTest(template = "empty")
    public static void collectIntoWalletWithFullInventory(GameTestHelper h) {
        Player player = player(h);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        ItemEntity entity = new ItemEntity(h.getLevel(), 0, 0, 0, Coin.PLATINUM.stack(64)); entity.setNoPickUpDelay();
        entity.playerTouch(player); entity.playerTouch(player);
        h.assertTrue(Wallet.balance(player) == 320000, "Pickup credits once with full inventory");
        h.assertTrue(entity.isRemoved() && physical(player) == 0, "No currency occupies inventory slots");
        h.assertTrue(player.getInventory().getItem(0).getCount() == 64, "Inventory preserved"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void pickupRespectsOwnerDelayAndWalletLimit(GameTestHelper h) {
        Player player = player(h); ItemEntity entity = new ItemEntity(h.getLevel(), 0, 0, 0, Coin.GOLD.stack(2));
        entity.setDefaultPickUpDelay(); entity.playerTouch(player);
        h.assertTrue(Wallet.balance(player) == 0, "Pickup delay respected");
        entity.setNoPickUpDelay(); entity.setTarget(java.util.UUID.randomUUID()); entity.playerTouch(player);
        h.assertTrue(Wallet.balance(player) == 0, "Owner protection respected");
        entity.setTarget(player.getUUID()); Wallet.credit(player, Wallet.MAX_BALANCE - 999); entity.playerTouch(player);
        h.assertTrue(!entity.isRemoved() && entity.getItem().getCount() == 2, "At limit the coins stay on the ground");
        h.assertTrue(physical(player) == 0 && Wallet.balance(player) == Wallet.MAX_BALANCE - 999, "No overflow or inventory fallback"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void withdrawConvertsAndDepositConservesValue(GameTestHelper h) {
        Player player = player(h); Wallet.credit(player, 10601);
        h.assertTrue(Wallet.withdraw(player, Coin.PLATINUM, 1) == Wallet.Result.SUCCESS, "Platinum withdrawal");
        h.assertTrue(Wallet.withdraw(player, Coin.GOLD, 3) == Wallet.Result.SUCCESS, "Gold withdrawal");
        h.assertTrue(Wallet.withdraw(player, Coin.SILVER, 7) == Wallet.Result.SUCCESS, "Silver withdrawal");
        h.assertTrue(Wallet.withdraw(player, Coin.COPPER, 51) == Wallet.Result.SUCCESS, "Copper withdrawal");
        h.assertTrue(Wallet.balance(player) == 3700 && physical(player) == 6901, "Exact value across denominations");
        h.assertTrue(Wallet.depositInventory(player, false) == Wallet.Result.SUCCESS, "Deposit succeeds");
        h.assertTrue(Wallet.balance(player) == 10601 && physical(player) == 0, "No value created or lost");
        h.assertTrue(Wallet.depositInventory(player, false) == Wallet.Result.EMPTY, "Repeat deposit cannot duplicate"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void fullInventoryAndInvalidWithdrawalAreAtomic(GameTestHelper h) {
        Player player = player(h); Wallet.credit(player, 10000);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(0, Coin.COPPER.stack(63));
        h.assertTrue(Wallet.withdraw(player, Coin.COPPER, 2) == Wallet.Result.FULL, "Complete request must fit");
        h.assertTrue(Wallet.balance(player) == 10000 && physical(player) == 63, "Failure is atomic");
        h.assertTrue(Wallet.withdraw(player, Coin.COPPER, 1) == Wallet.Result.SUCCESS, "Merges into existing stack");
        for (int count : new int[]{0,-1,Integer.MIN_VALUE,Integer.MAX_VALUE,2305})
            h.assertTrue(Wallet.withdraw(player, Coin.PLATINUM, count) == Wallet.Result.INVALID, "Invalid count rejected");
        h.assertTrue(Wallet.withdraw(player, null, 1) == Wallet.Result.INVALID, "Unknown denomination rejected");
        h.assertTrue(Wallet.withdraw(player, Coin.PLATINUM, 3) == Wallet.Result.INSUFFICIENT, "Insufficient funds rejected");
        h.assertTrue(Wallet.balance(player) == 9999 && physical(player) == 64, "Invalid calls cannot mint or charge"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void walletPersistsThroughPlayerNbt(GameTestHelper h) {
        Player original = player(h); Wallet.credit(original, 4_000_000_123L);
        CompoundTag saved = new CompoundTag(); original.saveWithoutId(saved);
        Player loaded = player(h); loaded.load(saved);
        h.assertTrue(Wallet.balance(loaded) == 4_000_000_123L, "Attachment save/load exceeds int range");
        h.assertTrue(!Wallet.credit(loaded, Long.MAX_VALUE) && !Wallet.debit(loaded, -1), "Overflow and negative debit rejected");
        h.assertTrue(Wallet.balance(loaded) == 4_000_000_123L, "Balance unchanged after rejected operations"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void walletSurvivesDeathAndPhysicalTrading(GameTestHelper h) {
        Player sender = player(h); Wallet.credit(sender, 12345);
        Player respawned = player(h);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(respawned, sender, true));
        h.assertTrue(Wallet.balance(respawned) == 12345, "Balance survives actual clone event without multiplication");
        Wallet.withdraw(respawned, Coin.GOLD, 2);
        ItemStack trade = respawned.getInventory().removeItem(0, 2);
        Player receiver = player(h);
        ItemEntity drop = new ItemEntity(h.getLevel(), 0, 0, 0, trade); drop.setNoPickUpDelay(); drop.playerTouch(receiver);
        h.assertTrue(Wallet.balance(respawned) == 11345 && Wallet.balance(receiver) == 1000, "Physical transfer preserves total value");
        h.assertTrue(physical(respawned) == 0 && physical(receiver) == 0 && drop.isRemoved(), "Trade pays once and coins are consumed");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void packetsKeepFullPrecisionAndRequestsAreValidated(GameTestHelper h) {
        var buf = Unpooled.buffer();
        try {
            WalletNetwork.Balance.CODEC.encode(buf, new WalletNetwork.Balance(Wallet.MAX_BALANCE));
            h.assertTrue(WalletNetwork.Balance.CODEC.decode(buf).amount() == Wallet.MAX_BALANCE, "Network uses 64-bit balance");
        } finally { buf.release(); }
        var player = h.makeMockServerPlayerInLevel(); Wallet.credit(player, 10000);
        h.assertTrue(WalletNetwork.perform(player, new WalletNetwork.Action(0, Coin.GOLD.ordinal(), 1)), "Valid server request");
        h.assertTrue(!WalletNetwork.perform(player, new WalletNetwork.Action(0, Coin.GOLD.ordinal(), 1)), "Same-tick replay rejected");
        h.assertTrue(Wallet.balance(player) == 9500 && physical(player) == 500, "One request charged");
        h.assertTrue(!WalletNetwork.perform(player, new WalletNetwork.Action(99, 0, 0)), "Unknown action rejected");
        player.containerMenu = new ReforgingMenu(8, player.getInventory(), ContainerLevelAccess.NULL);
        h.assertTrue(!WalletNetwork.perform(player, new WalletNetwork.Action(0, 0, 1)), "Requires player's own inventory menu"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void oldCoinsMigrateWithoutConsumingWithdrawnCurrency(GameTestHelper h) {
        Player player = player(h); player.getInventory().setItem(0, new ItemStack(ForgeIt.COIN.get(), 64));
        player.getInventory().setItem(1, Coin.GOLD.stack(2)); player.getInventory().setItem(2, new ItemStack(Items.DIAMOND));
        Wallet.depositInventory(player, true); Wallet.depositInventory(player, true);
        h.assertTrue(Wallet.balance(player) == 64, "Legacy value credits once");
        h.assertTrue(player.getInventory().getItem(1).getCount() == 2, "Withdrawn currency remains physical");
        h.assertTrue(player.getInventory().getItem(2).is(Items.DIAMOND), "Other inventory preserved"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void allDenominationsNormalizeExactly(GameTestHelper h) {
        for (long value : new long[]{0,1,49,50,499,500,4999,5000,5551,4_000_000_123L,Wallet.MAX_BALANCE}) {
            long total = 0;
            for (Coin coin : Coin.values()) total += coin.displayedCount(value) * coin.value;
            h.assertTrue(total == value, "Denomination decomposition is exact: " + value);
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void commonLootKeepsVanillaDrops(GameTestHelper h) {
        Player player = player(h); var cow = h.spawn(EntityType.COW, new BlockPos(1,1,1));
        cow.hurt(cow.damageSources().playerAttack(player), 1000);
        h.runAfterDelay(2, () -> {
            var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, cow.getBoundingBox().inflate(2));
            long total = drops.stream().mapToLong(e -> Coin.unitValue(e.getItem()) * e.getItem().getCount()).sum();
            h.assertTrue((total >= 1 && total <= 8) || (total >= 51 && total <= 58), "Common copper table plus optional silver");
            h.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.BEEF)), "Vanilla loot preserved");
            h.assertTrue(drops.stream().noneMatch(e -> e.getItem().is(ForgeIt.COIN.get())), "Old drops removed"); h.succeed();
        });
    }
}
