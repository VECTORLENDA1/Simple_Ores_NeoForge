package com.exoticmoss.simpleores.starstorage.binary;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import org.jetbrains.annotations.Nullable;
import com.exoticmoss.simpleores.starstorage.render.StarLook;

/// The Binary Star: two small stars turning around each other. It moves the items in and out of the blocks
/// with an inventory around it (chests, furnaces, machines...), using the network of the Magnetar whose field
/// it is in. Right-click opens its screen, where each slot gets its rule (see BinaryStarBlockEntity).
/// The stars are not a block model: they are drawn by StarBlockRenderer (StarLook.BINARY).
public class BinaryStarBlock extends BaseEntityBlock {
    public static final MapCodec<BinaryStarBlock> CODEC = simpleCodec(BinaryStarBlock::new);

    public BinaryStarBlock(Properties properties) {
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

    /// Only the stars can be clicked (a box just around them).
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return StarLook.BINARY.shape();
    }

    /// When the block is broken, what it keeps drops on the ground.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BinaryStarBlockEntity blockEntity) {
            blockEntity.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BinaryStarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof BinaryStarBlockEntity) {
            open(serverPlayer, pos);
            com.exoticmoss.simpleores.starstorage.network.StarTabs.send(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /// Opens the screen of the Binary Star (also from a tab of another screen of the network).
    public static void open(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof BinaryStarBlockEntity binary)) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BinaryMenu(id, inventory, pos, 240, binary.getUpgrades()),
                Component.translatable("screen.simpleores.binary.title")), pos);
        BinaryView.send(player, pos, false);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BINARY_STAR_BE.get(), (lvl, pos, st, binary) -> binary.serverTick());
    }
}
