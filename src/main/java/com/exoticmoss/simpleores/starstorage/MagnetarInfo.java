package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/// Sent by the server to the player with the Magnetar screen open (every second): how the network is.
public record MagnetarInfo(BlockPos pos, int drives, int cells, long used, long capacity, long fluidUsed, long fluidCapacity,
                           int radius) implements CustomPacketPayload {
    public static final Type<MagnetarInfo> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "magnetar_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagnetarInfo> STREAM_CODEC = StreamCodec.of(
            (buf, info) -> {
                BlockPos.STREAM_CODEC.encode(buf, info.pos());
                buf.writeVarInt(info.drives());
                buf.writeVarInt(info.cells());
                buf.writeVarLong(info.used());
                buf.writeVarLong(info.capacity());
                buf.writeVarLong(info.fluidUsed());
                buf.writeVarLong(info.fluidCapacity());
                buf.writeVarInt(info.radius());
            },
            buf -> new MagnetarInfo(BlockPos.STREAM_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(), buf.readVarLong(),
                    buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt()));

    /// Client: the last info received.
    private static MagnetarInfo last;

    @Override
    public Type<MagnetarInfo> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, MagnetarBlockEntity magnetar) {
        StarNetwork network = StarNetwork.around(magnetar.getLevel(), magnetar.getBlockPos());
        PacketDistributor.sendToPlayer(player, new MagnetarInfo(magnetar.getBlockPos(), network.drives().size(),
                network.cells().size(), network.usedItems(), network.capacity(), network.fluidUsed(), network.fluidCapacity(),
                magnetar.radius()));
    }

    public static void handle(MagnetarInfo message, IPayloadContext context) {
        last = message;
    }

    /// Client: the info of the Magnetar at "pos" (null if none came yet).
    public static MagnetarInfo last(BlockPos pos) {
        return last != null && last.pos().equals(pos) ? last : null;
    }
}
