package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/// The menu of the Magnetar Core: its upgrade slots (see MagnetarBlockEntity) and the player's inventory.
public class MagnetarMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 150;
    public static final int UPGRADES_TOP = 26;
    public static final int INVENTORY_TOP = 68;

    public final BlockPos pos;
    private final Container upgrades;

    /// Client: the upgrades are sent by the menu itself.
    public MagnetarMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, new SimpleContainer(MagnetarBlockEntity.SLOTS));
    }

    public MagnetarMenu(int id, Inventory inventory, BlockPos pos, Container upgrades) {
        super(ModMenus.MAGNETAR.get(), id);
        this.pos = pos;
        this.upgrades = upgrades;
        addSlot(new UpgradeSlot(MagnetarBlockEntity.SPEED_SLOT, slotX(0), UPGRADES_TOP));
        addSlot(new UpgradeSlot(MagnetarBlockEntity.RANGE_SLOT, slotX(1), UPGRADES_TOP));
        for (int i = 0; i < MagnetarBlockEntity.LINK_SLOTS; i++) {
            addSlot(new UpgradeSlot(MagnetarBlockEntity.LINK_START + i, slotX(3 + i), UPGRADES_TOP));
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_TOP);
    }

    /// x of the upgrade slot in column "column" (the speed and range slots, a gap, the links).
    public static int slotX(int column) {
        return 8 + column * 18 + (column >= 3 ? 9 : 0);
    }

    private class UpgradeSlot extends Slot {
        UpgradeSlot(int index, int x, int y) {
            super(upgrades, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return MagnetarBlockEntity.fits(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return getContainerSlot() < MagnetarBlockEntity.LINK_START ? MagnetarBlockEntity.MAX_UPGRADES : 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return getMaxStackSize();
        }
    }

    /// Shift + click: an upgrade goes to its slot, an upgrade of the Magnetar goes to the inventory.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack before = stack.copy();
        if (index < MagnetarBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, MagnetarBlockEntity.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, MagnetarBlockEntity.SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() instanceof MagnetarBlock
                && player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 64;
    }
}
