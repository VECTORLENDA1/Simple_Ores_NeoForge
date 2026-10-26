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

import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Sent by the server to the player using a Stellar Terminal: everything inside the Magnetar network,
/// how full each cell is, the items the Quasars of the network can craft, the liquids (Fluid Cells) and the
/// experience kept by the Magnetar.
public record TerminalContents(BlockPos pos, List<Item> items, long used, long capacity, List<Cell> cells,
                               List<ItemStackTemplate> craftables, List<FluidContents.Entry> fluids,
                               long fluidUsed, long fluidCapacity, long experience) implements CustomPacketPayload {
    /// One kind of item and how many of it the whole network has.
    /// On the screen, a liquid is shown as an Item too: its bucket, the mB, and the liquid ("fluid", else null).
    public record Item(ItemStackTemplate item, long count, @Nullable Fluid fluid) {
        public Item(ItemStackTemplate item, long count) {
            this(item, count, null);
        }
    }

    /// One cell of the network: its kind (CellTier ordinal), how many items (or mB) it has, and if it is a Fluid Cell.
    public record Cell(int tier, long used, boolean fluid) {
    }

    public static final Type<TerminalContents> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "terminal_contents"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalContents> STREAM_CODEC = StreamCodec.of(
            (buf, message) -> {
                BlockPos.STREAM_CODEC.encode(buf, message.pos());
                buf.writeVarInt(message.items().size());
                for (Item item : message.items()) {
                    ItemStackTemplate.STREAM_CODEC.encode(buf, item.item());
                    buf.writeVarLong(item.count());
                }
                buf.writeVarLong(message.used());
                buf.writeVarLong(message.capacity());
                buf.writeVarInt(message.cells().size());
                for (Cell cell : message.cells()) {
                    buf.writeVarInt(cell.tier());
                    buf.writeVarLong(cell.used());
                    buf.writeBoolean(cell.fluid());
                }
                buf.writeVarInt(message.craftables().size());
                for (ItemStackTemplate item : message.craftables()) ItemStackTemplate.STREAM_CODEC.encode(buf, item);
                buf.writeVarInt(message.fluids().size());
                for (FluidContents.Entry fluid : message.fluids()) FluidContents.Entry.STREAM_CODEC.encode(buf, fluid);
                buf.writeVarLong(message.fluidUsed());
                buf.writeVarLong(message.fluidCapacity());
                buf.writeVarLong(message.experience());
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                int size = buf.readVarInt();
                List<Item> items = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    items.add(new Item(ItemStackTemplate.STREAM_CODEC.decode(buf), buf.readVarLong()));
                }
                long used = buf.readVarLong();
                long capacity = buf.readVarLong();
                int cellCount = buf.readVarInt();
                List<Cell> cells = new ArrayList<>(cellCount);
                for (int i = 0; i < cellCount; i++) cells.add(new Cell(buf.readVarInt(), buf.readVarLong(), buf.readBoolean()));
                int craftableCount = buf.readVarInt();
                List<ItemStackTemplate> craftables = new ArrayList<>(craftableCount);
                for (int i = 0; i < craftableCount; i++) craftables.add(ItemStackTemplate.STREAM_CODEC.decode(buf));
                int fluidCount = buf.readVarInt();
                List<FluidContents.Entry> fluids = new ArrayList<>(fluidCount);
                for (int i = 0; i < fluidCount; i++) fluids.add(FluidContents.Entry.STREAM_CODEC.decode(buf));
                return new TerminalContents(pos, items, used, capacity, cells, craftables, fluids,
                        buf.readVarLong(), buf.readVarLong(), buf.readVarLong());
            });

    @Override
    public Type<TerminalContents> type() {
        return TYPE;
    }

    /// Nothing (before the server answers).
    public static TerminalContents empty(BlockPos pos) {
        return new TerminalContents(pos, List.of(), 0, 0, List.of(), List.of(), List.of(), 0, 0, 0);
    }

    /// Sends the network seen by the terminal at "pos" to the player.
    public static void send(ServerPlayer player, BlockPos pos) {
        StarNetwork network = StarNetwork.at(player.level(), pos);
        List<Item> items = new ArrayList<>();
        for (Map.Entry<ItemStackTemplate, Long> entry : network.contents()) {
            items.add(new Item(entry.getKey(), entry.getValue()));
        }
        List<ItemStackTemplate> craftables = new ArrayList<>();
        boolean advanced = AdvancedTerminalBlock.usesCores(player.level(), pos) || player.level().getBlockState(pos).getBlock() instanceof QuasarBlock;
        for (Pattern pattern : network.patterns()) {
            if ((advanced || !pattern.advanced()) && !craftables.contains(pattern.output())) craftables.add(pattern.output());
        }
        MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(player.level(), pos);
        PacketDistributor.sendToPlayer(player, new TerminalContents(pos, items, network.usedItems(), network.capacity(), network.cells(), craftables,
                network.fluids(), network.fluidUsed(), network.fluidCapacity(), magnetar == null ? 0 : magnetar.getExperience()));
    }

    /// Runs on the client: updates the terminal screen.
    public static void handle(TerminalContents message, IPayloadContext context) {
        TerminalClient.receive(message);
    }
}
