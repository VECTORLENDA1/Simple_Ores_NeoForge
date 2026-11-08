package com.exoticmoss.simpleores.starstorage.network;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// The tanks of a block, with FluidStacks: fill the block, drain the block, look at a tank (each one is done at
/// once, or only tried with SIMULATE). Used by the machines of the auto-craft and by the tanks joined by an Event
/// Horizon.
public record FluidTanks(ResourceHandler<FluidResource> handler) {
    public enum FluidAction {
        EXECUTE, SIMULATE;

        public boolean simulate() {
            return this == SIMULATE;
        }
    }

    public static @Nullable FluidTanks of(@Nullable ResourceHandler<FluidResource> handler) {
        return handler == null ? null : new FluidTanks(handler);
    }

    public static List<FluidTanks> all(List<ResourceHandler<FluidResource>> handlers) {
        List<FluidTanks> list = new ArrayList<>();
        for (ResourceHandler<FluidResource> handler : handlers) list.add(new FluidTanks(handler));
        return list;
    }

    public int getTanks() {
        return handler.size();
    }

    public FluidStack getFluidInTank(int tank) {
        FluidResource resource = handler.getResource(tank);
        int amount = handler.getAmountAsInt(tank);
        return resource.isEmpty() || amount <= 0 ? FluidStack.EMPTY : new FluidStack(resource.getFluid(), amount);
    }

    public int getTankCapacity(int tank) {
        FluidResource resource = handler.getResource(tank);
        return (int) Math.min(Integer.MAX_VALUE, handler.getCapacityAsLong(tank, resource));
    }

    /// Puts the liquid in the block. Returns how many mB (would) go in.
    public int fill(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int put = handler.insert(FluidResource.of(stack.getFluid()), stack.getAmount(), transaction);
            if (!action.simulate()) transaction.commit();
            return Math.max(0, put);
        }
    }

    /// Takes the liquid out of the block. Returns what (would) come out.
    public FluidStack drain(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        FluidResource resource = FluidResource.of(stack.getFluid());
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = handler.extract(resource, stack.getAmount(), transaction);
            if (!action.simulate()) transaction.commit();
            return taken <= 0 ? FluidStack.EMPTY : new FluidStack(stack.getFluid(), taken);
        }
    }
}
