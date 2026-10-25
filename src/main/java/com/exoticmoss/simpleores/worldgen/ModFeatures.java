package com.exoticmoss.simpleores.worldgen;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ModFeatures {
    /// Ore veins inside the bedrock ("type": "simpleores:bedrock_ore" in the configured feature files)
    public static final BedrockOreFeature BEDROCK_ORE = Registry.register(BuiltInRegistries.FEATURE,
            Identifier.fromNamespaceAndPath(SimpleOres.MODID, "bedrock_ore"), new BedrockOreFeature());

    /// Loads this class, which registers the features
    public static void register() {
    }
}
