package com.exoticmoss.simpleores.starstorage;

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

/// The Quasar: the brain of the auto-craft. It keeps the recipes (see QuasarBlockEntity); right-click opens
/// its screen, where the recipes are made (by hand or with JEI) and saved.
/// The star is not a block model: it is drawn by StarBlockRenderer (StarLook.QUASAR).
public class QuasarBlock extends BaseEntityBlock {
    public static final MapCodec<QuasarBlock> CODEC = simpleCodec(QuasarBlock::new);

    public QuasarBlock(Properties properties) {
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return StarLook.QUASAR.shape();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuasarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof QuasarBlockEntity) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new QuasarMenu(id, inventory, pos),
                    Component.translatable("screen.simpleores.quasar.title")), pos);
            QuasarPatterns.send(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
