package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/// The Magnetar Core: the "brain" of the star storage.
/// Every Stellar Drive and Stellar Terminal inside its magnetic field (StarNetwork.FIELD_RADIUS blocks
/// in every direction) is connected to it, without cables. It needs no energy: stars live for billions of years.
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagnetarBlockEntity(pos, state);
    }

    /// Right-click: tells how the network is (drives, cells, items).
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            StarNetwork network = StarNetwork.around(level, pos);
            player.sendOverlayMessage(Component.translatable("message.simpleores.magnetar.info",
                    network.drives().size(), network.cellCount(),
                    StarNetwork.format(network.usedItems()), StarNetwork.format(network.capacity()),
                    StarNetwork.FIELD_RADIUS).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }
}
