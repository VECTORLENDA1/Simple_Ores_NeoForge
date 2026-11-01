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
import net.minecraft.world.level.material.Fluid;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;

/// The Event Horizon: the inventories and tanks of other mods it touches (on its 6 sides: chests, barrels, drawers,
/// tanks...) become part of the network of the Magnetar whose field it is in. Their items show in the terminals, can
/// be taken out and used by the auto-craft, and the network can put items in them (see StarNetwork.External).
///  - priority: when the network keeps something, first the inventories with a priority above 0 (the highest first),
///    then the cells, then the other inventories. Things are taken from the lowest priority first;
///  - filter: when it has items, only those items are seen and kept through this Event Horizon; when it has buckets
///    of liquids, only those liquids (the buckets don't limit the items, and the items don't limit the liquids).
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

    /// True if this item goes through this Event Horizon (always when the filter has no items).
    public boolean accepts(ItemStack stack) {
        boolean any = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack item = filter.getItem(i);
            if (item.isEmpty() || StarFluids.fromBucket(item) != null) continue;
            any = true;
            if (stack.is(item.getItem())) return true;
        }
        return !any;
    }

    /// True if this liquid goes through this Event Horizon (always when the filter has no buckets of liquids).
    public boolean acceptsFluid(Fluid fluid) {
        boolean any = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            Fluid kept = StarFluids.fromBucket(filter.getItem(i));
            if (kept == null) continue;
            any = true;
            if (kept.isSame(fluid)) return true;
        }
        return !any;
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
