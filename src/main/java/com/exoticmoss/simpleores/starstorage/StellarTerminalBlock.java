package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/// The Stellar Terminal: a small teal star. Right-click opens the screen with every item of the
/// Magnetar network and a crafting grid (see TerminalScreen). It must be inside the field of a Magnetar Core.
/// The star is not a block model: it is drawn by StarBlockRenderer.
public class StellarTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<StellarTerminalBlock> CODEC = simpleCodec(StellarTerminalBlock::new);

    public StellarTerminalBlock(Properties properties) {
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
        return new StellarTerminalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (StarNetwork.findMagnetar(level, pos) == null) {
                player.sendOverlayMessage(Component.translatable("message.simpleores.terminal.no_magnetar", StarNetwork.FIELD_RADIUS)
                        .withStyle(ChatFormatting.RED));
            } else {
                serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TerminalMenu(id, inventory, pos, TerminalLayout.MIN_ROWS),
                        Component.translatable("screen.simpleores.terminal.title")), pos);
                TerminalContents.send(serverPlayer, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
