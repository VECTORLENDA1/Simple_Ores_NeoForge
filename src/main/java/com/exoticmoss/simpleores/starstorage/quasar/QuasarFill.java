package com.exoticmoss.simpleores.starstorage.quasar;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/// Sent by the client when JEI's "+" button is used with the Quasar screen open: the items of the recipe,
/// for each of the 9 slots the items that can go there (e.g. every kind of planks).
/// For the recipe of a machine of another mod also: how many of each slot, what it makes, and the machines
/// (empty output = a crafting table recipe).
public record QuasarFill(List<List<ItemStack>> slots, List<Integer> amounts, ItemStack output, List<ItemStack> machines)
        implements CustomPacketPayload {
    /// At most this many choices are kept for one slot.
    public static final int MAX_CHOICES = 32;
    /// At most this many machines are kept for one recipe.
    public static final int MAX_MACHINES = 32;

    /// A crafting table recipe.
    public QuasarFill(List<List<ItemStack>> slots) {
        this(slots, List.of(), ItemStack.EMPTY, List.of());
    }

    public static final Type<QuasarFill> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "quasar_fill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuasarFill> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CHOICES)).apply(ByteBufCodecs.list(Pattern.SIZE)), QuasarFill::slots,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(Pattern.SIZE)), QuasarFill::amounts,
            ItemStack.OPTIONAL_STREAM_CODEC, QuasarFill::output,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_MACHINES)), QuasarFill::machines,
            QuasarFill::new);

    @Override
    public Type<QuasarFill> type() {
        return TYPE;
    }

    public static void handle(QuasarFill message, IPayloadContext context) {
        if (context.player().containerMenu instanceof QuasarMenu menu) menu.fill(message);
    }
}
