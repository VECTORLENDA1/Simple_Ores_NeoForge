package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// The Stellar Terminal level 2: the same as the Stellar Terminal, but it can also craft the Gravity Core
/// recipes of the Quasars (level 1 only crafts the crafting table recipes). A golden star.
public class AdvancedTerminalBlock extends StellarTerminalBlock {
    public static final MapCodec<AdvancedTerminalBlock> CODEC = simpleCodec(AdvancedTerminalBlock::new);

    public AdvancedTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedTerminalBlockEntity(pos, state);
    }

    /// True if the terminal at "pos" can craft the Gravity Core recipes.
    public static boolean usesCores(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof AdvancedTerminalBlock;
    }
}
