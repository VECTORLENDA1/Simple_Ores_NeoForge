package com.exoticmoss.simpleores.starstorage.network;

import com.exoticmoss.simpleores.starstorage.gas.Gases;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

/// A liquid or a gas of Mekanism shown as an item, so the recipes and the auto-craft can treat them like items:
/// a recipe can make a liquid or a gas, a plan can make what it misses of them, and the terminal can ask for them.
///  - a liquid: its bucket (an empty bucket when it has none) with the name of the liquid;
///  - a gas: the item "gas" (drawn with the texture and the color of the gas) with the name of the gas.
/// A mark in the item ("simpleores_fluid" / "simpleores_gas") tells them from the real buckets.
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

    public static ItemStack gas(Identifier gas) {
        ItemStack stack = new ItemStack(com.exoticmoss.simpleores.item.ModItems.GAS.get());
        CompoundTag mark = new CompoundTag();
        mark.putString(GAS, gas.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
        stack.set(DataComponents.CUSTOM_NAME, Gases.name(gas).copy().withStyle(style -> style.withItalic(false)));
        return stack;
    }

    public static ItemStackTemplate fluidKey(Fluid fluid) {
        return ItemStackTemplate.fromNonEmptyStack(fluid(fluid));
    }

    public static ItemStackTemplate gasKey(Identifier gas) {
        return ItemStackTemplate.fromNonEmptyStack(gas(gas));
    }

    /// The liquid this item stands for (null = it is not one).
    public static @Nullable Fluid fluidOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(FLUID)) return null;
        Identifier id = Identifier.tryParse(data.copyTag().getStringOr(FLUID, ""));
        return id == null ? null : BuiltInRegistries.FLUID.getOptional(id).orElse(null);
    }

    /// The gas this item stands for (null = it is not one).
    public static @Nullable Identifier gasOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(GAS)) return null;
        return Identifier.tryParse(data.copyTag().getStringOr(GAS, ""));
    }

    /// The same liquid or gas with a line that says how many mB (for the pictures of the recipes).
    public static ItemStack withAmount(ItemStack stack, long amount) {
        ItemStack shown = plain(stack);
        shown.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(String.format(Locale.ROOT, "%,d mB", amount))
                .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.AQUA)))));
        return shown;
    }

    /// The liquid or gas of the item, without anything more (one item); other items: one of them.
    public static ItemStack plain(ItemStack stack) {
        Fluid fluid = fluidOf(stack);
        if (fluid != null) return fluid(fluid);
        Identifier gas = gasOf(stack);
        if (gas != null) return gas(gas);
        return stack.copyWithCount(1);
    }

    public static boolean isVirtual(ItemStack stack) {
        return fluidOf(stack) != null || gasOf(stack) != null;
    }
}
