package com.exoticmoss.simpleores.starstorage.network;

import com.exoticmoss.simpleores.starstorage.gas.Gases;
import com.exoticmoss.simpleores.util.ItemStackTemplate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/// A liquid or a gas of Mekanism shown as an item, so the recipes and the auto-craft can treat them like items:
/// a recipe can make a liquid or a gas, a plan can make what it misses of them, and the terminal can ask for them.
///  - a liquid: its bucket (an empty bucket when it has none) with the name of the liquid;
///  - a gas: a glass bottle with the name of the gas.
/// A mark in the item ("simpleores_fluid" / "simpleores_gas") tells them from the real buckets and bottles.
/// The amounts of these items are mB.
public final class Virtual {
    private static final String FLUID = "simpleores_fluid";
    private static final String GAS = "simpleores_gas";

    private Virtual() {
    }

    public static ItemStack fluid(Fluid fluid) {
        ItemStack stack = StarFluids.bucket(fluid);
        if (stack.isEmpty()) stack = new ItemStack(Items.BUCKET);
        CompoundTag mark = new CompoundTag();
        mark.putString(FLUID, BuiltInRegistries.FLUID.getKey(fluid).toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
        stack.set(DataComponents.CUSTOM_NAME, StarFluids.name(fluid).copy().withStyle(style -> style.withItalic(false)));
        return stack;
    }

    public static ItemStack gas(ResourceLocation gas) {
        ItemStack stack = new ItemStack(Items.GLASS_BOTTLE);
        CompoundTag mark = new CompoundTag();
        mark.putString(GAS, gas.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
        stack.set(DataComponents.CUSTOM_NAME, Gases.name(gas).copy().withStyle(style -> style.withItalic(false)));
        return stack;
    }

    public static ItemStackTemplate fluidKey(Fluid fluid) {
        return ItemStackTemplate.fromNonEmptyStack(fluid(fluid));
    }

    public static ItemStackTemplate gasKey(ResourceLocation gas) {
        return ItemStackTemplate.fromNonEmptyStack(gas(gas));
    }

    /// The liquid this item stands for (null = it is not one).
    public static @Nullable Fluid fluidOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(FLUID)) return null;
        ResourceLocation id = ResourceLocation.tryParse(data.copyTag().getString(FLUID));
        return id == null ? null : BuiltInRegistries.FLUID.getOptional(id).orElse(null);
    }

    /// The gas this item stands for (null = it is not one).
    public static @Nullable ResourceLocation gasOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(GAS)) return null;
        return ResourceLocation.tryParse(data.copyTag().getString(GAS));
    }

    public static boolean isVirtual(ItemStack stack) {
        return fluidOf(stack) != null || gasOf(stack) != null;
    }
}
