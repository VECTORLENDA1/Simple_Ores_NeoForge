package com.exoticmoss.simpleores.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/// Ore veins inside the bedrock (Bedrock Obscuridium Ore).
/// Unlike the normal ores, the vein NEVER goes out of the layers MIN_LAYER to MAX_LAYER (counted from the
/// bottom of the world: 1 to 4 = y -63 to -60 in the Overworld), so the last layer of bedrock (y -64)
/// always stays whole and there is never a hole to the void.
/// The vein is a small "walk" of up to "size" blocks from the starting point, replacing only the blocks of the
/// targets (the bedrock).
public class BedrockOreFeature extends Feature<OreConfiguration> {
    /// Layers where the ore can be (0 = the last layer of the world, never used).
    public static final int MIN_LAYER = 1;
    public static final int MAX_LAYER = 4;
    /// How far (in blocks) the vein can go from its starting point, sideways.
    private static final int MAX_SPREAD = 3;

    public BedrockOreFeature() {
        super(OreConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        OreConfiguration config = context.config();
        BlockPos origin = context.origin();
        int minY = level.getMinY() + MIN_LAYER;
        int maxY = level.getMinY() + MAX_LAYER;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(origin.getX(), Mth.clamp(origin.getY(), minY, maxY), origin.getZ());
        int placed = 0;
        for (int step = 0; step < config.size * 4 && placed < config.size; step++) {
            for (OreConfiguration.TargetBlockState target : config.targetStates) {
                if (target.target.test(level.getBlockState(pos), random)) {
                    level.setBlock(pos, target.state, 2);
                    placed++;
                    break;
                }
            }
            // Next block of the vein: one step to a side, up or down (always inside the allowed layers)
            pos.set(Mth.clamp(pos.getX() + random.nextInt(3) - 1, origin.getX() - MAX_SPREAD, origin.getX() + MAX_SPREAD),
                    Mth.clamp(pos.getY() + random.nextInt(3) - 1, minY, maxY),
                    Mth.clamp(pos.getZ() + random.nextInt(3) - 1, origin.getZ() - MAX_SPREAD, origin.getZ() + MAX_SPREAD));
        }
        return placed > 0;
    }
}
