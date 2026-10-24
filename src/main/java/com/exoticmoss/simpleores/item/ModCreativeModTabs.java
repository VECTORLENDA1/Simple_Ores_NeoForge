package com.exoticmoss.simpleores.item;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import com.exoticmoss.simpleores.block.ModBlocks;

import static com.exoticmoss.simpleores.SimpleOres.MODID;


//Creates the mod's tab in the creative inventory, with all its items
public class ModCreativeModTabs {
    // Fabric places the mod tabs after the vanilla ones (NeoForge's withTabsBefore(COMBAT) does not exist here)
    public static final CreativeModeTab SIMPLE_ORES = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MODID, "simpleores"),
            FabricCreativeModeTab.builder()
            .title(Component.translatable("creativetab.simpleores"))
            .icon(() -> ModItems.CELESTINE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {

                    //ITEMS//
                    output.accept(ModItems.CELESTINE.get());
                    output.accept(ModItems.ZENITHRA.get());
                    output.accept(ModItems.ASTRALITE.get());
                    output.accept(ModItems.RAW_ASTRALITE.get());
                    output.accept(ModItems.RAW_NEXALITE.get());
                    output.accept(ModItems.NEXALITE.get());
                    output.accept(ModItems.IGNITHRA.get());
                    output.accept(ModItems.RAW_IGNITHRA.get());
                    output.accept(ModItems.ANTRACITE.get());
                    output.accept(ModItems.RAW_OBSCURIDIUM.get());
                    output.accept(ModItems.OBSCURIDIUM.get());
                    output.accept(ModItems.OBSCURITE.get());
                    output.accept(ModItems.WITHER_REAPER.get());


                    //BLOCKS//
                    output.accept(ModBlocks.END_STONE_CELESTINE_ORE.get());
                    output.accept(ModBlocks.NEXALITE_BLOCK.get());
                    output.accept(ModBlocks.IGNITHRA_BLOCK.get());
                    output.accept(ModBlocks.RAW_IGNITHRA_BLOCK.get());
                    output.accept(ModBlocks.DEEPSLATE_IGNITHRA_ORE.get());
                    output.accept(ModBlocks.IGNITHRA_ORE.get());
                    output.accept(ModBlocks.ASTRALITE_BLOCK.get());
                    output.accept(ModBlocks.DEEPSLATE_ASTRALITE_ORE.get());
                    output.accept(ModBlocks.END_STONE_ZENITHRA_ORE.get());
                    output.accept(ModBlocks.RAW_NEXALITE_BLOCK.get());
                    output.accept(ModBlocks.RAW_ASTRALITE_BLOCK.get());
                    output.accept(ModBlocks.ZENITHRA_BLOCK.get());
                    output.accept(ModBlocks.CELESTINE_BLOCK.get());
                    output.accept(ModBlocks.DEEPSLATE_NEXALITE_ORE.get());
                    output.accept(ModBlocks.NETHER_ANTRACITE_ORE.get());
                    output.accept(ModBlocks.ANTRACITE_BLOCK.get());
                    output.accept(ModBlocks.BEDROCK_OBSCURIDIUM_ORE.get());

                    //GRAVITY CORES//
                    output.accept(ModBlocks.GRAVITY_CORE_SUN.get());
                    output.accept(ModBlocks.GRAVITY_CORE_RED_GIANT.get());
                    output.accept(ModBlocks.GRAVITY_CORE_PULSAR.get());
                    output.accept(ModBlocks.GRAVITY_CORE_BLACK_HOLE.get());


            })
                .build());

    /// Loads this class, which registers the tab
    public static void register() {
    }
}
