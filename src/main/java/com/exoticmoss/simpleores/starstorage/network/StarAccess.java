package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Moves items and liquids between the Magnetar network and the blocks around (chests, furnaces, tanks, and the
/// machines of other mods), with the item and fluid handlers of NeoForge (IItemHandler, IFluidHandler): it works with
/// every block that lets pipes and hoppers of other mods put things in and take them out.
/// First it tries what the block accepts (simulated), then moves only that.
public final class StarAccess {
    private StarAccess() {
    }

    /// The slots of the block (null = it has none).
    public static @Nullable IItemHandler items(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    /// The tanks of the block (null = it has none).
    public static @Nullable IFluidHandler fluids(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
    }

    /// The item in a slot, as an ItemStack (a copy).
    public static ItemStack stack(IItemHandler handler, int slot) {
        return handler.getStackInSlot(slot).copy();
    }

    // ---------------------------------------------------------------- the sides

    /// The slots of the block as seen from each of its 6 sides (only the different ones). Some mods (like Mekanism)
    /// only let things in and out through the sides they are configured for: without a side, their slots can only
    /// be looked at. Then the items go in and out through these.
    public static List<IItemHandler> itemSides(Level level, BlockPos pos) {
        List<IItemHandler> list = new ArrayList<>();
        for (Direction side : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (handler != null && !list.contains(handler)) list.add(handler);
        }
        return list;
    }

    public static List<IFluidHandler> fluidSides(Level level, BlockPos pos) {
        List<IFluidHandler> list = new ArrayList<>();
        for (Direction side : Direction.values()) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            if (handler != null && !list.contains(handler)) list.add(handler);
        }
        return list;
    }

    // ---------------------------------------------------------------- moving items

    /// Puts the stack in any slot of the handler that takes it (the same item first). Returns what didn't go in.
    private static ItemStack insertAnySlot(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack left = stack;
        for (int slot = 0; slot < handler.getSlots() && !left.isEmpty(); slot++) {
            if (ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), left)) left = handler.insertItem(slot, left, simulate);
        }
        for (int slot = 0; slot < handler.getSlots() && !left.isEmpty(); slot++) {
            if (handler.getStackInSlot(slot).isEmpty()) left = handler.insertItem(slot, left, simulate);
        }
        return left;
    }

    /// Takes up to "amount" of the item out of any slot of the handler. Returns what came out.
    private static int extractAnySlot(IItemHandler handler, ItemStack item, int amount, boolean simulate) {
        int taken = 0;
        for (int slot = 0; slot < handler.getSlots() && taken < amount; slot++) {
            if (!ItemStack.isSameItemSameComponents(handler.getStackInSlot(slot), item)) continue;
            // (a slot gives at most a stack at a time)
            while (taken < amount) {
                ItemStack out = handler.extractItem(slot, Math.min(amount - taken, item.getMaxStackSize()), simulate);
                if (out.isEmpty()) break;
                taken += out.getCount();
                if (simulate) break;
            }
        }
        return taken;
    }

    /// Puts up to "amount" in the slot; if the slot takes nothing that way, through the sides (the block chooses the slot).
    /// Returns how many went in.
    private static int insert(IItemHandler handler, int slot, List<IItemHandler> sides, ItemStack item, int amount, boolean simulate) {
        ItemStack stack = item.copyWithCount(amount);
        int put = amount - handler.insertItem(slot, stack, simulate).getCount();
        if (put > 0) return put;
        for (IItemHandler side : sides) {
            if (put >= amount) break;
            put += (amount - put) - insertAnySlot(side, item.copyWithCount(amount - put), simulate).getCount();
        }
        return put;
    }

    /// Takes up to "amount" out of the slot; if nothing comes out that way, through the sides. Returns how many.
    private static int extract(IItemHandler handler, int slot, List<IItemHandler> sides, ItemStack item, int amount, boolean simulate) {
        int taken = handler.extractItem(slot, amount, simulate).getCount();
        if (taken > 0) return taken;
        for (IItemHandler side : sides) {
            if (taken >= amount) break;
            taken += extractAnySlot(side, item, amount - taken, simulate);
        }
        return taken;
    }

    /// Puts up to "amount" in the block: in any slot that takes it, then through the sides. Returns how many.
    private static int insertAny(IItemHandler handler, List<IItemHandler> sides, ItemStack item, int amount, boolean simulate) {
        int put = amount - insertAnySlot(handler, item.copyWithCount(amount), simulate).getCount();
        for (IItemHandler side : sides) {
            if (put >= amount) break;
            put += (amount - put) - insertAnySlot(side, item.copyWithCount(amount - put), simulate).getCount();
        }
        return put;
    }

    /// Takes up to "amount" out of the block: from any slot that gives it, then through the sides. Returns how many.
    private static int extractAny(IItemHandler handler, List<IItemHandler> sides, ItemStack item, int amount, boolean simulate) {
        int taken = extractAnySlot(handler, item, amount, simulate);
        for (IItemHandler side : sides) {
            if (taken >= amount) break;
            taken += extractAnySlot(side, item, amount - taken, simulate);
        }
        return taken;
    }

    // ---------------------------------------------------------------- machines (auto-craft)

    /// Puts all these items in the machine (the machine chooses the slots), or nothing if one of them doesn't fit.
    public static boolean insertAll(IItemHandler handler, List<IItemHandler> sides, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (insertAny(handler, sides, item, item.getCount(), true) < item.getCount()) return false;
        }
        for (ItemStack item : items) insertAny(handler, sides, item, item.getCount(), false);
        return true;
    }

    /// Takes all of the item out of the machine (from any slot that gives it, then through the sides). Returns how many.
    public static int extractAll(IItemHandler handler, List<IItemHandler> sides, ItemStack item) {
        return extractAny(handler, sides, item, Integer.MAX_VALUE, false);
    }

    /// Puts as many of the items as fit in the machine (the machine chooses the slots). Returns how many went in.
    public static int insertSome(IItemHandler handler, List<IItemHandler> sides, ItemStack item) {
        return insertAny(handler, sides, item, item.getCount(), false);
    }

    /// Takes up to "max" of the item out of the machine to the network (what doesn't fit stays). Returns how many went.
    public static int takeOut(StarNetwork network, IItemHandler handler, List<IItemHandler> sides, ItemStack item, int max) {
        int canTake = extractAny(handler, sides, item, max, true);
        if (canTake <= 0) return 0;
        ItemStack left = network.insert(item.copyWithCount(canTake));
        int moved = canTake - left.getCount();
        if (moved <= 0) return 0;
        int taken = extractAny(handler, sides, item, moved, false);
        if (taken < moved) network.extract(item.copyWithCount(1), moved - taken);
        return taken;
    }

    // ---------------------------------------------------------------- items

    /// The items of the slot go to the network (what doesn't fit stays).
    public static void takeItems(StarNetwork network, IItemHandler handler, List<IItemHandler> sides, int slot) {
        ItemStack inside = handler.getStackInSlot(slot);
        if (inside.isEmpty()) return;
        ItemStack item = inside.copyWithCount(1);
        int amount = inside.getCount();
        int canTake = extract(handler, slot, sides, item, amount, true);
        if (canTake <= 0) return;
        ItemStack left = network.insert(item.copyWithCount(canTake));
        int moved = canTake - left.getCount();
        if (moved <= 0) return;
        int taken = extract(handler, slot, sides, item, moved, false);
        // (should not happen) the block gave less than it said: the network gives the rest back
        if (taken < moved) network.extract(item, moved - taken);
    }

    /// The slot is filled with "item" from the network, up to a full stack (or what the slot takes).
    /// Only if the slot is empty or already has that item.
    public static void keepItems(StarNetwork network, IItemHandler handler, List<IItemHandler> sides, int slot, ItemStack item) {
        ItemStack current = handler.getStackInSlot(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, item)) return;
        int have = current.getCount();
        int max = Math.min(item.getMaxStackSize(), handler.getSlotLimit(slot));
        if (max - have <= 0) return;
        int accepted = insert(handler, slot, sides, item, max - have, true);
        if (accepted <= 0) return;
        ItemStack taken = network.extract(item.copyWithCount(1), accepted);
        if (taken.isEmpty()) return;
        int put = insert(handler, slot, sides, item, taken.getCount(), false);
        if (put < taken.getCount()) network.insert(taken.copyWithCount(taken.getCount() - put));
    }

    // ---------------------------------------------------------------- liquids

    /// Takes up to "amount" of the liquid out of the block (also through the sides). Returns how much.
    private static int drain(IFluidHandler handler, List<IFluidHandler> sides, Fluid fluid, int amount, IFluidHandler.FluidAction action) {
        int taken = handler.drain(new FluidStack(fluid, amount), action).getAmount();
        for (IFluidHandler side : sides) {
            if (taken >= amount) break;
            taken += side.drain(new FluidStack(fluid, amount - taken), action).getAmount();
        }
        return taken;
    }

    /// Puts up to "amount" of the liquid in the block (also through the sides). Returns how much.
    private static int fill(IFluidHandler handler, List<IFluidHandler> sides, Fluid fluid, int amount, IFluidHandler.FluidAction action) {
        int put = handler.fill(new FluidStack(fluid, amount), action);
        for (IFluidHandler side : sides) {
            if (put >= amount) break;
            put += side.fill(new FluidStack(fluid, amount - put), action);
        }
        return put;
    }

    /// The liquid of the tank goes to the Fluid Cells of the network (what doesn't fit stays).
    public static void takeFluid(StarNetwork network, IFluidHandler handler, List<IFluidHandler> sides, int tank) {
        FluidStack inside = handler.getFluidInTank(tank);
        if (inside.isEmpty()) return;
        Fluid fluid = inside.getFluid();
        int canTake = drain(handler, sides, fluid, inside.getAmount(), IFluidHandler.FluidAction.SIMULATE);
        if (canTake <= 0) return;
        long moved = network.insertFluid(fluid, canTake);
        if (moved <= 0) return;
        int taken = drain(handler, sides, fluid, (int) moved, IFluidHandler.FluidAction.EXECUTE);
        if (taken < moved) network.extractFluid(fluid, moved - taken);
    }

    /// The tank is filled with "fluid" from the network (as much as it takes).
    /// Only if the tank is empty or already has that liquid.
    public static void keepFluid(StarNetwork network, IFluidHandler handler, List<IFluidHandler> sides, int tank, Fluid fluid) {
        FluidStack current = handler.getFluidInTank(tank);
        if (!current.isEmpty() && current.getFluid() != fluid) return;
        long inNetwork = network.fluidAmount(fluid);
        if (inNetwork <= 0) return;
        int accepted = fill(handler, sides, fluid, (int) Math.min(Integer.MAX_VALUE, inNetwork), IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) return;
        long taken = network.extractFluid(fluid, accepted);
        if (taken <= 0) return;
        int put = fill(handler, sides, fluid, (int) taken, IFluidHandler.FluidAction.EXECUTE);
        if (put < taken) network.insertFluid(fluid, taken - put);
    }
}
