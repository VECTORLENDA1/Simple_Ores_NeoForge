package com.vector.simpleores.datagen;

import com.vector.simpleores.SimpleOres;
import com.vector.simpleores.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    // O ExistingFileHelper foi removido do NeoForge
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, SimpleOres.MODID);
    }

    // tag(...).add() agora recebe a ResourceKey do bloco (DeferredBlock#getKey) em vez do Block
    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.CELESTINE_BLOCK.getKey())
                .add(ModBlocks.ASTRALITE_BLOCK.getKey())
                .add(ModBlocks.DEEPSLATE_ASTRALITE_ORE.getKey())
                .add(ModBlocks.DEEPSLATE_NEXALITE_ORE.getKey())
                .add(ModBlocks.DEEPSLATE_IGNITHRA_ORE.getKey())
                .add(ModBlocks.END_STONE_CELESTINE_ORE.getKey())
                .add(ModBlocks.END_STONE_ZENITHRA_ORE.getKey())
                .add(ModBlocks.IGNITHRA_BLOCK.getKey())
                .add(ModBlocks.IGNITHRA_ORE.getKey())
                .add(ModBlocks.NEXALITE_BLOCK.getKey())
                .add(ModBlocks.ZENITHRA_BLOCK.getKey())
                .add(ModBlocks.RAW_ASTRALITE_BLOCK.getKey())
                .add(ModBlocks.RAW_NEXALITE_BLOCK.getKey())
                .add(ModBlocks.RAW_IGNITHRA_BLOCK.getKey())
                .add(ModBlocks.NETHER_ANTRACITE_ORE.getKey())
                .add(ModBlocks.ANTRACITE_BLOCK.getKey())
                .add(ModBlocks.OBSCURIDIUM_BLOCK.getKey())
                .add(ModBlocks.OBSCURITE_BLOCK.getKey())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.getKey())
                .add(ModBlocks.SIMPLE_CRAFTING_TABLE.getKey())
                .add(ModBlocks.ULTRA_CRAFTING_TABLE.getKey())
                .add(ModBlocks.ATOMIC_CRAFTING_TABLE.getKey())
                .add(ModBlocks.GRAVITY_CORE_SUN.getKey())
                .add(ModBlocks.GRAVITY_CORE_RED_GIANT.getKey())
                .add(ModBlocks.GRAVITY_CORE_PULSAR.getKey())
                .add(ModBlocks.GRAVITY_CORE_BLACK_HOLE.getKey());


        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.RAW_IGNITHRA_BLOCK.getKey())
                .add(ModBlocks.IGNITHRA_BLOCK.getKey())
                .add(ModBlocks.IGNITHRA_ORE.getKey())
                .add(ModBlocks.DEEPSLATE_IGNITHRA_ORE.getKey());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.DEEPSLATE_ASTRALITE_ORE.getKey())
                .add(ModBlocks.DEEPSLATE_NEXALITE_ORE.getKey())
                .add(ModBlocks.ASTRALITE_BLOCK.getKey())
                .add(ModBlocks.NEXALITE_BLOCK.getKey())
                .add(ModBlocks.RAW_ASTRALITE_BLOCK.getKey())
                .add(ModBlocks.RAW_NEXALITE_BLOCK.getKey())
                .add(ModBlocks.NETHER_ANTRACITE_ORE.getKey())
                .add(ModBlocks.ANTRACITE_BLOCK.getKey())
                .add(ModBlocks.SIMPLE_CRAFTING_TABLE.getKey());

        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.CELESTINE_BLOCK.getKey())
                .add(ModBlocks.END_STONE_CELESTINE_ORE.getKey())
                .add(ModBlocks.END_STONE_ZENITHRA_ORE.getKey())
                .add(ModBlocks.ZENITHRA_BLOCK.getKey());



        //NEEDS NETHERITE TIER TOOLS TO BREAK//

        tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.getKey())
                .add(ModBlocks.OBSCURITE_BLOCK.getKey())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.getKey());

        tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.getKey())
                .add(ModBlocks.OBSCURITE_BLOCK.getKey())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.getKey());

        tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.getKey())
                .add(ModBlocks.OBSCURITE_BLOCK.getKey())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.getKey());

        tag(BlockTags.INCORRECT_FOR_IRON_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.getKey())
                .add(ModBlocks.OBSCURITE_BLOCK.getKey())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.getKey());

        tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.getKey())
                .add(ModBlocks.OBSCURITE_BLOCK.getKey())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.getKey());

    }
}
