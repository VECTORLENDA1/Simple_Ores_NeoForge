package com.exoticmoss.simpleores.starstorage.horizon;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// The Event Horizon: the inventories and tanks of other mods it touches (on its 6 sides: chests, barrels, drawers,
/// tanks...) become part of the network of the Magnetar whose field it is in. Their items show in the terminals, can
/// be taken out and used by the auto-craft, and the network can put items in them (see StarNetwork.External).
///  - priority: when the network keeps something, first the inventories with a priority above 0 (the highest first),
///    then the cells, then the other inventories. Things are taken from the lowest priority first;
///  - filter: when it has items, only those items are seen and kept through this Event Horizon.
public class EventHorizonBlockEntity extends BlockEntity {
    public static final int FILTER_SLOTS = 9;
    public static final int MIN_PRIORITY = -99;
    public static final int MAX_PRIORITY = 99;

    private int priority;
    private final SimpleContainer filter = new SimpleContainer(FILTER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            EventHorizonBlockEntity.this.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };

    public EventHorizonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EVENT_HORIZON_BE.get(), pos, state);
    }

    public int priority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.clamp(priority, MIN_PRIORITY, MAX_PRIORITY);
        setChanged();
    }

    public SimpleContainer filter() {
        return filter;
    }

    /// True if this item goes through this Event Horizon (always without a filter).
    public boolean accepts(ItemStack stack) {
        if (filter.isEmpty()) return true;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack item = filter.getItem(i);
            if (!item.isEmpty() && stack.is(item.getItem())) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("priority", priority);
        ContainerHelper.saveAllItems(tag, filter.getItems(), registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        priority = tag.getInt("priority");
        filter.getItems().replaceAll(stack -> ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, filter.getItems(), registries);
    }
}
