package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Optional;

/// What the Magnetar does with one slot of a block (chest, furnace...) inside its field.
/// "mode" has two switches (they can be on together):
///  - TAKE: whatever is in the slot goes to the network (e.g. the output of a furnace);
///  - KEEP: the slot is kept full of the "item" from the network (e.g. ore in the input, coal in the fuel).
///  - BOTH (TAKE + KEEP): for slots that are input and output at the same time (some machines of other mods):
///    the slot is kept full of the item, and anything else that appears in it goes to the network.
public record SlotRule(int slot, int mode, Optional<ItemStackTemplate> item) {
    public static final int TAKE = 1;
    public static final int KEEP = 2;
    public static final int BOTH = TAKE | KEEP;

    public boolean takes() {
        return (mode & TAKE) != 0;
    }

    public boolean keeps() {
        return (mode & KEEP) != 0 && item.isPresent();
    }

    public static final Codec<SlotRule> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("slot").forGetter(SlotRule::slot),
            Codec.INT.fieldOf("mode").forGetter(SlotRule::mode),
            ItemStackTemplate.CODEC.optionalFieldOf("item").forGetter(SlotRule::item)
    ).apply(inst, SlotRule::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotRule> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotRule::slot,
            ByteBufCodecs.VAR_INT, SlotRule::mode,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), SlotRule::item,
            SlotRule::new);
}
