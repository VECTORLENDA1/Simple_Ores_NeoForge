package com.vector.simpleores.block.custom;

import com.mojang.serialization.MapCodec;
import com.vector.simpleores.block.entity.ModBlockEntities;
import com.vector.simpleores.block.entity.custom.UltraCraftingTableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class UltraCraftingTable extends BaseEntityBlock {
    public static final VoxelShape SHAPE = Block.box(0.0f, 0.0f, 0.0f, 16.0f, 16.0f, 16.0f);
    public static final MapCodec<UltraCraftingTable> CODEC = simpleCodec(UltraCraftingTable::new);


    public UltraCraftingTable(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /// important if you don't have this the block will be invisible//
    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new UltraCraftingTableEntity(pPos, pState);
    }


    /// This will open the Menu of the block (Ultra Crafting Table)\\
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof UltraCraftingTableEntity ultraCraftingTableEntity) {
                ((ServerPlayer) player).openMenu(new SimpleMenuProvider(ultraCraftingTableEntity, Component.literal("Ultra Crafting Table")), pos);
            } else {
                throw new IllegalStateException("Our container provider is missing!");
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (pLevel.isClientSide()) {
            return null;
        }

        return createTickerHelper(pBlockEntityType, ModBlockEntities.ULTRA_CRAFTING_TABLE_BE.get(),
                (level, blockPos, blockState, ultraCraftingTableEntity)
                        -> ultraCraftingTableEntity.tick(level, blockPos, blockState));
    }
}

