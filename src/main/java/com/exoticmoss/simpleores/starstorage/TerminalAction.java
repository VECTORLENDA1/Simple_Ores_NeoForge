package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/// Sent by the client when the player does something in the terminal screen.
/// The server checks everything again, changes the network and sends back the new contents.
public record TerminalAction(BlockPos pos, int action, Optional<ItemStackTemplate> item, int slot) implements CustomPacketPayload {
    /// Only asks for the contents again (the screen does it every second).
    public static final int REFRESH = 0;
    /// Takes a full stack of "item" from the network.
    public static final int TAKE_STACK = 1;
    /// Takes one "item" from the network.
    public static final int TAKE_ONE = 2;
    /// Puts the stack of the inventory slot "slot" in the network.
    public static final int INSERT_SLOT = 3;
    /// Puts every item of the inventory (not the hotbar) in the network.
    public static final int INSERT_ALL = 4;

    /// Slots 0 to 8 are the hotbar, 9 to 35 the rest of the inventory.
    public static final int HOTBAR_SIZE = 9;
    public static final int INVENTORY_SIZE = 36;

    /// How far (in blocks) the player can be from the terminal.
    public static final double RANGE = 8;

    public static final Type<TerminalAction> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "terminal_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAction> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalAction::pos,
            ByteBufCodecs.VAR_INT, TerminalAction::action,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), TerminalAction::item,
            ByteBufCodecs.VAR_INT, TerminalAction::slot,
            TerminalAction::new);

    @Override
    public Type<TerminalAction> type() {
        return TYPE;
    }

    public static void handle(TerminalAction message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BlockPos pos = message.pos();
        if (player.position().distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE) return;
        if (!(player.level().getBlockState(pos).getBlock() instanceof StellarTerminalBlock)) return;
        StarNetwork network = StarNetwork.at(player.level(), pos);
        Inventory inventory = player.getInventory();

        switch (message.action()) {
            case TAKE_STACK, TAKE_ONE -> message.item().ifPresent(template -> {
                ItemStack wanted = template.create();
                int amount = message.action() == TAKE_ONE ? 1 : wanted.getMaxStackSize();
                ItemStack taken = network.extract(wanted.copyWithCount(1), amount);
                if (taken.isEmpty()) return;
                inventory.add(taken);
                // What did not fit in the inventory goes back to the network (or to the ground)
                if (!taken.isEmpty()) {
                    ItemStack left = network.insert(taken);
                    if (!left.isEmpty()) player.drop(left, false);
                }
            });
            case INSERT_SLOT -> insertSlot(network, inventory, message.slot());
            case INSERT_ALL -> {
                for (int slot = HOTBAR_SIZE; slot < INVENTORY_SIZE; slot++) {
                    insertSlot(network, inventory, slot);
                }
            }
            default -> {
            }
        }
        TerminalContents.send(player, pos, false);
    }

    private static void insertSlot(StarNetwork network, Inventory inventory, int slot) {
        if (slot < 0 || slot >= INVENTORY_SIZE) return;
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) return;
        inventory.setItem(slot, network.insert(stack.copy()));
    }
}
