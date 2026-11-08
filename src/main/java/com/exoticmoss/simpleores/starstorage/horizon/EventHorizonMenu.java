package com.exoticmoss.simpleores.starstorage.horizon;

import com.exoticmoss.simpleores.starstorage.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/// The menu of the Event Horizon: its filter (pictures of items: clicking one with an item in the mouse cursor puts a
/// picture of it, clicking with an empty cursor takes it away) and its priority (buttons). Buttons: LOWER, HIGHER
/// (one), LOWER_MORE, HIGHER_MORE (ten).
public class EventHorizonMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 152;
    public static final int FILTER_TOP = 22;
    public static final int PRIORITY_TOP = 46;
    public static final int INVENTORY_TOP = 70;

    public static final int LOWER = 0;
    public static final int HIGHER = 1;
    public static final int LOWER_MORE = 2;
    public static final int HIGHER_MORE = 3;

    public final BlockPos pos;
    private final Container filter;
    /// Client: the priority sent by the server.
    private int syncedPriority;

    /// Client: the filter is sent by the menu itself.
    public EventHorizonMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, new SimpleContainer(EventHorizonBlockEntity.FILTER_SLOTS));
    }

    public EventHorizonMenu(int id, Inventory inventory, BlockPos pos, Container filter) {
        super(ModMenus.EVENT_HORIZON.get(), id);
        this.pos = pos;
        this.filter = filter;
        for (int i = 0; i < EventHorizonBlockEntity.FILTER_SLOTS; i++) {
            addSlot(new FilterSlot(i, 8 + i * 18, FILTER_TOP));
        }
        Player player = inventory.player;
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return player.level().getBlockEntity(pos) instanceof EventHorizonBlockEntity horizon ? horizon.priority() : syncedPriority;
            }

            @Override
            public void set(int value) {
                syncedPriority = value;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, INVENTORY_TOP + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, INVENTORY_TOP + 58));
        }
    }

    public int priority() {
        return syncedPriority;
    }

    /// A picture of an item: it can't be taken or filled like a normal slot (see clicked).
    private class FilterSlot extends Slot {
        FilterSlot(int index, int x, int y) {
            super(filter, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < EventHorizonBlockEntity.FILTER_SLOTS) {
            ItemStack carried = getCarried();
            filter.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            filter.setChanged();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /// Shift + click on an item of the inventory: a picture of it goes in the first free place of the filter.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < EventHorizonBlockEntity.FILTER_SLOTS) return ItemStack.EMPTY;
        ItemStack stack = slots.get(index).getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < EventHorizonBlockEntity.FILTER_SLOTS; i++) {
            if (!filter.getItem(i).isEmpty() && ItemStack.isSameItem(filter.getItem(i), stack)) return ItemStack.EMPTY;
        }
        for (int i = 0; i < EventHorizonBlockEntity.FILTER_SLOTS; i++) {
            if (filter.getItem(i).isEmpty()) {
                filter.setItem(i, stack.copyWithCount(1));
                filter.setChanged();
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player.level().getBlockEntity(pos) instanceof EventHorizonBlockEntity horizon)) return false;
        int change = switch (id) {
            case LOWER -> -1;
            case HIGHER -> 1;
            case LOWER_MORE -> -10;
            case HIGHER_MORE -> 10;
            default -> 0;
        };
        if (change == 0) return false;
        horizon.setPriority(horizon.priority() + change);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() instanceof EventHorizonBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
