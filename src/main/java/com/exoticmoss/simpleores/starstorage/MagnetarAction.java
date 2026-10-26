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

/// Sent by the client when the player changes the rules in the Magnetar screen.
/// The server checks everything again, changes the rules and sends back the new view.
public record MagnetarAction(BlockPos magnetar, BlockPos block, int action, int slot) implements CustomPacketPayload {
    /// Only asks for the view again (the screen does it every second).
    public static final int REFRESH = 0;
    /// Left-click on a slot: TAKE (or nothing, if it was already TAKE).
    public static final int TOGGLE_TAKE = 1;
    /// Right-click on a slot: KEEP the item in the slot, or in the player's hand (or nothing, if it was already KEEP).
    public static final int TOGGLE_KEEP = 2;
    /// Every slot of the block: TAKE.
    public static final int TAKE_ALL = 3;
    /// No rules for the block.
    public static final int CLEAR = 4;

    /// How far (in blocks) the player can be from the Magnetar.
    public static final double RANGE = 8;

    public static final Type<MagnetarAction> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "magnetar_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagnetarAction> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MagnetarAction::magnetar,
            BlockPos.STREAM_CODEC, MagnetarAction::block,
            ByteBufCodecs.VAR_INT, MagnetarAction::action,
            ByteBufCodecs.VAR_INT, MagnetarAction::slot,
            MagnetarAction::new);

    @Override
    public Type<MagnetarAction> type() {
        return TYPE;
    }

    public static void handle(MagnetarAction message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BlockPos pos = message.magnetar();
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE) return;
        if (!(player.level().getBlockEntity(pos) instanceof MagnetarBlockEntity magnetar)) return;

        BlockPos block = message.block();
        if (message.action() != REFRESH && StarNetwork.inField(pos, block)
                && player.level().getBlockEntity(block) instanceof Container container) {
            int slot = message.slot();
            SlotRule old = magnetar.rulesOf(block).get(slot);
            switch (message.action()) {
                case TOGGLE_TAKE -> {
                    if (slot >= 0 && slot < container.getContainerSize()) {
                        boolean wasTake = old != null && old.mode() == SlotRule.TAKE;
                        magnetar.setRule(block, slot, wasTake ? null : new SlotRule(slot, SlotRule.TAKE, Optional.empty()));
                    }
                }
                case TOGGLE_KEEP -> {
                    if (slot >= 0 && slot < container.getContainerSize()) {
                        if (old != null && old.mode() == SlotRule.KEEP) {
                            magnetar.setRule(block, slot, null);
                        } else {
                            // The item to keep: the one in the slot, or else the one in the player's hand
                            ItemStack item = container.getItem(slot);
                            if (item.isEmpty()) item = player.getMainHandItem();
                            if (!item.isEmpty()) {
                                magnetar.setRule(block, slot, new SlotRule(slot, SlotRule.KEEP,
                                        Optional.of(ItemStackTemplate.fromNonEmptyStack(item.copyWithCount(1)))));
                            }
                        }
                    }
                }
                case TAKE_ALL -> {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        magnetar.setRule(block, i, new SlotRule(i, SlotRule.TAKE, Optional.empty()));
                    }
                }
                case CLEAR -> magnetar.clearRules(block);
                default -> {
                }
            }
        }
        MagnetarView.send(player, pos, false);
    }
}
