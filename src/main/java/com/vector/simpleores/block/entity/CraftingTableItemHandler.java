package com.vector.simpleores.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/// Inventario das mesas de crafting, baseado na nova API de transferencia do NeoForge
/// (substitui o antigo ItemStackHandler, que foi marcado para remocao).
/// Mantem os metodos que as mesas ja usavam para que a logica de crafting nao precise de mudar.
public class CraftingTableItemHandler extends ItemStacksResourceHandler {
    public CraftingTableItemHandler(int size) {
        super(size);
    }

    public int getSlots() {
        return size();
    }

    /// Devolve uma copia: para alterar o conteudo usa setStackInSlot/extractItem.
    public ItemStack getStackInSlot(int slot) {
        return getResource(slot).toStack(getAmountAsInt(slot));
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        set(slot, ItemResource.of(stack), stack.getCount());
    }

    public ItemStack extractItem(int slot, int amount) {
        ItemResource resource = getResource(slot);
        if (resource.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        // Todas as alteracoes na nova API passam por uma transacao, que so e aplicada no commit()
        try (Transaction tx = Transaction.openRoot()) {
            int extracted = extract(slot, resource, amount, tx);
            tx.commit();
            return resource.toStack(extracted);
        }
    }
}
