package com.exoticmoss.simpleores.starstorage.binary;

import com.exoticmoss.simpleores.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import com.exoticmoss.simpleores.starstorage.ModMenus;

/// The menu of the Binary Star: the slot of its Magnetic Field Upgrades and the player's inventory (the blocks around
/// it are drawn by BinaryScreen from BinaryView). With the inventory, an item can be taken with the mouse and put as
/// the item a slot keeps.
public class BinaryMenu extends AbstractContainerMenu implements com.exoticmoss.simpleores.starstorage.network.StarTabs.Anchored {
    /// The block the player is next to (this one, or the one whose screen opened this one with a tab).
    private BlockPos anchor;

    @Override
    public BlockPos menuPos() {
        return pos;
    }

    @Override
    public BlockPos anchor() {
        return anchor == null ? pos : anchor;
    }

    @Override
    public void setAnchor(BlockPos anchor) {
        this.anchor = anchor;
    }


    public static final int WIDTH = 260;
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
        addPlayerSlots(inventory, (WIDTH - 9 * 18) / 2 + 1, height - INVENTORY_HEIGHT + 12);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() instanceof BinaryStarBlock
                && com.exoticmoss.simpleores.starstorage.network.StarTabs.near(player, anchor(), BinaryAction.RANGE);
    }

    /// The 27 slots of the inventory and the 9 of the hotbar below them (4 pixels lower).
    private void addPlayerSlots(net.minecraft.world.entity.player.Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new net.minecraft.world.inventory.Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new net.minecraft.world.inventory.Slot(inventory, column, left + column * 18, top + 58));
        }
    }
}
