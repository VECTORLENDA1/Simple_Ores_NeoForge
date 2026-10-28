package com.exoticmoss.simpleores.starstorage.terminal;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// The Stellar Terminal block entity. It keeps nothing: it only lets StarBlockRenderer draw the star.
public class StellarTerminalBlockEntity extends BlockEntity {
    public StellarTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STELLAR_TERMINAL_BE.get(), pos, state);
    }
}
