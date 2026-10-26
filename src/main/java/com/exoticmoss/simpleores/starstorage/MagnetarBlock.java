package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.MapCodec;
import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/// The Magnetar Core: the "brain" of the star storage.
/// Every Stellar Drive and Stellar Terminal inside its magnetic field (StarNetwork.FIELD_RADIUS blocks
/// in every direction) is connected to it, without cables. It needs no energy: stars live for billions of years.
/// Right-click opens its screen: there the player chooses, slot by slot, what the Magnetar takes from the blocks
/// with an inventory of its field and what it keeps in them (see MagnetarBlockEntity).
/// The star is not a block model: it is drawn by StarBlockRenderer (StarLook.MAGNETAR).
public class MagnetarBlock extends BaseEntityBlock {
    public static final MapCodec<MagnetarBlock> CODEC = simpleCodec(MagnetarBlock::new);

    public MagnetarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /// INVISIBLE: the block has no model, the renderer draws the star.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /// Only the star can be clicked (a box just around it).
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return StarLook.MAGNETAR.shape();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagnetarBlockEntity(pos, state);
    }

    /// Right-click: opens the Magnetar screen (the network and the blocks with an inventory inside the field).
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) MagnetarView.send(serverPlayer, pos, true);
        return InteractionResult.SUCCESS;
    }

    /// The Magnetar moves the items of the blocks of its field (see MagnetarBlockEntity).
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.MAGNETAR_BE.get(), (lvl, pos, st, magnetar) -> magnetar.serverTick());
    }
}
