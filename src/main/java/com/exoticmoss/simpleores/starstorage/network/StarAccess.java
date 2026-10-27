package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/// Moves items and liquids between the Magnetar network and the blocks around (chests, furnaces, tanks, and the
/// machines of other mods), with the transfer API of NeoForge (ResourceHandler): it works with every block that
/// lets pipes and hoppers of other mods put things in and take them out.
/// First it tries what the block accepts (a transaction that is not committed), then moves only that.
public final class StarAccess {
    private StarAccess() {
    }

    /// The slots of the block (null = it has none).
    public static @Nullable ResourceHandler<ItemResource> items(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.Item.BLOCK, pos, null);
    }

    /// The tanks of the block (null = it has none).
    public static @Nullable ResourceHandler<FluidResource> fluids(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, null);
    }

    /// The item in a slot, as an ItemStack (a copy).
    public static ItemStack stack(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        int amount = handler.getAmountAsInt(slot);
        return resource.isEmpty() || amount <= 0 ? ItemStack.EMPTY : resource.toStack(amount);
    }

    // ---------------------------------------------------------------- items

    /// The items of the slot go to the network (what doesn't fit stays).
    public static void takeItems(StarNetwork network, ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        int amount = handler.getAmountAsInt(slot);
        if (resource.isEmpty() || amount <= 0) return;
        int canTake;
        try (Transaction test = Transaction.openRoot()) {
            canTake = handler.extract(slot, resource, amount, test);
        }
        if (canTake <= 0) return;
        ItemStack left = network.insert(resource.toStack(canTake));
        int moved = canTake - left.getCount();
        if (moved <= 0) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = handler.extract(slot, resource, moved, transaction);
            transaction.commit();
            // (should not happen) the block gave less than it said: the network gives the rest back
            if (taken < moved) network.extract(resource.toStack(1), moved - taken);
        }
    }

    /// The slot is filled with "item" from the network, up to a full stack (or what the slot takes).
    /// Only if the slot is empty or already has that item.
    public static void keepItems(StarNetwork network, ResourceHandler<ItemResource> handler, int slot, ItemStack item) {
        ItemResource wanted = ItemResource.of(item);
        ItemResource current = handler.getResource(slot);
        if (!current.isEmpty() && !current.equals(wanted)) return;
        int have = current.isEmpty() ? 0 : handler.getAmountAsInt(slot);
        int max = Math.min(item.getMaxStackSize(), handler.getCapacityAsInt(slot, wanted));
        if (max - have <= 0) return;
        int accepted;
        try (Transaction test = Transaction.openRoot()) {
            accepted = handler.insert(slot, wanted, max - have, test);
        }
        if (accepted <= 0) return;
        ItemStack taken = network.extract(item.copyWithCount(1), accepted);
        if (taken.isEmpty()) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int put = handler.insert(slot, wanted, taken.getCount(), transaction);
            transaction.commit();
            if (put < taken.getCount()) network.insert(taken.copyWithCount(taken.getCount() - put));
        }
    }

    // ---------------------------------------------------------------- liquids

    /// The liquid of the tank goes to the Fluid Cells of the network (what doesn't fit stays).
    public static void takeFluid(StarNetwork network, ResourceHandler<FluidResource> handler, int tank) {
        FluidResource resource = handler.getResource(tank);
        int amount = handler.getAmountAsInt(tank);
        if (resource.isEmpty() || amount <= 0) return;
        int canTake;
        try (Transaction test = Transaction.openRoot()) {
            canTake = handler.extract(tank, resource, amount, test);
        }
        if (canTake <= 0) return;
        Fluid fluid = resource.getFluid();
        long moved = network.insertFluid(fluid, canTake);
        if (moved <= 0) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = handler.extract(tank, resource, (int) moved, transaction);
            transaction.commit();
            if (taken < moved) network.extractFluid(fluid, moved - taken);
        }
    }

    /// The tank is filled with "fluid" from the network (as much as it takes).
    /// Only if the tank is empty or already has that liquid.
    public static void keepFluid(StarNetwork network, ResourceHandler<FluidResource> handler, int tank, Fluid fluid) {
        FluidResource wanted = FluidResource.of(fluid);
        FluidResource current = handler.getResource(tank);
        if (!current.isEmpty() && current.getFluid() != fluid) return;
        long inNetwork = network.fluidAmount(fluid);
        if (inNetwork <= 0) return;
        int accepted;
        try (Transaction test = Transaction.openRoot()) {
            accepted = handler.insert(tank, wanted, (int) Math.min(Integer.MAX_VALUE, inNetwork), test);
        }
        if (accepted <= 0) return;
        long taken = network.extractFluid(fluid, accepted);
        if (taken <= 0) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int put = handler.insert(tank, wanted, (int) taken, transaction);
            transaction.commit();
            if (put < taken) network.insertFluid(fluid, taken - put);
        }
    }
}
