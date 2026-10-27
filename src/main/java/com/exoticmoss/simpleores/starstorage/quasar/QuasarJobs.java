package com.exoticmoss.simpleores.starstorage.quasar;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.client.QuasarClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import com.exoticmoss.simpleores.starstorage.autocraft.Autocraft;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;

/// Sent by the server to the player with a Quasar screen open: the auto-crafts being made in the network.
public record QuasarJobs(BlockPos pos, List<View> jobs) implements CustomPacketPayload {
    /// One auto-craft: what it makes, how many, how many are made (all its steps counted), and what it does (a message key).
    public record View(int id, ItemStack output, long amount, long made, String status) {
        static final StreamCodec<RegistryFriendlyByteBuf, View> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, View::id,
                ItemStack.OPTIONAL_STREAM_CODEC, View::output,
                ByteBufCodecs.VAR_LONG, View::amount,
                ByteBufCodecs.VAR_LONG, View::made,
                ByteBufCodecs.STRING_UTF8, View::status,
                View::new);
    }

    public static final Type<QuasarJobs> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "quasar_jobs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuasarJobs> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuasarJobs::pos,
            View.STREAM_CODEC.apply(ByteBufCodecs.list()), QuasarJobs::jobs,
            QuasarJobs::new);

    @Override
    public Type<QuasarJobs> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, BlockPos pos) {
        List<View> views = new ArrayList<>();
        for (QuasarBlockEntity quasar : StarNetwork.at(player.level(), pos).quasars()) {
            for (Autocraft.Job job : quasar.getJobs()) {
                views.add(new View(job.id, job.output.create(), job.amount, job.made(), job.status()));
            }
        }
        PacketDistributor.sendToPlayer(player, new QuasarJobs(pos, views));
    }

    public static void handle(QuasarJobs message, IPayloadContext context) {
        QuasarClient.receiveJobs(message);
    }
}
