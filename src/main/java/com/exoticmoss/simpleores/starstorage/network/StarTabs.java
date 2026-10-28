package com.exoticmoss.simpleores.starstorage.network;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.binary.BinaryStarBlock;
import com.exoticmoss.simpleores.starstorage.binary.BinaryStarBlockEntity;
import com.exoticmoss.simpleores.starstorage.client.NetworkTabs;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarBlock;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarBlockEntity;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarBlock;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarBlockEntity;
import com.exoticmoss.simpleores.starstorage.terminal.StellarTerminalBlock;
import com.exoticmoss.simpleores.starstorage.terminal.StellarTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// The tabs on top of the screens of the terminals, the Magnetar, the Quasars and the Binary Stars: one for each of
/// these blocks in the field of the same Magnetar, so the player can go from one screen to the other without
/// walking to each block. Sent by the server when one of those screens opens.
public record StarTabs(List<Tab> tabs) implements CustomPacketPayload {
    /// At most this many tabs are sent.
    public static final int MAX_TABS = 64;

    /// One tab: the block and its icon.
    public record Tab(BlockPos pos, ItemStack icon) {
        static final StreamCodec<RegistryFriendlyByteBuf, Tab> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Tab::pos,
                ItemStack.OPTIONAL_STREAM_CODEC, Tab::icon,
                Tab::new);
    }

    /// The menus of these blocks: the block the player really is next to (the anchor). A screen opened with a tab
    /// keeps the anchor of the screen it came from, so it stays open while the player stays next to that block.
    public interface Anchored {
        BlockPos menuPos();

        BlockPos anchor();

        void setAnchor(BlockPos anchor);
    }

    public static final Type<StarTabs> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleOres.MODID, "star_tabs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StarTabs> STREAM_CODEC = StreamCodec.composite(
            Tab.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TABS)), StarTabs::tabs,
            StarTabs::new);

    @Override
    public Type<StarTabs> type() {
        return TYPE;
    }

    /// True if the player is close enough to the anchor of the menu.
    public static boolean near(net.minecraft.world.entity.player.Player player, BlockPos anchor, double range) {
        return player.position().distanceToSqr(Vec3.atCenterOf(anchor)) <= range * range;
    }

    /// The tabs of the field of the Magnetar of "pos": the terminals, the Magnetar, the Quasars, the Binary Stars.
    public static List<Tab> tabs(Level level, BlockPos pos) {
        MagnetarBlockEntity magnetar = StarNetwork.findMagnetar(level, pos);
        if (magnetar == null) return List.of();
        List<List<BlockEntity>> kinds = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        StarNetwork.forEachBlockEntity(level, magnetar.getBlockPos(), magnetar.radius(), blockEntity -> {
            if (blockEntity.isRemoved()) return;
            if (blockEntity instanceof StellarTerminalBlockEntity || blockEntity instanceof com.exoticmoss.simpleores.starstorage.terminal.AdvancedTerminalBlockEntity) {
                kinds.get(0).add(blockEntity);
            }
            else if (blockEntity instanceof MagnetarBlockEntity && blockEntity == magnetar) kinds.get(1).add(blockEntity);
            else if (blockEntity instanceof QuasarBlockEntity) kinds.get(2).add(blockEntity);
            else if (blockEntity instanceof BinaryStarBlockEntity) kinds.get(3).add(blockEntity);
        });
        if (kinds.get(1).isEmpty()) kinds.get(1).add(magnetar);
        List<Tab> tabs = new ArrayList<>();
        for (List<BlockEntity> kind : kinds) {
            kind.sort(Comparator.comparing(BlockEntity::getBlockPos));
            for (BlockEntity blockEntity : kind) {
                if (tabs.size() < MAX_TABS) tabs.add(new Tab(blockEntity.getBlockPos(), new ItemStack(blockEntity.getBlockState().getBlock())));
            }
        }
        return tabs;
    }

    public static void send(ServerPlayer player, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new StarTabs(tabs(player.level(), pos)));
    }

    public static void handle(StarTabs message, IPayloadContext context) {
        NetworkTabs.receive(message.tabs());
    }

    /// Opens the screen of a block (the terminal, the Magnetar, a Quasar or a Binary Star) with "anchor" as the block
    /// the player is next to. Returns false if the block is not one of them.
    public static boolean open(ServerPlayer player, BlockPos pos, BlockPos anchor) {
        Block block = player.level().getBlockState(pos).getBlock();
        if (!(block instanceof StellarTerminalBlock || block instanceof MagnetarBlock || block instanceof QuasarBlock
                || block instanceof BinaryStarBlock)) return false;
        // The screen open now is closed without telling the client: then the client goes straight to the new screen
        // (closing it first would put the mouse cursor back in the middle of the window)
        player.doCloseContainer();
        if (block instanceof StellarTerminalBlock) StellarTerminalBlock.open(player, pos);
        else if (block instanceof MagnetarBlock) MagnetarBlock.open(player, pos);
        else if (block instanceof QuasarBlock) QuasarBlock.open(player, pos);
        else if (block instanceof BinaryStarBlock) BinaryStarBlock.open(player, pos);
        else return false;
        if (player.containerMenu instanceof Anchored menu && menu.menuPos().equals(pos)) menu.setAnchor(anchor);
        send(player, pos);
        return true;
    }

    /// Sent by the client when a tab is clicked: opens the screen of that block (only a block in the field of the
    /// same Magnetar as the screen open now).
    public record Open(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SimpleOres.MODID, "star_tab_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Open::pos,
                Open::new);

        @Override
        public Type<Open> type() {
            return TYPE;
        }

        public static void handle(Open message, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof Anchored menu)) return;
            if (!player.containerMenu.stillValid(player) || menu.menuPos().equals(message.pos())) return;
            Level level = player.level();
            if (!level.isLoaded(message.pos())) return;
            MagnetarBlockEntity here = StarNetwork.findMagnetar(level, menu.menuPos());
            MagnetarBlockEntity there = StarNetwork.findMagnetar(level, message.pos());
            if (here == null || there == null || !here.getBlockPos().equals(there.getBlockPos())) return;
            open(player, message.pos(), menu.anchor());
        }
    }
}
