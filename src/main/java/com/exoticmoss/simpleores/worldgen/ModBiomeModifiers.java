package com.exoticmoss.simpleores.worldgen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;

/// Adds the ores of the mod to the biomes (Fabric's BiomeModifications, instead of NeoForge's
/// biome modifier files). The ores themselves are in data/simpleores/worldgen/placed_feature.
public class ModBiomeModifiers {

    public static void bootstrap() {
        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.IGNITHRA_ORE_PLACED_KEY);

        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_END),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.END_STONE_CELESTINE_ORE_PLACED_KEY);

        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_END),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.END_STONE_ZENITHRA_ORE_PLACED_KEY);

        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.DEEPSLATE_ASTRALITE_ORE_PLACED_KEY);

        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.DEEPSLATE_IGNITHRA_ORE_PLACED_KEY);

        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.DEEPSLATE_NEXALITE_ORE_PLACED_KEY);

        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_NETHER),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.NETHER_ANTRACITE_ORE_PLACED_KEY);

        BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                GenerationStep.Decoration.UNDERGROUND_ORES, ModPlacedFeatures.BEDROCK_OBSCURIDIUM_ORE_PLACED_KEY);
    }
}
