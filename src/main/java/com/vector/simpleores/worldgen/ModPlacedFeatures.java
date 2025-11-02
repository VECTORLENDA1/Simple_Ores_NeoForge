package com.vector.simpleores.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import com.vector.simpleores.SimpleOres;

import java.util.List;

public class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> IGNITHRA_ORE_PLACED_KEY = registerKey( "ignithra_ore_placed");
    public static final ResourceKey<PlacedFeature> NETHER_ANTRACITE_ORE_PLACED_KEY = registerKey( "nether_antracite_ore_placed");
    public static final ResourceKey<PlacedFeature> END_STONE_CELESTINE_ORE_PLACED_KEY = registerKey( "end_stone_celestine_ore_placed");
    public static final ResourceKey<PlacedFeature> END_STONE_ZENITHRA_ORE_PLACED_KEY = registerKey( "end_stone_zenithra_ore_placed");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_NEXALITE_ORE_PLACED_KEY = registerKey( "deepslate_nexalite_ore_placed");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_ASTRALITE_ORE_PLACED_KEY = registerKey( "deepslate_astralite_ore_placed");
    public static final ResourceKey<PlacedFeature> DEEPSLATE_IGNITHRA_ORE_PLACED_KEY = registerKey( "deepslate_ignithra_ore_placed");
    public static final ResourceKey<PlacedFeature> BEDROCK_OBSCURIDIUM_ORE_PLACED_KEY = registerKey( "bedrock_obscuridium_ore_placed");

    public static void bootstrap(BootstrapContext <PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, IGNITHRA_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_IGNITHRA_ORE_KEY),
                ModOrePlacement.commonOrePlacement(8,//how many veins per chunk
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(60))));

        register(context, END_STONE_CELESTINE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.END_STONE_CELESTINE_ORE_KEY),
                ModOrePlacement.commonOrePlacement(6,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(50))));

        register(context, NETHER_ANTRACITE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.NETHER_ANTRACITE_ORE_KEY),
                ModOrePlacement.commonOrePlacement(10,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(40), VerticalAnchor.absolute(126))));

        register(context, END_STONE_ZENITHRA_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.END_STONE_ZENITHRA_ORE_KEY),
                ModOrePlacement.commonOrePlacement(6,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(50))));

        register(context, DEEPSLATE_ASTRALITE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_DEEPSLATE_ASTRALITE_ORE_KEY),
                ModOrePlacement.commonOrePlacement(8,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-58), VerticalAnchor.absolute(-30))));

        register(context, DEEPSLATE_IGNITHRA_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_DEEPSLATE_IGNITHRA_ORE_KEY),
                ModOrePlacement.commonOrePlacement(8,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-30), VerticalAnchor.absolute(0))));

        register(context, DEEPSLATE_NEXALITE_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_DEEPSLATE_NEXALITE_ORE_KEY),
                ModOrePlacement.commonOrePlacement(8,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-58), VerticalAnchor.absolute(-30))));

        register(context, BEDROCK_OBSCURIDIUM_ORE_PLACED_KEY, configuredFeatures.getOrThrow(ModConfiguredFeatures.OVERWORLD_BEDROCK_OBSCURIDIUM_ORE_KEY),
                ModOrePlacement.commonOrePlacement(4,
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-63), VerticalAnchor.absolute(-59))));
    }


    private static ResourceKey<PlacedFeature> registerKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(SimpleOres.MODID, name));
    }

    private static void register(BootstrapContext<PlacedFeature> context, ResourceKey<PlacedFeature> key, Holder<ConfiguredFeature<?, ?>> configuration,
                                 List<PlacementModifier> modifiers) {
        context.register(key, new PlacedFeature(configuration, List.copyOf(modifiers)));
    }



}
