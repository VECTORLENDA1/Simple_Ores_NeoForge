package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/// Sent by the client when the player changes the crafting grid of the Stellar Terminal.
/// The server keeps the grid (TerminalCrafting) and sends back what it makes.
public record TerminalGrid(BlockPos pos, List<ItemStack> grid) implements CustomPacketPayload {
    public static final Type<TerminalGrid> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "terminal_grid"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalGrid> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalGrid::pos,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(TerminalCrafting.SIZE)), TerminalGrid::grid,
            TerminalGrid::new);

    @Override
    public Type<TerminalGrid> type() {
        return TYPE;
    }

    public static void handle(TerminalGrid message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (player.position().distanceToSqr(Vec3.atCenterOf(message.pos())) > TerminalAction.RANGE * TerminalAction.RANGE) return;
        TerminalCrafting.setGrid(player, message.grid());
        TerminalContents.send(player, message.pos(), false);
    }
}
