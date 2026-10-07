package com.vector.simpleores.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/// Inventory of the crafting tables, based on NeoForge's new transfer API
/// (replaces the old ItemStackHandler, which is marked for removal).
/// Keeps the methods the tables already used, so the crafting logic doesn't need to change.
public class CraftingTableItemHandler extends ItemStacksResourceHandler {
    public CraftingTableItemHandler(int size) {
        super(size);
    }

    public int getSlots() {
        return size();
    }

    /// Returns a copy: to change the contents use setStackInSlot/extractItem.
    public ItemStack getStackInSlot(int slot) {
        return getResource(slot).toStack(getAmountAsInt(slot));
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        set(slot, ItemResource.of(stack), stack.getCount());
    }

    public ItemStack extractItem(int slot, int amount) {
        ItemResource resource = getResource(slot);
        if (resource.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        // Every change in the new API goes through a transaction, which is only applied on commit()
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = extract(slot, resource, amount, tx);
            tx.commit();
            return resource.toStack(extracted);
        }
    }
}
