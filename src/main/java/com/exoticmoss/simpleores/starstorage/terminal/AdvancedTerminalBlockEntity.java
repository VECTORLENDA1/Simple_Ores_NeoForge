package com.exoticmoss.simpleores.starstorage.terminal;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// The Stellar Terminal level 2 block entity. It keeps nothing: it only lets StarBlockRenderer draw the star.
public class AdvancedTerminalBlockEntity extends BlockEntity {
    public AdvancedTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_TERMINAL_BE.get(), pos, state);
    }
}
