package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;

/// The items kept inside a Star Cell. Every kind of item is kept once with its amount,
/// so a cell can keep a HUGE amount of items without being slow.
public record CellContents(List<Entry> entries) {
    public static final CellContents EMPTY = new CellContents(List.of());

    /// One kind of item (with its components) and how many of it the cell has.
    public record Entry(ItemStackTemplate item, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ItemStackTemplate.CODEC.fieldOf("item").forGetter(Entry::item),
                Codec.INT.fieldOf("count").forGetter(Entry::count)
        ).apply(inst, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, Entry::item,
                ByteBufCodecs.VAR_INT, Entry::count,
                Entry::new);

        /// An ItemStack of this item (the amount is the given count).
        public ItemStack create(int amount) {
            ItemStack stack = item.create();
            stack.setCount(amount);
            return stack;
        }

        public boolean is(ItemStack stack) {
            return ItemStack.isSameItemSameComponents(create(1), stack);
        }
    }

    public static final Codec<CellContents> CODEC = Entry.CODEC.listOf().xmap(CellContents::new, CellContents::entries);
    public static final StreamCodec<RegistryFriendlyByteBuf, CellContents> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(CellContents::new, CellContents::entries);

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /// Total amount of items inside (all kinds together).
    public long totalCount() {
        long total = 0;
        for (Entry entry : entries) total += entry.count();
        return total;
    }

    /// Where this item is in the list (-1 = not in the cell).
    public int indexOf(ItemStack stack) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).is(stack)) return i;
        }
        return -1;
    }

    /// A new list with "amount" more of this item (a new entry if the cell didn't have it yet).
    public CellContents plus(ItemStack stack, int amount) {
        List<Entry> list = new ArrayList<>(entries);
        int index = indexOf(stack);
        if (index >= 0) {
            Entry entry = list.get(index);
            list.set(index, new Entry(entry.item(), entry.count() + amount));
        } else {
            list.add(new Entry(ItemStackTemplate.fromNonEmptyStack(stack.copyWithCount(1)), amount));
        }
        return new CellContents(List.copyOf(list));
    }

    /// A new list with "amount" less of the entry at "index" (the entry goes away at 0).
    public CellContents minus(int index, int amount) {
        List<Entry> list = new ArrayList<>(entries);
        Entry entry = list.get(index);
        int left = entry.count() - amount;
        if (left > 0) list.set(index, new Entry(entry.item(), left));
        else list.remove(index);
        return new CellContents(List.copyOf(list));
    }
}
