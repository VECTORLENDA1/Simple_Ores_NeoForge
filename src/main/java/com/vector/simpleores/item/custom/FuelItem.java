package com.vector.simpleores.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;
import org.jetbrains.annotations.Nullable;

public class FuelItem extends Item {
    private int burntime = 0;

    public FuelItem(Properties pProperties, int burntime) {
        super(pProperties);
        this.burntime = burntime;
    }

    // getBurnTime agora recebe tambem os FuelValues
    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType, FuelValues fuelValues) {
        return this.burntime;
    }

}

