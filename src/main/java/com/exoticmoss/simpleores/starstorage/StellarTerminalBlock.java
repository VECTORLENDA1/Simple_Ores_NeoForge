package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/// The Stellar Terminal: right-click opens the screen with every item of the Magnetar network
/// (see TerminalScreen). It must be inside the field of a Magnetar Core.
public class StellarTerminalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<StellarTerminalBlock> CODEC = simpleCodec(StellarTerminalBlock::new);

    public StellarTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /// The screen looks at the player who places it.
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (StarNetwork.findMagnetar(level, pos) == null) {
                player.sendOverlayMessage(Component.translatable("message.simpleores.terminal.no_magnetar", StarNetwork.FIELD_RADIUS)
                        .withStyle(ChatFormatting.RED));
            } else {
                TerminalContents.send(serverPlayer, pos, true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
