package com.vector.simpleores.gravitycraft;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vector.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/// The Gravity Core block. There is one block per tier, all using this same class.
/// The sphere is not a block model: it is drawn by GravityCoreRenderer.
public class GravityCoreBlock extends BaseEntityBlock {
    public static final MapCodec<GravityCoreBlock> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            CoreTier.CODEC.fieldOf("tier").forGetter(block -> block.tier),
            propertiesCodec()
    ).apply(inst, GravityCoreBlock::new));

    public final CoreTier tier;

    public GravityCoreBlock(CoreTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /// INVISIBLE: the block has no model, the renderer draws the sphere.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravityCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GRAVITY_CORE_BE.get(), (lvl, pos, st, core) -> core.tick());
    }

    /// Right-click with an empty hand: gives back the orbiting items.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GravityCoreBlockEntity core) {
            core.giveItemsBack(player);
        }
        return InteractionResult.SUCCESS;
    }
}
