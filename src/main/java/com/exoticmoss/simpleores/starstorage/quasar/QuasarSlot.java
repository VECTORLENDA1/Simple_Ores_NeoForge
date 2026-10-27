package com.exoticmoss.simpleores.starstorage.quasar;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/// Sent by the client when an item of the JEI list is dragged onto a slot of the recipe grid of the Quasar.
public record QuasarSlot(int slot, ItemStack item) implements CustomPacketPayload {
    public static final Type<QuasarSlot> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "quasar_slot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, QuasarSlot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, QuasarSlot::slot,
            ItemStack.OPTIONAL_STREAM_CODEC, QuasarSlot::item,
            QuasarSlot::new);

    @Override
    public Type<QuasarSlot> type() {
        return TYPE;
    }

    public static void handle(QuasarSlot message, IPayloadContext context) {
        if (context.player().containerMenu instanceof QuasarMenu menu) menu.setPicture(message.slot(), message.item());
    }
}
