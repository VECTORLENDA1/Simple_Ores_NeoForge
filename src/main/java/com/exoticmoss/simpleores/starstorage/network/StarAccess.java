package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
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
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (handler != null) return handler;
        // Only through the sides: the side that shows the most slots
        for (ResourceHandler<ItemResource> side : itemSides(level, pos)) {
            if (handler == null || side.size() > handler.size()) handler = side;
        }
        return handler;
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

    /// Puts up to "amount" in the block: in any slot that takes it, then through the sides.
    private static <T extends Resource> int insertAny(ResourceHandler<T> handler, List<ResourceHandler<T>> sides,
                                                      T resource, int amount, TransactionContext transaction) {
        int put = handler.insert(resource, amount, transaction);
        for (ResourceHandler<T> side : sides) {
            if (put >= amount) break;
            put += side.insert(resource, amount - put, transaction);
        }
        return put;
    }

    /// Takes up to "amount" out of the block: from any slot that gives it, then through the sides.
    private static <T extends Resource> int extractAny(ResourceHandler<T> handler, List<ResourceHandler<T>> sides,
                                                       T resource, int amount, TransactionContext transaction) {
        int taken = handler.extract(resource, amount, transaction);
        for (ResourceHandler<T> side : sides) {
            if (taken >= amount) break;
            taken += side.extract(resource, amount - taken, transaction);
        }
        return taken;
    }

    // ---------------------------------------------------------------- machines (auto-craft)

    /// Puts all these items in the machine (the machine chooses the slots), or nothing if one of them doesn't fit.
    public static boolean insertAll(ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides, List<ItemStack> items) {
        try (Transaction transaction = Transaction.openRoot()) {
            for (ItemStack item : items) {
                if (insertAny(handler, sides, ItemResource.of(item), item.getCount(), transaction) < item.getCount()) return false;
            }
            transaction.commit();
            return true;
        }
    }

    /// Takes all of the item out of the machine (from any slot that gives it, then through the sides). Returns how many.
    public static int extractAll(ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides, ItemStack item) {
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = extractAny(handler, sides, ItemResource.of(item), Integer.MAX_VALUE, transaction);
            transaction.commit();
            return taken;
        }
    }

    /// Takes out of the machine every stack of the same item as "item" but with other data (like a tool made with
    /// its energy). Returns what came out.
    public static List<ItemStack> extractVariants(ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides, ItemStack item) {
        List<ItemStack> out = new ArrayList<>();
        List<ResourceHandler<ItemResource>> all = new ArrayList<>();
        all.add(handler);
        all.addAll(sides);
        for (ResourceHandler<ItemResource> from : all) {
            for (int slot = 0; slot < from.size(); slot++) {
                ItemStack inside = stack(from, slot);
                if (inside.isEmpty() || !inside.is(item.getItem()) || ItemStack.isSameItemSameComponents(inside, item)) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int got = from.extract(slot, from.getResource(slot), inside.getCount(), transaction);
                    transaction.commit();
                    if (got > 0) out.add(inside.copyWithCount(got));
                }
            }
        }
        return out;
    }

    /// Puts as many of the items as fit in the machine (the machine chooses the slots). Returns how many went in.
    public static int insertSome(ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides, ItemStack item) {
        try (Transaction transaction = Transaction.openRoot()) {
            int put = insertAny(handler, sides, ItemResource.of(item), item.getCount(), transaction);
            transaction.commit();
            return put;
        }
    }

    /// Puts as many of the items as fit in the machine, spread over up to "maxSlots" of its slots (the slots that
    /// already have the item count too), and always leaving a free slot for each of the "others" (the other
    /// ingredients of the recipe) that is not in the machine yet. For machines that need each ingredient in its own
    /// slots, like the 8 slots of the Dissolution Chamber of Industrial Foregoing: an ingredient that the recipe uses
    /// twice goes in two slots, and one ingredient never fills the slots the others need. Through the sides when it
    /// takes nothing without one. Returns how many went in.
    public static int insertLimited(ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides, ItemStack item,
                                    int maxSlots, List<ItemStack> others) {
        List<ResourceHandler<ItemResource>> all = new ArrayList<>();
        all.add(handler);
        all.addAll(sides);
        int amount = item.getCount(), put = 0;
        for (ResourceHandler<ItemResource> to : all) {
            if (put >= amount) break;
            put += insertInSlots(to, ItemResource.of(item), amount - put, maxSlots, others);
        }
        return put;
    }

    private static int insertInSlots(ResourceHandler<ItemResource> handler, ItemResource resource, int amount, int maxSlots, List<ItemStack> others) {
        // The slots that have it, and the free slots that take it
        List<Integer> holding = new ArrayList<>(), free = new ArrayList<>();
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack inside = stack(handler, slot);
            if (inside.isEmpty()) {
                try (Transaction test = Transaction.openRoot()) {
                    if (handler.insert(slot, resource, 1, test) > 0) free.add(slot);
                }
            } else if (handler.getResource(slot).equals(resource)) {
                holding.add(slot);
            }
        }
        // A free slot is kept for each other ingredient that is not in the machine yet
        int missing = 0;
        for (ItemStack other : others) {
            boolean there = false;
            for (int slot = 0; slot < handler.size() && !there; slot++) {
                there = ItemStack.isSameItemSameComponents(stack(handler, slot), other);
            }
            if (!there) missing++;
        }
        int slots = Math.max(1, Math.min(maxSlots, holding.size() + free.size() - missing));
        List<Integer> targets = new ArrayList<>(holding);
        for (int i = 0; i < free.size() && targets.size() < slots; i++) targets.add(free.get(i));
        if (targets.isEmpty()) return 0;
        // Spread evenly over those slots
        int left = amount;
        boolean moved = true;
        while (left > 0 && moved) {
            moved = false;
            int part = Math.max(1, left / targets.size());
            for (int slot : targets) {
                if (left <= 0) break;
                try (Transaction transaction = Transaction.openRoot()) {
                    int in = handler.insert(slot, resource, Math.min(part, left), transaction);
                    transaction.commit();
                    if (in > 0) moved = true;
                    left -= in;
                }
            }
        }
        return amount - left;
    }

    /// Takes up to "max" of the item out of the machine to the network (what doesn't fit stays). Returns how many went.
    public static int takeOut(StarNetwork network, ResourceHandler<ItemResource> handler, List<ResourceHandler<ItemResource>> sides,
                              ItemStack item, int max) {
        ItemResource resource = ItemResource.of(item);
        int canTake;
        try (Transaction test = Transaction.openRoot()) {
            canTake = extractAny(handler, sides, resource, max, test);
        }
        if (canTake <= 0) return 0;
        ItemStack left = network.insert(resource.toStack(canTake));
        int moved = canTake - left.getCount();
        if (moved <= 0) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = extractAny(handler, sides, resource, moved, transaction);
            transaction.commit();
            if (taken < moved) network.extract(resource.toStack(1), moved - taken);
            return taken;
        }
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
