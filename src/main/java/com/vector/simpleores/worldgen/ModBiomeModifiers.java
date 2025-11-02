package com.vector.simpleores.worldgen;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.vector.simpleores.SimpleOres;

public class ModBiomeModifiers {

    public static final ResourceKey<BiomeModifier> ADD_IGNITHRA_ORE = registerKey("add_ignithra_ore");
    public static final ResourceKey<BiomeModifier> ADD_END_STONE_CELESTINE_ORE = registerKey("add_end_stone_celestine_ore");
    public static final ResourceKey<BiomeModifier> ADD_END_STONE_ZENITHRA_ORE = registerKey("add_end_stone_zenithra_ore");
    public static final ResourceKey<BiomeModifier> ADD_NETHER_ANTRACITE_ORE = registerKey("add_nether_antracite_ore");
    public static final ResourceKey<BiomeModifier> ADD_DEEPSLATE_NEXALITE_ORE = registerKey("add_deepslate_nexalite_ore");
    public static final ResourceKey<BiomeModifier> ADD_DEEPSLATE_ASTRALITE_ORE = registerKey("add_deepslate-astralite_ore");
    public static final ResourceKey<BiomeModifier> ADD_DEEPSLATE_IGNITHRA_ORE = registerKey("add_deepslate_ignithra_ore");
    public static final ResourceKey<BiomeModifier> ADD_BEDROCK_OBSCURIDIUM_ORE = registerKey("add_bedrock_obscuridium_ore");


    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        var placeFeatures = context.lookup(Registries.PLACED_FEATURE);
        var biomes = context.lookup(Registries.BIOME);

        context.register(ADD_IGNITHRA_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.IGNITHRA_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_END_STONE_CELESTINE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_END),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.END_STONE_CELESTINE_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_END_STONE_ZENITHRA_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_END),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.END_STONE_ZENITHRA_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_DEEPSLATE_ASTRALITE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.DEEPSLATE_ASTRALITE_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_DEEPSLATE_IGNITHRA_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.DEEPSLATE_IGNITHRA_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_DEEPSLATE_NEXALITE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.DEEPSLATE_NEXALITE_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_NETHER_ANTRACITE_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.NETHER_ANTRACITE_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        context.register(ADD_BEDROCK_OBSCURIDIUM_ORE, new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                HolderSet.direct(placeFeatures.getOrThrow(ModPlacedFeatures.BEDROCK_OBSCURIDIUM_ORE_PLACED_KEY)),
                GenerationStep.Decoration.UNDERGROUND_ORES));


    }

    public static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(SimpleOres.MODID, name));
    }
}
