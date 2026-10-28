package com.exoticmoss.simpleores.util.render;

import net.minecraft.core.BlockPos;

/// The data a block entity renderer of this mod reads before drawing (the block, its light).
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public int lightCoords;
}
