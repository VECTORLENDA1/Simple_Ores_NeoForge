package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/// The Stellar Drive: keeps up to MAX_CELLS Star Cells. Inside the field of a Magnetar, the items of its
/// cells can be seen and used from any Stellar Terminal of the same field.
///  - Right-click with a cell: puts the cell in the drive.
///  - Right-click with an empty hand: takes out the last cell put in.
/// The front shows how many cells are inside (the "cells" block state).
public class StellarDriveBlock extends BaseEntityBlock {
    public static final MapCodec<StellarDriveBlock> CODEC = simpleCodec(StellarDriveBlock::new);
    public static final int MAX_CELLS = 4;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty CELLS = IntegerProperty.create("cells", 0, MAX_CELLS);

    public StellarDriveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELLS, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELLS);
    }

    /// The front looks at the player who places it.
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StellarDriveBlockEntity(pos, state);
    }

    /// Right-click with a Star Cell: puts it in.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof StarCellItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StellarDriveBlockEntity drive) {
            if (drive.addCell(stack.copyWithCount(1))) {
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /// Right-click with an empty hand: takes out the last cell.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StellarDriveBlockEntity drive) {
            ItemStack cell = drive.removeLastCell();
            if (!cell.isEmpty()) {
                if (!player.getInventory().add(cell)) player.drop(cell, false);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
