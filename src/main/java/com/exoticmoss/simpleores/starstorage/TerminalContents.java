package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.client.TerminalClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Sent by the server to the player using a Stellar Terminal: everything inside the Magnetar network.
/// "open" = open the terminal screen (false = only update the screen that is already open).
public record TerminalContents(BlockPos pos, boolean open, List<Item> items, long used, long capacity, int cells)
        implements CustomPacketPayload {
    /// One kind of item and how many of it the whole network has.
    public record Item(ItemStackTemplate item, long count) {
    }

    public static final Type<TerminalContents> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "terminal_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalContents> STREAM_CODEC = StreamCodec.of(
            (buf, message) -> {
                BlockPos.STREAM_CODEC.encode(buf, message.pos());
                buf.writeBoolean(message.open());
                buf.writeVarInt(message.items().size());
                for (Item item : message.items()) {
                    ItemStackTemplate.STREAM_CODEC.encode(buf, item.item());
                    buf.writeVarLong(item.count());
                }
                buf.writeVarLong(message.used());
                buf.writeVarLong(message.capacity());
                buf.writeVarInt(message.cells());
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                boolean open = buf.readBoolean();
                int size = buf.readVarInt();
                List<Item> items = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    items.add(new Item(ItemStackTemplate.STREAM_CODEC.decode(buf), buf.readVarLong()));
                }
                return new TerminalContents(pos, open, items, buf.readVarLong(), buf.readVarLong(), buf.readVarInt());
            });

    @Override
    public Type<TerminalContents> type() {
        return TYPE;
    }

    /// Sends the network seen by the terminal at "pos" to the player.
    public static void send(ServerPlayer player, BlockPos pos, boolean open) {
        StarNetwork network = StarNetwork.at(player.level(), pos);
        List<Item> items = new ArrayList<>();
        for (Map.Entry<ItemStackTemplate, Long> entry : network.contents()) {
            items.add(new Item(entry.getKey(), entry.getValue()));
        }
        PacketDistributor.sendToPlayer(player, new TerminalContents(pos, open, items,
                network.usedItems(), network.capacity(), network.cellCount()));
    }

    /// Runs on the client: opens or updates the terminal screen.
    public static void handle(TerminalContents message, IPayloadContext context) {
        TerminalClient.receive(message);
    }
}
