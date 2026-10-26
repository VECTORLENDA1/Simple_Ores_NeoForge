package com.exoticmoss.simpleores.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.exoticmoss.simpleores.SimpleOres.MODID;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);

    /// Ore veins inside the bedrock ("type": "simpleores:bedrock_ore" in the configured feature files)
    public static final DeferredHolder<Feature<?>, BedrockOreFeature> BEDROCK_ORE = FEATURES.register("bedrock_ore", BedrockOreFeature::new);

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
