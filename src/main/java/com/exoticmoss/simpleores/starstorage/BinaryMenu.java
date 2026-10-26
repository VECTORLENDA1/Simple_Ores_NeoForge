package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/// The menu of the Binary Star: only the player's inventory (the blocks around it are drawn by BinaryScreen
/// from BinaryView). With the inventory, an item can be taken with the mouse and put as the item a slot keeps.
public class BinaryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 220;
    /// Height of the inventory part at the bottom of the panel.
    public static final int INVENTORY_HEIGHT = 3 * 18 + 4 + 18 + 22;

    public final BlockPos pos;
    public final int height;

    public BinaryMenu(int id, Inventory inventory, BlockPos pos, int height) {
        super(ModMenus.BINARY.get(), id);
        this.pos = pos;
        this.height = height;
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
