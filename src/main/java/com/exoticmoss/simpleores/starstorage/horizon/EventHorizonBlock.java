package com.exoticmoss.simpleores.starstorage.horizon;

import com.exoticmoss.simpleores.starstorage.render.StarLook;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/// The Event Horizon (see EventHorizonBlockEntity): a black star with a disk of light around it. It joins the
/// inventories and tanks around it to the network. Right-click opens its screen (priority and filter).
/// The star is not a block model: it is drawn by StarBlockRenderer (StarLook.HORIZON).
public class EventHorizonBlock extends BaseEntityBlock {
    public static final MapCodec<EventHorizonBlock> CODEC = simpleCodec(EventHorizonBlock::new);

    public EventHorizonBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /// Only the star can be clicked (a box just around it).
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return StarLook.HORIZON.shape();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EventHorizonBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof EventHorizonBlockEntity horizon) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EventHorizonMenu(id, inventory, pos, horizon.filter()),
                    Component.translatable("block.simpleores.event_horizon")), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
