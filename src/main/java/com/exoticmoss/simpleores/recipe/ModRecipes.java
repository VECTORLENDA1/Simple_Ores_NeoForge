package com.exoticmoss.simpleores.recipe;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.function.Supplier;


public class ModRecipes {
    private static final RecipeSerializer<GravityCoreRecipe> GRAVITY_CRAFT_SERIALIZER_VALUE = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath("simpleores", "gravity_craft"),
            new RecipeSerializer<>(GravityCoreRecipe.CODEC, GravityCoreRecipe.STREAM_CODEC));
    private static final RecipeType<GravityCoreRecipe> GRAVITY_CRAFT_TYPE_VALUE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE, Identifier.fromNamespaceAndPath("simpleores", "gravity_craft"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return "gravity_craft";
                }
            });

    // Gravity Core recipes ("type": "simpleores:gravity_craft")
    public static final Supplier<RecipeSerializer<GravityCoreRecipe>> GRAVITY_CRAFT_SERIALIZER = () -> GRAVITY_CRAFT_SERIALIZER_VALUE;
    public static final Supplier<RecipeType<GravityCoreRecipe>> GRAVITY_CRAFT_TYPE = () -> GRAVITY_CRAFT_TYPE_VALUE;

    /// Loads this class, which registers the recipe serializer and type
    public static void register() {
    }
}
