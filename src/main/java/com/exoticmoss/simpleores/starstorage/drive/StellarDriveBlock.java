package com.exoticmoss.simpleores.starstorage.drive;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.exoticmoss.simpleores.starstorage.cell.FluidCellItem;
import com.exoticmoss.simpleores.starstorage.cell.StarCellItem;

/// The Stellar Drive: keeps up to MAX_CELLS Star Cells. Inside the field of a Magnetar, the items of its
/// cells can be seen and used from any Stellar Terminal of the same field.
///  - Right-click with a cell: puts the cell in the drive.
///  - Right-click with an empty hand: takes out the last cell put in.
/// The block is see-through: the cells inside are drawn as small stars turning slowly (StellarDriveRenderer).
public class StellarDriveBlock extends BaseEntityBlock {
    public static final MapCodec<StellarDriveBlock> CODEC = simpleCodec(StellarDriveBlock::new);
    public static final int MAX_CELLS = 4;
    public static final IntegerProperty CELLS = IntegerProperty.create("cells", 0, MAX_CELLS);

    public StellarDriveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CELLS, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CELLS);
    }


    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /// Like glass: no wall between two drives side by side.
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    /// When the block is broken, what it keeps drops on the ground.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof StellarDriveBlockEntity blockEntity) {
            blockEntity.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StellarDriveBlockEntity(pos, state);
    }

    /// Right-click with a Star Cell or a Fluid Cell: puts it in.
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof StarCellItem) && !(stack.getItem() instanceof FluidCellItem)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StellarDriveBlockEntity drive) {
            if (drive.addCell(stack.copyWithCount(1))) {
                stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
            }
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
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
