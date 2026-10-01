package com.ticuliro.forgeit;

import net.minecraft.network.chat.Component;
import com.ticuliro.forgeit.economy.Wallet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Per-player work slot, like a crafting table. Returned on close, death or disconnection. */
public final class ReforgingMenu extends AbstractContainerMenu {
    private final SimpleContainer input = new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final Inventory inventory;
    private final DataSlot costLow = DataSlot.standalone();
    private final DataSlot costHigh = DataSlot.standalone();
    private long lastRollTick = Long.MIN_VALUE;

    public ReforgingMenu(int id, Inventory inventory) { this(id, inventory, ContainerLevelAccess.NULL); }

    public ReforgingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ForgeIt.REFORGING_MENU.get(), id);
        this.inventory = inventory;
        this.access = access;
        addSlot(new Slot(input, 0, 31, 54) {
            @Override public boolean mayPlace(ItemStack stack) { return Reforging.accepts(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 47 + col * 18, 141 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 47 + col * 18, 199));
        addDataSlot(costLow);
        addDataSlot(costHigh);
        updateTotals();
    }

    public ItemStack item() { return input.getItem(0); }
    public long balance() { return Wallet.balance(inventory.player); }
    public int cost() { return (costLow.get() & 0xFFFF) | ((costHigh.get() & 0xFFFF) << 16); }

    private void updateTotals() {
        if (!inventory.player.level().isClientSide()) {
            int value = inventory.player.getAbilities().instabuild ? 0 : ForgeItConfig.REFORGE_COST.get();
            costLow.set(value & 0xFFFF);
            costHigh.set(value >>> 16);
        }
    }

    @Override public void broadcastChanges() { updateTotals(); super.broadcastChanges(); }
    @Override public boolean stillValid(Player player) {
        return player == inventory.player && stillValid(access, player, ForgeIt.REFORGING_TABLE.get());
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0 || player.level().isClientSide() || !stillValid(player) || player.isSpectator()) return false;
        if (!Reforging.accepts(item())) return false;
        long tick = player.level().getGameTime();
        if (lastRollTick != Long.MIN_VALUE && tick - lastRollTick < 5) return false;
        int price = player.getAbilities().instabuild ? 0 : ForgeItConfig.REFORGE_COST.get();
        // Prepare the result before spending. No client-supplied price, result or item is trusted.
        ItemStack result = item().copy();
        ReforgeModifier modifier = ReforgeModifier.roll(player.getRandom());
        Reforging.apply(result, modifier);
        if (!Wallet.debit(player, price)) {
            player.displayClientMessage(Component.translatable("message.forgeit.no_coins"), true);
            return false;
        }
        input.setItem(0, result);
        lastRollTick = tick;
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F,
            modifier == ReforgeModifier.LEGENDARY ? 1.3F : 1.0F));
        player.displayClientMessage(Component.translatable("message.forgeit.result", modifier.displayName()), true);
        broadcastChanges();
        return true;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) clearContainer(player, input);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (Reforging.accepts(stack) && !slots.get(0).hasItem()) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
