package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import com.exoticmoss.simpleores.starstorage.cell.FluidContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    // ---------------------------------------------------------------- inputs and outputs

    /// What a slot (or tank) of a machine is for, found by trying (nothing is really moved):
    ///  - INPUT: it takes something (its own item, an item of the player, or of the network);
    ///  - OUTPUT: it has something that can be taken out, and takes nothing;
    ///  - UNKNOWN: it is empty and nothing tried could go in (no color on the screen).
    public static final int UNKNOWN = 0;
    public static final int INPUT = 1;
    public static final int OUTPUT = 2;

    /// The role of every slot (up to "count"). "probes" = the items tried (the player's and the network's).
    /// Each slot is tried directly; for the blocks that only take items through their sides (Mekanism...), each probe
    /// is also put in through every side, and the slot it went to is an input.
    public static List<Integer> itemRoles(ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides,
                                          int count, List<ItemResource> probes) {
        boolean[] input = new boolean[count];
        for (int slot = 0; slot < count; slot++) {
            ItemResource current = handler.getResource(slot);
            try (Transaction test = Transaction.openRoot()) {
                if (!current.isEmpty()) {
                    // A full slot can't take more: then only "can this item go here" is asked
                    input[slot] = handler.getAmountAsLong(slot) >= handler.getCapacityAsLong(slot, current)
                            ? handler.isValid(slot, current) : handler.insert(slot, current, 1, test) > 0;
                } else {
                    for (ItemResource probe : probes) {
                        if (handler.insert(slot, probe, 1, test) > 0) {
                            input[slot] = true;
                            break;
                        }
                    }
                }
            }
        }
        for (ResourceHandler<ItemResource> side : sides) {
            for (ItemResource probe : probes) {
                try (Transaction test = Transaction.openRoot()) {
                    long[] before = new long[count];
                    for (int slot = 0; slot < count; slot++) before[slot] = handler.getAmountAsLong(slot);
                    if (side.insert(probe, 1, test) <= 0) continue;
                    for (int slot = 0; slot < count; slot++) {
                        if (handler.getAmountAsLong(slot) != before[slot]) input[slot] = true;
                    }
                }
            }
        }
        List<Integer> roles = new ArrayList<>();
        for (int slot = 0; slot < count; slot++) {
            if (input[slot]) roles.add(INPUT);
            else if (handler.getResource(slot).isEmpty()) roles.add(UNKNOWN);
            else roles.add(canTake(handler, sides, slot) ? OUTPUT : UNKNOWN);
        }
        return roles;
    }

    /// True if something can be taken out of the slot (directly or through a side), only tried.
    private static <T extends Resource> boolean canTake(ResourceHandler<T> handler, List<ResourceHandler<T>> sides, int slot) {
        T resource = handler.getResource(slot);
        long before = handler.getAmountAsLong(slot);
        try (Transaction test = Transaction.openRoot()) {
            if (handler.extract(slot, resource, 1, test) > 0) return true;
        }
        for (ResourceHandler<T> side : sides) {
            try (Transaction test = Transaction.openRoot()) {
                if (side.extract(resource, 1, test) > 0 && handler.getAmountAsLong(slot) < before) return true;
            }
        }
        return false;
    }

    /// The same for tanks. "probes" = the liquids tried.
    public static List<Integer> fluidRoles(ResourceHandler<FluidResource> handler, List<ResourceHandler<FluidResource>> sides,
                                           int count, List<FluidResource> probes) {
        boolean[] input = new boolean[count];
        for (int tank = 0; tank < count; tank++) {
            FluidResource current = handler.getResource(tank);
            try (Transaction test = Transaction.openRoot()) {
                List<FluidResource> tries = current.isEmpty() ? probes : List.of(current);
                for (FluidResource probe : tries) {
                    if (handler.isValid(tank, probe) && (handler.insert(tank, probe, 1, test) > 0
                            || handler.getAmountAsLong(tank) >= handler.getCapacityAsLong(tank, probe))) {
                        input[tank] = true;
                        break;
                    }
                }
            }
        }
        for (ResourceHandler<FluidResource> side : sides) {
            for (FluidResource probe : probes) {
                try (Transaction test = Transaction.openRoot()) {
                    long[] before = new long[count];
                    for (int tank = 0; tank < count; tank++) before[tank] = handler.getAmountAsLong(tank);
                    if (side.insert(probe, 1, test) <= 0) continue;
                    for (int tank = 0; tank < count; tank++) {
                        if (handler.getAmountAsLong(tank) != before[tank]) input[tank] = true;
                    }
                }
            }
        }
        List<Integer> roles = new ArrayList<>();
        for (int tank = 0; tank < count; tank++) {
            if (input[tank]) roles.add(INPUT);
            else if (handler.getResource(tank).isEmpty()) roles.add(UNKNOWN);
            else roles.add(canTake(handler, sides, tank) ? OUTPUT : UNKNOWN);
        }
        return roles;
    }

    /// The items tried to find the inputs: the ones in the player's inventory and the most of the network.
    public static List<ItemResource> itemProbes(net.minecraft.world.entity.player.Player player, StarNetwork network) {
        List<ItemResource> probes = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                ItemResource resource = ItemResource.of(stack);
                if (!probes.contains(resource)) probes.add(resource);
            }
        }
        var contents = network.contents();
        for (int i = 0; i < Math.min(MAX_NETWORK_PROBES, contents.size()); i++) {
            ItemResource resource = ItemResource.of(contents.get(i).getKey());
            if (!probes.contains(resource)) probes.add(resource);
        }
        return probes;
    }

    /// The liquids tried: water, lava and the ones of the network.
    public static List<FluidResource> fluidProbes(StarNetwork network) {
        List<FluidResource> probes = new ArrayList<>();
        probes.add(FluidResource.of(Fluids.WATER));
        probes.add(FluidResource.of(Fluids.LAVA));
        for (FluidContents.Entry entry : network.fluids()) {
            FluidResource resource = FluidResource.of(entry.fluid());
            if (!probes.contains(resource)) probes.add(resource);
        }
        return probes;
    }

    /// At most this many kinds of items of the network are tried (the ones with the most items).
    private static final int MAX_NETWORK_PROBES = 32;

    // ---------------------------------------------------------------- the sides

    /// The slots of the block as seen from each of its 6 sides (only the different ones). Some mods (like Mekanism)
    /// only let things in and out through the sides they are configured for: without a side, their slots can only
    /// be looked at. Then the items go in and out through these.
    public static List<ResourceHandler<ItemResource>> itemSides(Level level, BlockPos pos) {
        List<ResourceHandler<ItemResource>> list = new ArrayList<>();
        for (Direction side : Direction.values()) {
            ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, side);
            if (handler != null && !list.contains(handler)) list.add(handler);
        }
        return list;
    }

    public static List<ResourceHandler<FluidResource>> fluidSides(Level level, BlockPos pos) {
        List<ResourceHandler<FluidResource>> list = new ArrayList<>();
        for (Direction side : Direction.values()) {
            ResourceHandler<FluidResource> handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
            if (handler != null && !list.contains(handler)) list.add(handler);
        }
        return list;
    }

    /// Puts up to "amount" in the slot; if the slot takes nothing that way, through the sides (the block chooses the slot).
    private static <T extends Resource> int insert(ResourceHandler<T> handler, int slot, List<ResourceHandler<T>> sides,
                                                   T resource, int amount, TransactionContext transaction) {
        int put = handler.insert(slot, resource, amount, transaction);
        if (put > 0) return put;
        for (ResourceHandler<T> side : sides) {
            if (put >= amount) break;
            put += side.insert(resource, amount - put, transaction);
        }
        return put;
    }

    /// Takes up to "amount" out of the slot; if nothing comes out that way, through the sides.
    private static <T extends Resource> int extract(ResourceHandler<T> handler, int slot, List<ResourceHandler<T>> sides,
                                                    T resource, int amount, TransactionContext transaction) {
        int taken = handler.extract(slot, resource, amount, transaction);
        if (taken > 0) return taken;
        for (ResourceHandler<T> side : sides) {
            if (taken >= amount) break;
            taken += side.extract(resource, amount - taken, transaction);
        }
        return taken;
    }

    // ---------------------------------------------------------------- items

    /// The items of the slot go to the network (what doesn't fit stays).
    public static void takeItems(StarNetwork network, ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides, int slot) {
        ItemResource resource = handler.getResource(slot);
        int amount = handler.getAmountAsInt(slot);
        if (resource.isEmpty() || amount <= 0) return;
        int canTake;
        try (Transaction test = Transaction.openRoot()) {
            canTake = extract(handler, slot, sides, resource, amount, test);
        }
        if (canTake <= 0) return;
        ItemStack left = network.insert(resource.toStack(canTake));
        int moved = canTake - left.getCount();
        if (moved <= 0) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = extract(handler, slot, sides, resource, moved, transaction);
            transaction.commit();
            // (should not happen) the block gave less than it said: the network gives the rest back
            if (taken < moved) network.extract(resource.toStack(1), moved - taken);
        }
    }

    /// The slot is filled with "item" from the network, up to a full stack (or what the slot takes).
    /// Only if the slot is empty or already has that item.
    public static void keepItems(StarNetwork network, ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides,
                                 int slot, ItemStack item) {
        ItemResource wanted = ItemResource.of(item);
        ItemResource current = handler.getResource(slot);
        if (!current.isEmpty() && !current.equals(wanted)) return;
        int have = current.isEmpty() ? 0 : handler.getAmountAsInt(slot);
        int max = Math.min(item.getMaxStackSize(), handler.getCapacityAsInt(slot, wanted));
        if (max - have <= 0) return;
        int accepted;
        try (Transaction test = Transaction.openRoot()) {
            accepted = insert(handler, slot, sides, wanted, max - have, test);
        }
        if (accepted <= 0) return;
        ItemStack taken = network.extract(item.copyWithCount(1), accepted);
        if (taken.isEmpty()) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int put = insert(handler, slot, sides, wanted, taken.getCount(), transaction);
            transaction.commit();
            if (put < taken.getCount()) network.insert(taken.copyWithCount(taken.getCount() - put));
        }
    }

    // ---------------------------------------------------------------- liquids

    /// The liquid of the tank goes to the Fluid Cells of the network (what doesn't fit stays).
    public static void takeFluid(StarNetwork network, ResourceHandler<FluidResource> handler, List<ResourceHandler<FluidResource>> sides, int tank) {
        FluidResource resource = handler.getResource(tank);
        int amount = handler.getAmountAsInt(tank);
        if (resource.isEmpty() || amount <= 0) return;
        int canTake;
        try (Transaction test = Transaction.openRoot()) {
            canTake = extract(handler, tank, sides, resource, amount, test);
        }
        if (canTake <= 0) return;
        Fluid fluid = resource.getFluid();
        long moved = network.insertFluid(fluid, canTake);
        if (moved <= 0) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = extract(handler, tank, sides, resource, (int) moved, transaction);
            transaction.commit();
            if (taken < moved) network.extractFluid(fluid, moved - taken);
        }
    }

    /// The tank is filled with "fluid" from the network (as much as it takes).
    /// Only if the tank is empty or already has that liquid.
    public static void keepFluid(StarNetwork network, ResourceHandler<FluidResource> handler, List<ResourceHandler<FluidResource>> sides,
                                 int tank, Fluid fluid) {
        FluidResource wanted = FluidResource.of(fluid);
        FluidResource current = handler.getResource(tank);
        if (!current.isEmpty() && current.getFluid() != fluid) return;
        long inNetwork = network.fluidAmount(fluid);
        if (inNetwork <= 0) return;
        int accepted;
        try (Transaction test = Transaction.openRoot()) {
            accepted = insert(handler, tank, sides, wanted, (int) Math.min(Integer.MAX_VALUE, inNetwork), test);
        }
        if (accepted <= 0) return;
        long taken = network.extractFluid(fluid, accepted);
        if (taken <= 0) return;
        try (Transaction transaction = Transaction.openRoot()) {
            int put = insert(handler, tank, sides, wanted, (int) taken, transaction);
            transaction.commit();
            if (put < taken) network.insertFluid(fluid, taken - put);
        }
    }
}
