package com.vector.simpleores.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import com.vector.simpleores.block.entity.CraftingTableItemHandler;

public record AtomicCraftingTableRecipeInput(CraftingTableItemHandler input) implements RecipeInput {
    @Override
    public ItemStack getItem(int pIndex) {
        return input.getStackInSlot(pIndex);
    }

    @Override
    public int size() {
        return 81;
    }
}