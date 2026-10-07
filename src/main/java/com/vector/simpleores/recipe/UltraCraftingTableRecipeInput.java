package com.vector.simpleores.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import com.vector.simpleores.block.entity.CraftingTableItemHandler;

public record UltraCraftingTableRecipeInput(CraftingTableItemHandler input) implements RecipeInput {
    @Override
    public ItemStack getItem(int pIndex) {
        return input.getStackInSlot(pIndex);
    }

    @Override
    public int size() {
        return 49;
    }
}