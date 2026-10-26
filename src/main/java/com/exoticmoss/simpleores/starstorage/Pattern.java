package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;

/// A recipe saved in a Quasar (a crafting table recipe, 3x3):
///  - output / count: what it makes;
///  - inputs: the 9 slots of the grid; each slot has the items that can go there (from JEI, e.g. every kind
///    of planks), or none for an empty slot. The first one is used when there are many.
public record Pattern(ItemStackTemplate output, int count, List<List<ItemStackTemplate>> inputs) {
    public static final int SIZE = 9;

    public static final Codec<Pattern> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStackTemplate.CODEC.fieldOf("output").forGetter(Pattern::output),
            Codec.INT.fieldOf("count").forGetter(Pattern::count),
            ItemStackTemplate.CODEC.listOf().listOf().fieldOf("inputs").forGetter(Pattern::inputs)
    ).apply(inst, Pattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Pattern> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, Pattern::output,
            ByteBufCodecs.VAR_INT, Pattern::count,
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), Pattern::inputs,
            Pattern::new);

    public ItemStack outputStack() {
        ItemStack stack = output.create();
        stack.setCount(count);
        return stack;
    }

    /// True if this pattern makes this item.
    public boolean makes(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(output.create(), stack);
    }
}
