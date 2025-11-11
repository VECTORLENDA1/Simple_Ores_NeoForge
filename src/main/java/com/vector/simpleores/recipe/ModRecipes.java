package com.vector.simpleores.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "simpleores");
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, "simpleores");


    public static final Supplier<RecipeSerializer<SimpleCraftingTableRecipe>> SIMPLE_CRAFTING_TABLE_SERIALIZER =
            SERIALIZERS.register("simple_crafting_table", SimpleCraftingTableRecipe.Serializer::new);
    public static final Supplier<RecipeType<SimpleCraftingTableRecipe>> SIMPLE_CRAFTING_TABLE_TYPE =
            TYPES.register("simple_crafting_table", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "simple_crafting_table";
                }
            });

    public static final Supplier<RecipeSerializer<UltraCraftingTableRecipe>> ULTRA_CRAFTING_TABLE_SERIALIZER =
            SERIALIZERS.register("ultra_crafting_table", UltraCraftingTableRecipe.Serializer::new);
    public static final Supplier<RecipeType<UltraCraftingTableRecipe>> ULTRA_CRAFTING_TABLE_TYPE =
            TYPES.register("ultra_crafting_table", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "ultra_crafting_table";
                }
            });




    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
