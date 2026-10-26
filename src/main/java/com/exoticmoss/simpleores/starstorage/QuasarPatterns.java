package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.client.QuasarClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/// Sent by the server: the recipes saved in a Quasar (for its screen).
public record QuasarPatterns(BlockPos pos, List<Pattern> patterns) implements CustomPacketPayload {
    public static final Type<QuasarPatterns> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "quasar_patterns"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuasarPatterns> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuasarPatterns::pos,
            Pattern.STREAM_CODEC.apply(ByteBufCodecs.list()), QuasarPatterns::patterns,
            QuasarPatterns::new);

    @Override
    public Type<QuasarPatterns> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, BlockPos pos) {
        if (player.level().getBlockEntity(pos) instanceof QuasarBlockEntity quasar) {
            PacketDistributor.sendToPlayer(player, new QuasarPatterns(pos, List.copyOf(quasar.getPatterns())));
        }
    }

    public static void handle(QuasarPatterns message, IPayloadContext context) {
        QuasarClient.receive(message);
    }
}
