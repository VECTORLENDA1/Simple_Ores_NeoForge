package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.client.TerminalClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Sent by the client: craft "amount" of "item" with the Quasars of the network of the terminal at "pos".
/// The server answers with Reply (what is missing, or that it was crafted).
public record CraftRequest(BlockPos pos, ItemStackTemplate item, int amount) implements CustomPacketPayload {
    public static final Type<CraftRequest> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "craft_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequest> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CraftRequest::pos,
            ItemStackTemplate.STREAM_CODEC, CraftRequest::item,
            ByteBufCodecs.VAR_INT, CraftRequest::amount,
            CraftRequest::new);

    @Override
    public Type<CraftRequest> type() {
        return TYPE;
    }

    public static void handle(CraftRequest message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (!(player.containerMenu instanceof TerminalMenu menu) || !menu.pos.equals(message.pos()) || !menu.stillValid(player)) return;
        if (message.amount() <= 0) return;
        StarNetwork network = StarNetwork.at(level, message.pos());
        Autocraft.Result result = Autocraft.request(level, network, message.item(), message.amount());
        List<Missing> missing = new ArrayList<>();
        for (Map.Entry<ItemStackTemplate, Long> entry : result.missing().entrySet()) {
            missing.add(new Missing(entry.getKey().create(), entry.getValue()));
        }
        PacketDistributor.sendToPlayer(player, new Reply(result.ok(), result.error() == null ? "" : result.error(), missing));
        TerminalContents.send(player, message.pos());
    }

    /// One missing item and how many.
    public record Missing(ItemStack item, long count) {
        static final StreamCodec<RegistryFriendlyByteBuf, Missing> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, Missing::item,
                ByteBufCodecs.VAR_LONG, Missing::count,
                Missing::new);
    }

    /// The answer of the server: crafted (ok), or an error message key, or what is missing.
    public record Reply(boolean ok, String error, List<Missing> missing) implements CustomPacketPayload {
        public static final Type<Reply> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "craft_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Reply> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Reply::ok,
                ByteBufCodecs.STRING_UTF8, Reply::error,
                Missing.STREAM_CODEC.apply(ByteBufCodecs.list()), Reply::missing,
                Reply::new);

        @Override
        public Type<Reply> type() {
            return TYPE;
        }

        public static void handle(Reply message, IPayloadContext context) {
            TerminalClient.receiveReply(message);
        }
    }
}
