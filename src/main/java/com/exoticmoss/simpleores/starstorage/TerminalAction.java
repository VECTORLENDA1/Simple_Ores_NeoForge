package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;

/// Sent by the client when the player does something in the terminal screen.
/// The server checks everything again, changes the network and sends back the new contents.
public record TerminalAction(BlockPos pos, int action, Optional<ItemStackTemplate> item) implements CustomPacketPayload {
    /// Only asks for the contents again (the screen does it every second).
    public static final int REFRESH = 0;
    /// Takes a stack / one of "item" from the network into the mouse cursor.
    public static final int TAKE_STACK = 1;
    public static final int TAKE_ONE = 2;
    /// Takes a stack of "item" from the network into the inventory (Shift + click).
    public static final int TAKE_TO_INVENTORY = 3;
    /// Puts the stack in the mouse cursor in the network (all of it / only one).
    public static final int STORE_CARRIED = 4;
    public static final int STORE_CARRIED_ONE = 5;
    /// Puts every item of the crafting grid back in the network (the small X button).
    public static final int CLEAR_GRID = 6;

    /// How far (in blocks) the player can be from the terminal.
    public static final double RANGE = 8;

    public static final Type<TerminalAction> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "terminal_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalAction> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalAction::pos,
            ByteBufCodecs.VAR_INT, TerminalAction::action,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), TerminalAction::item,
            TerminalAction::new);

    @Override
    public Type<TerminalAction> type() {
        return TYPE;
    }

    public static void handle(TerminalAction message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        BlockPos pos = message.pos();
        // Only while the screen of this terminal is open
        if (!(player.containerMenu instanceof NetworkMenu networkMenu) || !networkMenu.networkPos().equals(pos)
                || !player.containerMenu.stillValid(player)) return;
        var menu = player.containerMenu;
        StarNetwork network = StarNetwork.at(player.level(), pos);
        ItemStack carried = menu.getCarried();

        switch (message.action()) {
            case TAKE_STACK, TAKE_ONE -> message.item().ifPresent(template -> {
                ItemStack wanted = template.create();
                if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(carried, wanted)) return;
                int room = wanted.getMaxStackSize() - carried.getCount();
                int amount = Math.min(room, message.action() == TAKE_ONE ? 1 : wanted.getMaxStackSize());
                if (amount <= 0) return;
                ItemStack taken = network.extract(wanted.copyWithCount(1), amount);
                if (taken.isEmpty()) return;
                if (carried.isEmpty()) menu.setCarried(taken);
                else carried.grow(taken.getCount());
            });
            case TAKE_TO_INVENTORY -> message.item().ifPresent(template -> {
                ItemStack wanted = template.create();
                ItemStack taken = network.extract(wanted.copyWithCount(1), wanted.getMaxStackSize());
                if (taken.isEmpty()) return;
                player.getInventory().add(taken);
                // What did not fit in the inventory goes back to the network (or to the ground)
                if (!taken.isEmpty()) {
                    ItemStack left = network.insert(taken);
                    if (!left.isEmpty()) player.drop(left, false);
                }
            });
            case STORE_CARRIED -> {
                if (!carried.isEmpty()) menu.setCarried(network.insert(carried));
            }
            case STORE_CARRIED_ONE -> {
                if (!carried.isEmpty() && network.insert(carried.copyWithCount(1)).isEmpty()) carried.shrink(1);
            }
            case CLEAR_GRID -> {
                if (menu instanceof TerminalMenu terminal) terminal.clearGrid(player, network);
            }
            default -> {
            }
        }
        menu.broadcastChanges();
        TerminalContents.send(player, pos);
    }
}
