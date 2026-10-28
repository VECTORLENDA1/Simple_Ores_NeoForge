package com.exoticmoss.simpleores.starstorage.binary;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import com.exoticmoss.simpleores.util.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// What the Magnetar does with one slot of a block (chest, furnace...) inside its field.
/// "mode" has two switches (they can be on together):
///  - TAKE: whatever is in the slot goes to the network (e.g. the output of a furnace);
///  - KEEP: the slot is kept full of the "items" from the network (e.g. ore in the input, coal in the fuel).
///    More than one item: each one is put in the slot (e.g. a trash can of other mods that destroys them all).
///  - BOTH (TAKE + KEEP): for slots that are input and output at the same time (some machines of other mods):
///    the slot is kept full of the item, and anything else that appears in it goes to the network.
public record SlotRule(int slot, int mode, List<ItemStackTemplate> items) {
    public static final int TAKE = 1;
    public static final int KEEP = 2;
    public static final int BOTH = TAKE | KEEP;

    public boolean takes() {
        return (mode & TAKE) != 0;
    }

    public boolean keeps() {
        return (mode & KEEP) != 0 && !items.isEmpty();
    }

    /// The first item kept (empty if none).
    public Optional<ItemStackTemplate> item() {
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    /// The same rule with this item added (or, if it was there, removed).
    public SlotRule toggle(ItemStackTemplate item) {
        List<ItemStackTemplate> list = new ArrayList<>(items);
        if (!list.remove(item)) list.add(item);
        return new SlotRule(slot, mode, list);
    }

    // "item" = how the rules were saved before (one item)
    public static final Codec<SlotRule> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("slot").forGetter(SlotRule::slot),
            Codec.INT.fieldOf("mode").forGetter(SlotRule::mode),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(SlotRule::items),
            ItemStackTemplate.CODEC.optionalFieldOf("item").forGetter(rule -> Optional.empty())
    ).apply(inst, (slot, mode, items, old) -> {
        List<ItemStackTemplate> list = new ArrayList<>(items);
        old.ifPresent(item -> {
            if (!list.contains(item)) list.add(item);
        });
        return new SlotRule(slot, mode, list);
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlotRule> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotRule::slot,
            ByteBufCodecs.VAR_INT, SlotRule::mode,
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), SlotRule::items,
            SlotRule::new);
}
