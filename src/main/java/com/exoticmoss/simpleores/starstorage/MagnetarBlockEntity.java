package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/// The Magnetar Core block entity. It keeps nothing (the items are in the cells):
/// it only marks where the Magnetars are, and lets MagnetarRenderer draw the star.
public class MagnetarBlockEntity extends BlockEntity {
    public MagnetarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGNETAR_BE.get(), pos, state);
    }
}
