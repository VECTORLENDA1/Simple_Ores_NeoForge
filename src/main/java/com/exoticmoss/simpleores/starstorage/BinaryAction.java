package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/// Sent by the client when the player changes the rules in the Binary Star screen.
/// The server checks everything again, changes the rules and sends back the new view.
public record BinaryAction(BlockPos binary, BlockPos block, int action, int slot) implements CustomPacketPayload {
    /// Only asks for the view again (the screen does it every second).
    public static final int REFRESH = 0;
    /// Left-click on a slot: next mode (nothing -> take -> keep -> input and output -> nothing).
    /// With an item in the mouse cursor: that item becomes the item the slot keeps.
    public static final int CLICK = 1;
    /// Right-click on a slot: no rule.
    public static final int REMOVE = 2;
    /// Every slot of the block: TAKE.
    public static final int TAKE_ALL = 3;
    /// No rules for the block.
    public static final int CLEAR = 4;

    /// How far (in blocks) the player can be from the Binary Star.
    public static final double RANGE = 8;

    public static final Type<BinaryAction> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "binary_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BinaryAction> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BinaryAction::binary,
            BlockPos.STREAM_CODEC, BinaryAction::block,
            ByteBufCodecs.VAR_INT, BinaryAction::action,
            ByteBufCodecs.VAR_INT, BinaryAction::slot,
            BinaryAction::new);

    @Override
    public Type<BinaryAction> type() {
        return TYPE;
    }

    public static void handle(BinaryAction message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof BinaryMenu)) return;
        BlockPos pos = message.binary();
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE) return;
        if (!(player.level().getBlockEntity(pos) instanceof BinaryStarBlockEntity binary)) return;

        BlockPos block = message.block();
        if (message.action() != REFRESH && binary.inReach(block)
                && player.level().getBlockEntity(block) instanceof Container container) {
            int slot = message.slot();
            SlotRule old = binary.rulesOf(block).get(slot);
            switch (message.action()) {
                case CLICK -> {
                    if (slot >= 0 && slot < container.getContainerSize()) click(binary, block, container, slot, player.containerMenu.getCarried());
                }
                case REMOVE -> binary.setRule(block, slot, null);
                case TAKE_ALL -> {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        SlotRule rule = binary.rulesOf(block).get(i);
                        binary.setRule(block, i, new SlotRule(i, (rule == null ? 0 : rule.mode()) | SlotRule.TAKE,
                                rule == null ? Optional.empty() : rule.item()));
                    }
                }
                case CLEAR -> binary.clearRules(block);
                default -> {
                }
            }
        }
        BinaryView.send(player, pos, false);
    }

    /// Left-click on a slot: with an item in the mouse cursor, the slot keeps that item; else the next mode.
    /// The modes that keep an item need one: the one already chosen, or the one in the slot (else they are skipped).
    private static void click(BinaryStarBlockEntity binary, BlockPos block, Container container, int slot, ItemStack carried) {
        SlotRule old = binary.rulesOf(block).get(slot);
        int mode = old == null ? 0 : old.mode();
        Optional<ItemStackTemplate> item = old == null ? Optional.empty() : old.item();
        if (!carried.isEmpty()) {
            item = Optional.of(ItemStackTemplate.fromNonEmptyStack(carried.copyWithCount(1)));
            binary.setRule(block, slot, new SlotRule(slot, (mode & SlotRule.TAKE) | SlotRule.KEEP, item));
            return;
        }
        if (item.isEmpty() && !container.getItem(slot).isEmpty()) {
            item = Optional.of(ItemStackTemplate.fromNonEmptyStack(container.getItem(slot).copyWithCount(1)));
        }
        int[] order = {0, SlotRule.TAKE, SlotRule.KEEP, SlotRule.BOTH};
        int at = 0;
        for (int i = 0; i < order.length; i++) if (order[i] == mode) at = i;
        int next = 0;
        for (int step = 1; step <= order.length; step++) {
            next = order[(at + step) % order.length];
            if ((next & SlotRule.KEEP) == 0 || item.isPresent()) break;
        }
        binary.setRule(block, slot, next == 0 ? null : new SlotRule(slot, next, (next & SlotRule.KEEP) != 0 ? item : Optional.empty()));
    }
}
