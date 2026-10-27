package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// The Stellar Terminal II: a Stellar Terminal (a golden star) that lets the whole network (every terminal and
/// Quasar in the field of the Magnetar, and in the linked fields) craft the Gravity Core recipes of the Quasars.
/// Without one, only the crafting table and furnace recipes are crafted.
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
}
