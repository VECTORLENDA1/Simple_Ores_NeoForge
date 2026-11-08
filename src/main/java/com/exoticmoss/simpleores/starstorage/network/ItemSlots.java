package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/// The slots of a block one by one, with ItemStacks: put in a slot, take out of a slot, look at a slot (each one is
/// done at once, or only tried with "simulate"). Used by the altars and the pedestals of the auto-craft and by the
/// inventories joined by an Event Horizon.
public record ItemSlots(ResourceHandler<ItemResource> handler) {
    public static @Nullable ItemSlots of(@Nullable ResourceHandler<ItemResource> handler) {
        return handler == null ? null : new ItemSlots(handler);
    }

    public int getSlots() {
        return handler.size();
    }

    public ItemStack getStackInSlot(int slot) {
        return StarAccess.stack(handler, slot);
    }

    /// How many items the slot can have.
    public int getSlotLimit(int slot) {
        ItemResource resource = handler.getResource(slot);
        return (int) Math.min(Integer.MAX_VALUE, handler.getCapacityAsLong(slot, resource.isEmpty() ? ItemResource.EMPTY : resource));
    }

    /// Puts the stack in the slot. Returns what didn't go in.
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int put = handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            if (!simulate) transaction.commit();
            return put >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - Math.max(0, put));
        }
    }

    /// Takes up to "amount" items out of the slot. Returns what came out.
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = handler.extract(slot, resource, amount, transaction);
            if (!simulate) transaction.commit();
            return taken <= 0 ? ItemStack.EMPTY : resource.toStack(taken);
        }
    }
}
