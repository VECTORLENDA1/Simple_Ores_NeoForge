package com.exoticmoss.simpleores.starstorage.binary;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.client.BinaryClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.exoticmoss.simpleores.starstorage.quasar.Pattern;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import com.exoticmoss.simpleores.starstorage.network.StarAccess;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;

/// Sent by the server to the player using a Binary Star: every block with an inventory around it
/// (its slots and the rules of each slot). "open" = open the Binary Star screen.
public record BinaryView(BlockPos pos, boolean open, int drives, int cells, long used, long capacity, List<Block> blocks, int radius)
        implements CustomPacketPayload {
    /// One block with an inventory: where it is, its item (icon and name), its slots, its tanks, the rules
    /// (the rules of the tanks have the slot numbers BinaryStarBlockEntity.TANKS + tank) and what each slot is
    /// for (SlotRoles: input / output).
    /// "options": the machine recipes of the Quasars this block can make, and which of them are linked to it
    /// (it makes only those, see BinaryStarBlockEntity.links).
    public record Block(BlockPos pos, ItemStack icon, List<ItemStack> slots, List<Tank> tanks, List<SlotRule> rules, List<Integer> roles,
                        List<Pattern> options, List<Boolean> linked) {
    }

    /// One tank: a bucket of its liquid (EMPTY = empty tank or a liquid without bucket), the mB and how many fit.
    public record Tank(ItemStack bucket, long amount, long capacity) {
    }

    public static final Type<BinaryView> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleOres.MODID, "binary_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BinaryView> STREAM_CODEC = StreamCodec.of(
            (buf, message) -> {
                BlockPos.STREAM_CODEC.encode(buf, message.pos());
                buf.writeBoolean(message.open());
                buf.writeVarInt(message.drives());
                buf.writeVarInt(message.cells());
                buf.writeVarLong(message.used());
                buf.writeVarLong(message.capacity());
                buf.writeVarInt(message.radius());
                buf.writeVarInt(message.blocks().size());
                for (Block block : message.blocks()) {
                    BlockPos.STREAM_CODEC.encode(buf, block.pos());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, block.icon());
                    buf.writeVarInt(block.slots().size());
                    for (ItemStack stack : block.slots()) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                    buf.writeVarInt(block.tanks().size());
                    for (Tank tank : block.tanks()) {
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, tank.bucket());
                        buf.writeVarLong(tank.amount());
                        buf.writeVarLong(tank.capacity());
                    }
                    buf.writeVarInt(block.rules().size());
                    for (SlotRule rule : block.rules()) SlotRule.STREAM_CODEC.encode(buf, rule);
                    buf.writeVarInt(block.roles().size());
                    for (int role : block.roles()) buf.writeByte(role);
                    buf.writeVarInt(block.options().size());
                    for (int o = 0; o < block.options().size(); o++) {
                        Pattern.STREAM_CODEC.encode(buf, block.options().get(o));
                        buf.writeBoolean(block.linked().get(o));
                    }
                }
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                boolean open = buf.readBoolean();
                int drives = buf.readVarInt();
                int cells = buf.readVarInt();
                long used = buf.readVarLong();
                long capacity = buf.readVarLong();
                int radius = buf.readVarInt();
                int count = buf.readVarInt();
                List<Block> blocks = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    BlockPos blockPos = BlockPos.STREAM_CODEC.decode(buf);
                    ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    int slotCount = buf.readVarInt();
                    List<ItemStack> slots = new ArrayList<>(slotCount);
                    for (int s = 0; s < slotCount; s++) slots.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    int tankCount = buf.readVarInt();
                    List<Tank> tanks = new ArrayList<>(tankCount);
                    for (int t = 0; t < tankCount; t++) {
                        tanks.add(new Tank(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readVarLong()));
                    }
                    int ruleCount = buf.readVarInt();
                    List<SlotRule> rules = new ArrayList<>(ruleCount);
                    for (int r = 0; r < ruleCount; r++) rules.add(SlotRule.STREAM_CODEC.decode(buf));
                    int roleCount = buf.readVarInt();
                    List<Integer> roles = new ArrayList<>(roleCount);
                    for (int r = 0; r < roleCount; r++) roles.add((int) buf.readByte());
                    int optionCount = buf.readVarInt();
                    List<Pattern> options = new ArrayList<>(optionCount);
                    List<Boolean> linked = new ArrayList<>(optionCount);
                    for (int o = 0; o < optionCount; o++) {
                        options.add(Pattern.STREAM_CODEC.decode(buf));
                        linked.add(buf.readBoolean());
                    }
                    blocks.add(new Block(blockPos, icon, slots, tanks, rules, roles, options, linked));
                }
                return new BinaryView(pos, open, drives, cells, used, capacity, blocks, radius);
            });

    @Override
    public Type<BinaryView> type() {
        return TYPE;
    }

    /// At most this many slots (and tanks) of one block are shown (some blocks of other mods have thousands).
    private static final int MAX_SLOTS = 243;

    /// Sends what the Binary Star at "pos" sees to the player.
    public static void send(ServerPlayer player, BlockPos pos, boolean open) {
        if (!(player.level().getBlockEntity(pos) instanceof BinaryStarBlockEntity binary)) return;
        StarNetwork network = StarNetwork.at(player.level(), pos);
        List<Block> blocks = new ArrayList<>();
        StarNetwork.forEachBlockEntity(player.level(), pos, binary.radius(), blockEntity -> {
            if (blockEntity.isRemoved() || blockEntity instanceof BinaryStarBlockEntity) return;
            BlockPos blockPos = blockEntity.getBlockPos();
            // Any block that has slots or tanks (also of other mods)
            IItemHandler items = StarAccess.items(player.level(), blockPos);
            IFluidHandler fluids = StarAccess.fluids(player.level(), blockPos);
            if ((items == null || items.getSlots() == 0) && (fluids == null || fluids.getTanks() == 0)) return;
            ItemStack icon = new ItemStack(blockEntity.getBlockState().getBlock().asItem());
            if (icon.isEmpty()) return;
            List<ItemStack> slots = new ArrayList<>();
            if (items != null) {
                for (int i = 0; i < Math.min(items.getSlots(), MAX_SLOTS); i++) slots.add(StarAccess.stack(items, i));
            }
            List<Tank> tanks = new ArrayList<>();
            if (fluids != null) {
                for (int t = 0; t < Math.min(fluids.getTanks(), MAX_SLOTS); t++) {
                    FluidStack fluid = fluids.getFluidInTank(t);
                    ItemStack bucket = fluid.isEmpty() ? ItemStack.EMPTY : StarFluids.bucket(fluid.getFluid());
                    tanks.add(new Tank(bucket, fluid.getAmount(), fluids.getTankCapacity(t)));
                }
            }
            Map<Integer, SlotRule> rules = binary.rulesOf(blockPos);
            List<Integer> roles = items == null ? List.of() : SlotRoles.items(player.level(), blockPos, items);
            List<Pattern> options = options(network, blockEntity.getBlockState());
            List<Pattern> links = binary.linksOf(blockPos);
            List<Boolean> linked = new ArrayList<>();
            for (Pattern option : options) linked.add(links.contains(option));
            blocks.add(new Block(blockPos, icon, slots, tanks, new ArrayList<>(rules.values()), roles, options, linked));
        });
        // In the order the blocks were found. The ones found at the same time (e.g.
        // the first time) in groups: first the machines (blocks with inputs or outputs), then the others (chests...);
        // each kind of block together (by name), and the closest first
        List<Block> found = new ArrayList<>(blocks.stream().filter(block -> binary.orderOf(block.pos()) < 0).toList());
        found.sort(Comparator.comparing((Block block) -> block.roles().stream().allMatch(role -> role == SlotRoles.UNKNOWN))
                .thenComparing(block -> block.icon().getHoverName().getString())
                .thenComparingDouble(block -> block.pos().distSqr(pos)));
        binary.addToOrder(found.stream().map(Block::pos).toList());
        // The blocks of the same kind together (where the first one of that kind is), each one in the order it was
        // found: a new machine goes to the end of the machines of its kind
        Map<net.minecraft.world.item.Item, Integer> groupAt = new java.util.HashMap<>();
        for (Block block : blocks) groupAt.merge(block.icon().getItem(), binary.orderOf(block.pos()), Math::min);
        blocks.sort(Comparator.comparingInt((Block block) -> groupAt.get(block.icon().getItem()))
                .thenComparingInt(block -> binary.orderOf(block.pos())));
        PacketDistributor.sendToPlayer(player, new BinaryView(pos, open, network.drives().size(), network.cellCount(),
                network.usedItems(), network.capacity(), blocks, binary.radius()));
    }

    /// The machine recipes of the Quasars of the network that the block can make (in the order of the Quasars).
    public static List<Pattern> options(StarNetwork network, net.minecraft.world.level.block.state.BlockState state) {
        List<Pattern> options = new ArrayList<>();
        for (Pattern pattern : network.patterns()) {
            if (pattern.isMachine() && pattern.madeBy(state) && !options.contains(pattern)) options.add(pattern);
        }
        return options;
    }

    /// The same, for the block at "block", seen from the Binary Star at "binary".
    public static List<Pattern> options(net.minecraft.world.level.Level level, BlockPos binary, BlockPos block) {
        return options(StarNetwork.at(level, binary), level.getBlockState(block));
    }

    /// Runs on the client: opens or updates the Binary Star screen.
    public static void handle(BinaryView message, IPayloadContext context) {
        BinaryClient.receive(message);
    }
}
