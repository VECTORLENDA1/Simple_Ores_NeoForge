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
public record QuasarFill(List<List<ItemStack>> slots) implements CustomPacketPayload {
    /// At most this many choices are kept for one slot.
    public static final int MAX_CHOICES = 32;

    public static final Type<QuasarFill> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "quasar_fill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuasarFill> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CHOICES)).apply(ByteBufCodecs.list(Pattern.SIZE)), QuasarFill::slots,
            QuasarFill::new);

    @Override
    public Type<QuasarFill> type() {
        return TYPE;
    }

    public static void handle(QuasarFill message, IPayloadContext context) {
        if (context.player().containerMenu instanceof QuasarMenu menu) menu.fill(message.slots());
    }
}
