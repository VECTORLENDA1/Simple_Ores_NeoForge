package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/// The menu of the Binary Star: the slot of its Magnetic Field Upgrades and the player's inventory (the blocks around
/// it are drawn by BinaryScreen from BinaryView). With the inventory, an item can be taken with the mouse and put as
/// the item a slot keeps.
public class BinaryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 220;
    /// Height of the inventory part at the bottom of the panel.
    public static final int INVENTORY_HEIGHT = 3 * 18 + 4 + 18 + 22;

    public final BlockPos pos;
    public final int height;
    /// Where the upgrade slot is (top left of the panel).
    public static final int UPGRADE_X = 7;
    public static final int UPGRADE_Y = 4;

    /// Client: the upgrade is sent by the menu itself.
    public BinaryMenu(int id, Inventory inventory, BlockPos pos, int height) {
        this(id, inventory, pos, height, new SimpleContainer(1));
    }

    public BinaryMenu(int id, Inventory inventory, BlockPos pos, int height, Container upgrades) {
        super(ModMenus.BINARY.get(), id);
        this.pos = pos;
        this.height = height;
        addSlot(new Slot(upgrades, 0, UPGRADE_X, UPGRADE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.MAGNETIC_FIELD_UPGRADE.get());
            }

            @Override
            public int getMaxStackSize() {
                return BinaryStarBlockEntity.MAX_UPGRADES;
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return BinaryStarBlockEntity.MAX_UPGRADES;
            }
        });
        addStandardInventorySlots(inventory, (WIDTH - 9 * 18) / 2 + 1, height - INVENTORY_HEIGHT + 12);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() instanceof BinaryStarBlock
                && player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= BinaryAction.RANGE * BinaryAction.RANGE;
    }
}
