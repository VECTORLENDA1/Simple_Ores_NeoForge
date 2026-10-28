package com.exoticmoss.simpleores.starstorage.network;

import com.exoticmoss.simpleores.starstorage.cell.FluidCellItem;
import com.exoticmoss.simpleores.starstorage.cell.FluidContents;
import com.exoticmoss.simpleores.starstorage.cell.StarCellItem;
import com.exoticmoss.simpleores.util.ItemStackTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The Magnetar network seen by the pipes, hoppers and machines of other mods (through the Binary Star):
/// they can put items / liquids in the network and take them out, like with a chest.
///
/// Each kind of item (or liquid) of the network is one "slot", and there is always one empty slot more for new kinds.
/// The contents are read once per tick (reading them is not free), and again after every change.
public final class NetworkHandler {
    private NetworkHandler() {
    }

    // ---------------------------------------------------------------- items

    public static class Items implements IItemHandler {
        private final Level level;
        private final BlockPos pos;
        private List<ItemStackTemplate> kinds = List.of();
        private List<Long> amounts = List.of();
        private long space;
        private long readTick = Long.MIN_VALUE;

        public Items(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
        }

        private StarNetwork network() {
            return StarNetwork.at(level, pos);
        }

        private void refresh() {
            if (level.getGameTime() == readTick) return;
            readTick = level.getGameTime();
            StarNetwork network = network();
            List<ItemStackTemplate> list = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            if (!network.drives().isEmpty()) {
                for (Map.Entry<ItemStackTemplate, Long> entry : network.contents()) {
                    list.add(entry.getKey());
                    counts.add(entry.getValue());
                }
            }
            kinds = list;
            amounts = counts;
            space = network.drives().isEmpty() ? 0 : Math.max(0, network.capacity() - network.usedItems());
        }

        private boolean accepts(ItemStack stack) {
            return !(stack.getItem() instanceof StarCellItem && !StarCellItem.isEmptyCell(stack))
                    && !(stack.getItem() instanceof FluidCellItem && !FluidCellItem.getContents(stack).isEmpty());
        }

        @Override
        public int getSlots() {
            refresh();
            return kinds.size() + 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            refresh();
            if (slot >= kinds.size()) return ItemStack.EMPTY;
            ItemStack stack = kinds.get(slot).create();
            return stack.copyWithCount((int) Math.min(amounts.get(slot), stack.getMaxStackSize()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            refresh();
            if (stack.isEmpty() || !accepts(stack)) return stack;
            // A slot of another kind takes nothing (the empty slot at the end takes any kind)
            if (slot < kinds.size() && !ItemStack.isSameItemSameComponents(kinds.get(slot).create(), stack)) return stack;
            int inserted = (int) Math.min(stack.getCount(), space);
            if (inserted <= 0) return stack;
            if (!simulate) {
                ItemStack left = network().insert(stack.copyWithCount(inserted));
                // (should not happen) no room any more: the items drop next to the Binary Star
                if (!left.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, left);
                readTick = Long.MIN_VALUE;
            }
            return stack.copyWithCount(stack.getCount() - inserted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            refresh();
            if (amount <= 0 || slot >= kinds.size()) return ItemStack.EMPTY;
            ItemStack kind = kinds.get(slot).create();
            int extracted = (int) Math.min(Math.min(amount, kind.getMaxStackSize()), amounts.get(slot));
            if (extracted <= 0) return ItemStack.EMPTY;
            if (simulate) return kind.copyWithCount(extracted);
            readTick = Long.MIN_VALUE;
            return network().extract(kind.copyWithCount(1), extracted);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return accepts(stack);
        }
    }

    // ---------------------------------------------------------------- liquids

    public static class Fluids implements IFluidHandler {
        private final Level level;
        private final BlockPos pos;
        private List<FluidContents.Entry> kinds = List.of();
        private long space;
        private long readTick = Long.MIN_VALUE;

        public Fluids(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos.immutable();
        }

        private StarNetwork network() {
            return StarNetwork.at(level, pos);
        }

        private void refresh() {
            if (level.getGameTime() == readTick) return;
            readTick = level.getGameTime();
            StarNetwork network = network();
            kinds = network.drives().isEmpty() ? List.of() : List.copyOf(network.fluids());
            space = network.drives().isEmpty() ? 0 : Math.max(0, network.fluidCapacity() - network.fluidUsed());
        }

        @Override
        public int getTanks() {
            refresh();
            return kinds.size() + 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            refresh();
            if (tank >= kinds.size()) return FluidStack.EMPTY;
            FluidContents.Entry entry = kinds.get(tank);
            return new FluidStack(entry.fluid(), (int) Math.min(entry.amount(), Integer.MAX_VALUE));
        }

        @Override
        public int getTankCapacity(int tank) {
            refresh();
            long have = tank < kinds.size() ? kinds.get(tank).amount() : 0;
            return (int) Math.min(Integer.MAX_VALUE, have + space);
        }

        /// The Fluid Cells keep only the liquid (not extra data of other mods).
        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.getComponentsPatch().isEmpty();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            refresh();
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int filled = (int) Math.min(resource.getAmount(), space);
            if (filled <= 0) return 0;
            if (action.execute()) {
                filled = (int) network().insertFluid(resource.getFluid(), filled);
                readTick = Long.MIN_VALUE;
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            refresh();
            if (resource.isEmpty()) return FluidStack.EMPTY;
            return drain(resource.getFluid(), resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            refresh();
            if (kinds.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
            return drain(kinds.get(0).fluid(), maxDrain, action);
        }

        private FluidStack drain(Fluid fluid, int amount, FluidAction action) {
            long have = 0;
            for (FluidContents.Entry entry : kinds) {
                if (entry.fluid() == fluid) have += entry.amount();
            }
            int drained = (int) Math.min(amount, have);
            if (drained <= 0) return FluidStack.EMPTY;
            if (action.execute()) {
                drained = (int) network().extractFluid(fluid, drained);
                readTick = Long.MIN_VALUE;
            }
            return drained <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, drained);
        }
    }
}
