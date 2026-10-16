package com.exoticmoss.simpleores.compat.jer;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.item.ModItems;
import jeresources.api.IJERAPI;
import jeresources.api.IJERPlugin;
import jeresources.api.IWorldGenRegistry;
import jeresources.api.JERPlugin;
import jeresources.api.conditionals.Conditional;
import jeresources.api.distributions.DistributionSquare;
import jeresources.api.drop.LootDrop;
import jeresources.api.restrictions.Restriction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/// Just Enough Resources support (optional, only used when JER is installed): the ores of
/// Simple Ores show up in the "World Gen" page of JER, with the heights where they spawn and their drops.
///
/// The numbers are the same as the world generation of the mod (data/simpleores/worldgen/placed_feature):
/// DistributionSquare(veins per chunk, vein size, min height, max height).
/// If the world generation is changed, change the numbers here too.
@JERPlugin
public class SimpleOresJERPlugin implements IJERPlugin {

    @Override
    public void receive(IJERAPI api) {
        IWorldGenRegistry worldGen = api.getWorldGenRegistry();

        // Ignithra: stone and deepslate, two features (0 to 90 and -30 to 0)
        worldGen.register(stack(ModBlocks.IGNITHRA_ORE), stack(ModBlocks.DEEPSLATE_IGNITHRA_ORE),
                new DistributionSquare(8, 8, 0, 90), Restriction.OVERWORLD, true, oreDrop(ModItems.RAW_IGNITHRA));
        worldGen.register(stack(ModBlocks.IGNITHRA_ORE), stack(ModBlocks.DEEPSLATE_IGNITHRA_ORE),
                new DistributionSquare(7, 8, -30, 0), Restriction.OVERWORLD, true, oreDrop(ModItems.RAW_IGNITHRA));

        // Astralite and Nexalite: only deepslate
        worldGen.register(stack(ModBlocks.DEEPSLATE_ASTRALITE_ORE),
                new DistributionSquare(4, 4, -60, -20), Restriction.OVERWORLD, true, oreDrop(ModItems.RAW_ASTRALITE));
        worldGen.register(stack(ModBlocks.DEEPSLATE_NEXALITE_ORE),
                new DistributionSquare(4, 4, -60, -20), Restriction.OVERWORLD, true, oreDrop(ModItems.RAW_NEXALITE));

        // Obscuridium: near the bedrock, drops itself 33% of the times
        worldGen.register(stack(ModBlocks.BEDROCK_OBSCURIDIUM_ORE),
                new DistributionSquare(3, 3, -63, -59), Restriction.OVERWORLD, false,
                new LootDrop(stack(ModBlocks.BEDROCK_OBSCURIDIUM_ORE), 0.2F));

        // Antracite: Nether
        worldGen.register(stack(ModBlocks.NETHER_ANTRACITE_ORE),
                new DistributionSquare(10, 8, 0, 126), Restriction.NETHER, true, oreDrop(ModItems.ANTRACITE));

        // Zenithra and Celestine: End
        worldGen.register(stack(ModBlocks.END_STONE_ZENITHRA_ORE),
                new DistributionSquare(4, 4, 10, 50), Restriction.END, true, oreDrop(ModItems.ZENITHRA));
        worldGen.register(stack(ModBlocks.END_STONE_CELESTINE_ORE),
                new DistributionSquare(4, 4, 10, 50), Restriction.END, true, oreDrop(ModItems.CELESTINE));
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    /// 1 item, more with Fortune (like the vanilla ores)
    private static LootDrop oreDrop(ItemLike item) {
        return new LootDrop(stack(item), 1, 4, Conditional.affectedByFortune);
    }
}
