package com.exoticmoss.simpleores.recipe;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
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

    // Gravity Core recipes ("type": "simpleores:gravity_craft")
    public static final Supplier<RecipeSerializer<GravityCoreRecipe>> GRAVITY_CRAFT_SERIALIZER =
            SERIALIZERS.register("gravity_craft", () -> new RecipeSerializer<GravityCoreRecipe>() {
                @Override
                public com.mojang.serialization.MapCodec<GravityCoreRecipe> codec() {
                    return GravityCoreRecipe.CODEC;
                }

                @Override
                public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, GravityCoreRecipe> streamCodec() {
                    return GravityCoreRecipe.STREAM_CODEC;
                }
            });
    public static final Supplier<RecipeType<GravityCoreRecipe>> GRAVITY_CRAFT_TYPE =
            TYPES.register("gravity_craft", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "gravity_craft";
                }
            });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
