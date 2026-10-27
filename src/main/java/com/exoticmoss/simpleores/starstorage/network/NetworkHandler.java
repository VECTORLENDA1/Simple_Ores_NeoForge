package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.exoticmoss.simpleores.starstorage.cell.FluidCellItem;
import com.exoticmoss.simpleores.starstorage.cell.FluidContents;
import com.exoticmoss.simpleores.starstorage.cell.StarCellItem;

/// The Magnetar network seen by the pipes, hoppers and machines of other mods (through the Binary Star):
/// they can put items / liquids in the network and take them out, like with a chest.
///
/// Each kind of item (or liquid) of the network is one "slot", and there is always one empty slot more for new kinds.
/// Other mods use transactions (they try, and then keep or cancel what they did): while a transaction is open the
/// changes are only written down ("pending"), and only when it is kept (root commit) the network really changes.
public abstract class NetworkHandler<T extends Resource> extends SnapshotJournal<Map<T, Long>> implements ResourceHandler<T> {
    protected final Level level;
    protected final BlockPos pos;
    /// What the open transactions did: + put in, - taken out.
    private Map<T, Long> pending = new HashMap<>();
    /// The contents of the network, read once per tick (reading it is not free).
    private List<T> resources = List.of();
    private Map<T, Long> amounts = Map.of();
    private long space;
    private long readTick = Long.MIN_VALUE;

    protected NetworkHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    // ---------------------------------------------------------------- what each kind does

    /// The kinds in the network and their amounts (most first).
    protected abstract void read(StarNetwork network, List<T> resources, Map<T, Long> amounts);

    /// How much more fits in the network.
    protected abstract long space(StarNetwork network);

    /// Can this go in the network at all?
    protected abstract boolean accepts(T resource);

    /// Really puts it in / takes it out (after the transaction was kept).
    protected abstract void put(StarNetwork network, T resource, long amount);

    protected abstract void take(StarNetwork network, T resource, long amount);

    protected abstract T empty();

    // ---------------------------------------------------------------- the network now

    private void refresh() {
        if (level.getGameTime() == readTick || isInTransaction()) return;
        readTick = level.getGameTime();
        StarNetwork network = StarNetwork.at(level, pos);
        List<T> list = new ArrayList<>();
        Map<T, Long> map = new HashMap<>();
        if (!network.drives().isEmpty()) read(network, list, map);
        resources = list;
        amounts = map;
        space = network.drives().isEmpty() ? 0 : space(network);
    }

    private long stored(T resource) {
        return Math.max(0, amounts.getOrDefault(resource, 0L) + pending.getOrDefault(resource, 0L));
    }

    private long pendingIn() {
        long total = 0;
        for (long delta : pending.values()) total += Math.max(0, delta);
        return total;
    }

    // ---------------------------------------------------------------- ResourceHandler

    @Override
    public int size() {
        refresh();
        return resources.size() + 1;
    }

    @Override
    public T getResource(int index) {
        refresh();
        return index < resources.size() ? resources.get(index) : empty();
    }

    @Override
    public long getAmountAsLong(int index) {
        refresh();
        return index < resources.size() ? stored(resources.get(index)) : 0;
    }

    @Override
    public long getCapacityAsLong(int index, T resource) {
        refresh();
        T at = getResource(index);
        long have = at.isEmpty() ? 0 : stored(at);
        return Math.min(Long.MAX_VALUE / 2, have + Math.max(0, space - pendingIn()));
    }

    @Override
    public boolean isValid(int index, T resource) {
        return accepts(resource);
    }

    @Override
    public int insert(int index, T resource, int amount, TransactionContext transaction) {
        refresh();
        if (amount <= 0 || resource.isEmpty() || !accepts(resource)) return 0;
        // A slot of another kind takes nothing (the empty slot at the end takes any kind)
        if (index < resources.size() && !resources.get(index).equals(resource)) return 0;
        long room = Math.max(0, space - pendingIn());
        int inserted = (int) Math.min(amount, room);
        if (inserted <= 0) return 0;
        updateSnapshots(transaction);
        pending.merge(resource, (long) inserted, Long::sum);
        return inserted;
    }

    @Override
    public int extract(int index, T resource, int amount, TransactionContext transaction) {
        refresh();
        if (amount <= 0 || resource.isEmpty() || index >= resources.size() || !resources.get(index).equals(resource)) return 0;
        int extracted = (int) Math.min(amount, stored(resource));
        if (extracted <= 0) return 0;
        updateSnapshots(transaction);
        pending.merge(resource, (long) -extracted, Long::sum);
        return extracted;
    }

    // ---------------------------------------------------------------- transactions

    @Override
    protected Map<T, Long> createSnapshot() {
        return new HashMap<>(pending);
    }

    @Override
    protected void revertToSnapshot(Map<T, Long> snapshot) {
        pending = new HashMap<>(snapshot);
    }

    /// The transaction was kept: now the network really changes.
    @Override
    protected void onRootCommit(Map<T, Long> originalState) {
        Map<T, Long> changes = pending;
        pending = new HashMap<>();
        readTick = Long.MIN_VALUE;
        if (changes.isEmpty()) return;
        StarNetwork network = StarNetwork.at(level, pos);
        changes.forEach((resource, delta) -> {
            if (delta < 0) take(network, resource, -delta);
        });
        changes.forEach((resource, delta) -> {
            if (delta > 0) put(network, resource, delta);
        });
    }

    // ---------------------------------------------------------------- items

    public static class Items extends NetworkHandler<ItemResource> {
        public Items(Level level, BlockPos pos) {
            super(level, pos);
        }

        @Override
        protected void read(StarNetwork network, List<ItemResource> resources, Map<ItemResource, Long> amounts) {
            for (Map.Entry<net.minecraft.world.item.ItemStackTemplate, Long> entry : network.contents()) {
                ItemResource resource = ItemResource.of(entry.getKey());
                resources.add(resource);
                amounts.merge(resource, entry.getValue(), Long::sum);
            }
        }

        @Override
        protected long space(StarNetwork network) {
            return Math.max(0, network.capacity() - network.usedItems());
        }

        @Override
        protected boolean accepts(ItemResource resource) {
            ItemStack stack = resource.toStack(1);
            return !(stack.getItem() instanceof StarCellItem && !StarCellItem.isEmptyCell(stack))
                    && !(stack.getItem() instanceof FluidCellItem && !FluidCellItem.getContents(stack).isEmpty());
        }

        @Override
        protected void put(StarNetwork network, ItemResource resource, long amount) {
            while (amount > 0) {
                int part = (int) Math.min(amount, Integer.MAX_VALUE);
                ItemStack left = network.insert(resource.toStack(part));
                // (should not happen) no room any more: the items drop next to the Binary Star
                if (!left.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, left);
                amount -= part;
            }
        }

        @Override
        protected void take(StarNetwork network, ItemResource resource, long amount) {
            while (amount > 0) {
                int part = (int) Math.min(amount, Integer.MAX_VALUE);
                network.extract(resource.toStack(1), part);
                amount -= part;
            }
        }

        @Override
        protected ItemResource empty() {
            return ItemResource.EMPTY;
        }
    }

    // ---------------------------------------------------------------- liquids

    public static class Fluids extends NetworkHandler<FluidResource> {
        public Fluids(Level level, BlockPos pos) {
            super(level, pos);
        }

        @Override
        protected void read(StarNetwork network, List<FluidResource> resources, Map<FluidResource, Long> amounts) {
            for (FluidContents.Entry entry : network.fluids()) {
                FluidResource resource = FluidResource.of(entry.fluid());
                resources.add(resource);
                amounts.merge(resource, entry.amount(), Long::sum);
            }
        }

        @Override
        protected long space(StarNetwork network) {
            return Math.max(0, network.fluidCapacity() - network.fluidUsed());
        }

        /// The Fluid Cells keep only the liquid (not extra data of other mods).
        @Override
        protected boolean accepts(FluidResource resource) {
            return resource.isComponentsPatchEmpty();
        }

        @Override
        protected void put(StarNetwork network, FluidResource resource, long amount) {
            network.insertFluid(resource.getFluid(), amount);
        }

        @Override
        protected void take(StarNetwork network, FluidResource resource, long amount) {
            Fluid fluid = resource.getFluid();
            network.extractFluid(fluid, amount);
        }

        @Override
        protected FluidResource empty() {
            return FluidResource.EMPTY;
        }
    }
}
