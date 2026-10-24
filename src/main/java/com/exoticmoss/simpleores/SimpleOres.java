package com.exoticmoss.simpleores;

import com.mojang.logging.LogUtils;
import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.exoticmoss.simpleores.gravitycraft.MoveItemsToCore;
import com.exoticmoss.simpleores.gravitycraft.SelectCoreRecipe;
import com.exoticmoss.simpleores.item.custom.WitherReaperEvents;
import com.exoticmoss.simpleores.recipe.ModRecipes;
import com.exoticmoss.simpleores.worldgen.ModBiomeModifiers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.item.ModCreativeModTabs;
import com.exoticmoss.simpleores.item.ModItems;
import org.slf4j.Logger;

public class SimpleOres implements ModInitializer {
    public static final String MODID = "simpleores";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ModItems.Register();
        ModBlocks.Register();
        ModRecipes.register();
        ModBlockEntities.register();



        ModCreativeModTabs.register();

        // Ores in the biomes (replaces the NeoForge biome modifier files)
        ModBiomeModifiers.bootstrap();
        // Wither Reaper: extra Wither Skeleton skulls
        WitherReaperEvents.register();

        // The client no longer receives recipes automatically: Fabric sends our Gravity Core recipes
        // to the players (JEI uses them)
        RecipeSynchronization.synchronizeRecipeSerializer(ModRecipes.GRAVITY_CRAFT_SERIALIZER.get());

        registerPayloads();
    }

    // Network messages sent between client and server
    private void registerPayloads() {
        // JEI "Move Items" button for Gravity Core recipes (client -> server)
        PayloadTypeRegistry.serverboundPlay().register(MoveItemsToCore.TYPE, MoveItemsToCore.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MoveItemsToCore.TYPE, MoveItemsToCore::handle);
        // Selection bar above the Gravity Core: the player picked a recipe (client -> server)
        PayloadTypeRegistry.serverboundPlay().register(SelectCoreRecipe.TYPE, SelectCoreRecipe.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SelectCoreRecipe.TYPE, SelectCoreRecipe::handle);
    }
}
