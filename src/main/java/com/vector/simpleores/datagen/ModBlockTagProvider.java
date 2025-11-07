package com.vector.simpleores.datagen;

import com.vector.simpleores.SimpleOres;
import com.vector.simpleores.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, SimpleOres.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.CELESTINE_BLOCK.get())
                .add(ModBlocks.ASTRALITE_BLOCK.get())
                .add(ModBlocks.DEEPSLATE_ASTRALITE_ORE.get())
                .add(ModBlocks.DEEPSLATE_NEXALITE_ORE.get())
                .add(ModBlocks.DEEPSLATE_IGNITHRA_ORE.get())
                .add(ModBlocks.END_STONE_CELESTINE_ORE.get())
                .add(ModBlocks.END_STONE_ZENITHRA_ORE.get())
                .add(ModBlocks.IGNITHRA_BLOCK.get())
                .add(ModBlocks.IGNITHRA_ORE.get())
                .add(ModBlocks.NEXALITE_BLOCK.get())
                .add(ModBlocks.ZENITHRA_BLOCK.get())
                .add(ModBlocks.RAW_ASTRALITE_BLOCK.get())
                .add(ModBlocks.RAW_NEXALITE_BLOCK.get())
                .add(ModBlocks.RAW_IGNITHRA_BLOCK.get())
                .add(ModBlocks.NETHER_ANTRACITE_ORE.get())
                .add(ModBlocks.ANTRACITE_BLOCK.get())
                .add(ModBlocks.OBSCURIDIUM_BLOCK.get())
                .add(ModBlocks.OBSCURITE_BLOCK.get())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.get())
                .add(ModBlocks.SIMPLE_CRAFTING_TABLE.get());


        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.RAW_IGNITHRA_BLOCK.get())
                .add(ModBlocks.IGNITHRA_BLOCK.get())
                .add(ModBlocks.IGNITHRA_ORE.get())
                .add(ModBlocks.DEEPSLATE_IGNITHRA_ORE.get());

        tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.DEEPSLATE_ASTRALITE_ORE.get())
                .add(ModBlocks.DEEPSLATE_NEXALITE_ORE.get())
                .add(ModBlocks.ASTRALITE_BLOCK.get())
                .add(ModBlocks.NEXALITE_BLOCK.get())
                .add(ModBlocks.RAW_ASTRALITE_BLOCK.get())
                .add(ModBlocks.RAW_NEXALITE_BLOCK.get())
                .add(ModBlocks.NETHER_ANTRACITE_ORE.get())
                .add(ModBlocks.ANTRACITE_BLOCK.get())
                .add(ModBlocks.SIMPLE_CRAFTING_TABLE.get());

        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(ModBlocks.CELESTINE_BLOCK.get())
                .add(ModBlocks.END_STONE_CELESTINE_ORE.get())
                .add(ModBlocks.END_STONE_ZENITHRA_ORE.get())
                .add(ModBlocks.ZENITHRA_BLOCK.get());



        //NEEDS NETHERITE TIER TOOLS TO BREAK//

        tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.get())
                .add(ModBlocks.OBSCURITE_BLOCK.get())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.get())
                .add(ModBlocks.OBSCURITE_BLOCK.get())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.get())
                .add(ModBlocks.OBSCURITE_BLOCK.get())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_IRON_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.get())
                .add(ModBlocks.OBSCURITE_BLOCK.get())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.get());

        tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
                .add(ModBlocks.OBSCURIDIUM_BLOCK.get())
                .add(ModBlocks.OBSCURITE_BLOCK.get())
                .add(ModBlocks.RAW_OBSCURIDIUM_BLOCK.get());

    }
}
