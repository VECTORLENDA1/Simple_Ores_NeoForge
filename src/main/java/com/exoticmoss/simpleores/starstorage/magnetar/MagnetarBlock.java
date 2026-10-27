package com.exoticmoss.simpleores.starstorage.magnetar;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import com.exoticmoss.simpleores.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.exoticmoss.simpleores.starstorage.ModDataComponents;
import com.exoticmoss.simpleores.starstorage.render.StarLook;

/// The Magnetar Core: the "brain" of the star storage.
/// Every Stellar Drive and Stellar Terminal inside its magnetic field (StarNetwork.FIELD_RADIUS blocks
/// in every direction) is connected to it, without cables. It needs no energy: stars live for billions of years.
/// Right-click opens its screen, with its upgrades (see MagnetarBlockEntity).
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

    /// Server: the Magnetar keeps the chunks of the Wormhole Links loaded.
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.MAGNETAR_BE.get(), (lvl, pos, st, magnetar) -> magnetar.serverTick());
    }

    /// Right-click with a Wormhole Link: the link points to this Magnetar (put it in another Magnetar to join them).
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.WORMHOLE_LINK.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            stack.set(ModDataComponents.LINK.get(), GlobalPos.of(level.dimension(), pos.immutable()));
            player.sendOverlayMessage(Component.translatable("message.simpleores.magnetar.linked",
                    pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, pos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 1.0F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    /// Right-click: opens the screen of the upgrades and of how the network is (drives, cells, items, liquids).
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MagnetarBlockEntity magnetar) {
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MagnetarMenu(id, inventory, pos, magnetar.getUpgrades()),
                    Component.translatable("block.simpleores.magnetar_core")), pos);
            MagnetarInfo.send(serverPlayer, magnetar);
        }
        return InteractionResult.SUCCESS;
    }
}
