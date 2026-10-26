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
    /// Left-click on a slot: turns TAKE on or off.
    public static final int TOGGLE_TAKE = 1;
    /// Right-click on a slot: turns KEEP on (with the item in the slot, or in the player's hand) or off.
    /// With TAKE and KEEP on together, the slot is BOTH (input and output).
    public static final int TOGGLE_KEEP = 2;
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
        if (!(context.player() instanceof ServerPlayer player)) return;
        BlockPos pos = message.binary();
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE) return;
        if (!(player.level().getBlockEntity(pos) instanceof BinaryStarBlockEntity binary)) return;

        BlockPos block = message.block();
        if (message.action() != REFRESH && binary.inReach(block)
                && player.level().getBlockEntity(block) instanceof Container container) {
            int slot = message.slot();
            SlotRule old = binary.rulesOf(block).get(slot);
            switch (message.action()) {
                case TOGGLE_TAKE -> {
                    if (slot >= 0 && slot < container.getContainerSize()) {
                        int mode = (old == null ? 0 : old.mode()) ^ SlotRule.TAKE;
                        binary.setRule(block, slot, mode == 0 ? null : new SlotRule(slot, mode, old == null ? Optional.empty() : old.item()));
                    }
                }
                case TOGGLE_KEEP -> {
                    if (slot >= 0 && slot < container.getContainerSize()) {
                        int oldMode = old == null ? 0 : old.mode();
                        if ((oldMode & SlotRule.KEEP) != 0) {
                            int mode = oldMode & ~SlotRule.KEEP;
                            binary.setRule(block, slot, mode == 0 ? null : new SlotRule(slot, mode, Optional.empty()));
                        } else {
                            // The item to keep: the one in the slot, or else the one in the player's hand
                            ItemStack item = container.getItem(slot);
                            if (item.isEmpty()) item = player.getMainHandItem();
                            if (!item.isEmpty()) {
                                binary.setRule(block, slot, new SlotRule(slot, oldMode | SlotRule.KEEP,
                                        Optional.of(ItemStackTemplate.fromNonEmptyStack(item.copyWithCount(1)))));
                            }
                        }
                    }
                }
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
}
