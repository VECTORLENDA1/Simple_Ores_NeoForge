package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.client.BinaryClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/// Sent by the server to the player using a Binary Star: every block with an inventory around it
/// (its slots and the rules of each slot). "open" = open the Binary Star screen.
public record BinaryView(BlockPos pos, boolean open, int drives, int cells, long used, long capacity, List<Block> blocks)
        implements CustomPacketPayload {
    /// One block with an inventory: where it is, its item (icon and name), its slots and the rules.
    public record Block(BlockPos pos, ItemStack icon, List<ItemStack> slots, List<SlotRule> rules) {
    }

    public static final Type<BinaryView> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "binary_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BinaryView> STREAM_CODEC = StreamCodec.of(
            (buf, message) -> {
                BlockPos.STREAM_CODEC.encode(buf, message.pos());
                buf.writeBoolean(message.open());
                buf.writeVarInt(message.drives());
                buf.writeVarInt(message.cells());
                buf.writeVarLong(message.used());
                buf.writeVarLong(message.capacity());
                buf.writeVarInt(message.blocks().size());
                for (Block block : message.blocks()) {
                    BlockPos.STREAM_CODEC.encode(buf, block.pos());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, block.icon());
                    buf.writeVarInt(block.slots().size());
                    for (ItemStack stack : block.slots()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                    buf.writeVarInt(block.rules().size());
                    for (SlotRule rule : block.rules()) SlotRule.STREAM_CODEC.encode(buf, rule);
                }
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                boolean open = buf.readBoolean();
                int drives = buf.readVarInt();
                int cells = buf.readVarInt();
                long used = buf.readVarLong();
                long capacity = buf.readVarLong();
                int count = buf.readVarInt();
                List<Block> blocks = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    BlockPos blockPos = BlockPos.STREAM_CODEC.decode(buf);
                    ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    int slotCount = buf.readVarInt();
                    List<ItemStack> slots = new ArrayList<>(slotCount);
                    for (int s = 0; s < slotCount; s++) slots.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    int ruleCount = buf.readVarInt();
                    List<SlotRule> rules = new ArrayList<>(ruleCount);
                    for (int r = 0; r < ruleCount; r++) rules.add(SlotRule.STREAM_CODEC.decode(buf));
                    blocks.add(new Block(blockPos, icon, slots, rules));
                }
                return new BinaryView(pos, open, drives, cells, used, capacity, blocks);
            });

    @Override
    public Type<BinaryView> type() {
        return TYPE;
    }

    /// Sends what the Binary Star at "pos" sees to the player.
    public static void send(ServerPlayer player, BlockPos pos, boolean open) {
        if (!(player.level().getBlockEntity(pos) instanceof BinaryStarBlockEntity binary)) return;
        StarNetwork network = StarNetwork.at(player.level(), pos);
        List<Block> blocks = new ArrayList<>();
        StarNetwork.forEachBlockEntity(player.level(), pos, BinaryStarBlockEntity.RADIUS, blockEntity -> {
            if (!(blockEntity instanceof Container container) || blockEntity.isRemoved()) return;
            ItemStack icon = new ItemStack(blockEntity.getBlockState().getBlock().asItem());
            if (icon.isEmpty()) return;
            List<ItemStack> slots = new ArrayList<>();
            for (int i = 0; i < container.getContainerSize(); i++) slots.add(container.getItem(i).copy());
            Map<Integer, SlotRule> rules = binary.rulesOf(blockEntity.getBlockPos());
            blocks.add(new Block(blockEntity.getBlockPos(), icon, slots, new ArrayList<>(rules.values())));
        });
        // The closest blocks first
        blocks.sort(Comparator.comparingDouble(block -> block.pos().distSqr(pos)));
        PacketDistributor.sendToPlayer(player, new BinaryView(pos, open, network.drives().size(), network.cellCount(),
                network.usedItems(), network.capacity(), blocks));
    }

    /// Runs on the client: opens or updates the Binary Star screen.
    public static void handle(BinaryView message, IPayloadContext context) {
        BinaryClient.receive(message);
    }
}
