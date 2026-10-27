package com.exoticmoss.simpleores.starstorage.magnetar;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.exoticmoss.simpleores.starstorage.ModMenus;

/// The menu of the Magnetar Core: its upgrade slots (see MagnetarBlockEntity) and the player's inventory.
public class MagnetarMenu extends AbstractContainerMenu implements com.exoticmoss.simpleores.starstorage.network.StarTabs.Anchored {
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


    public static final int WIDTH = 176;
    public static final int HEIGHT = 208;
    public static final int UPGRADES_TOP = 26;
    /// The info of the network (MagnetarInfo) is drawn between the upgrades and the inventory.
    public static final int INFO_TOP = 50;
    public static final int INVENTORY_TOP = 126;

    public final BlockPos pos;
    private final Container upgrades;
    private final Player player;
    private int ticks;

    /// Client: the upgrades are sent by the menu itself.
    public MagnetarMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, new SimpleContainer(MagnetarBlockEntity.SLOTS));
    }

    public MagnetarMenu(int id, Inventory inventory, BlockPos pos, Container upgrades) {
        super(ModMenus.MAGNETAR.get(), id);
        this.pos = pos;
        this.upgrades = upgrades;
        this.player = inventory.player;
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

    /// Server: the info of the network is sent again every second.
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (++ticks % 20 == 0 && player instanceof ServerPlayer serverPlayer
                && player.level().getBlockEntity(pos) instanceof MagnetarBlockEntity magnetar) {
            MagnetarInfo.send(serverPlayer, magnetar);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() instanceof MagnetarBlock
                && com.exoticmoss.simpleores.starstorage.network.StarTabs.near(player, anchor(), 8);
    }
}
