package com.vector.simpleores.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.ItemStackHandler;

public record SimpleCraftingTableRecipeInput(ItemStackHandler input) implements RecipeInput {
    @Override
    public ItemStack getItem(int pIndex) {
        return input.getStackInSlot(pIndex);
    }

    @Override
    public int size() {
        return 25;
    }
}