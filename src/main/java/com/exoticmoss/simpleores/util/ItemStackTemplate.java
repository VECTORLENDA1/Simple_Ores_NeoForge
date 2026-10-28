package com.exoticmoss.simpleores.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/// An item with its data (and how many), that can be compared and used as a key of a map (an ItemStack can't).
/// Minecraft 1.21.1 doesn't have one (later versions have com.exoticmoss.simpleores.util.ItemStackTemplate): this one
/// does the same for this mod.
public record ItemStackTemplate(Holder<Item> item, int count, DataComponentPatch components) {
    public static final Codec<ItemStackTemplate> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Item.CODEC.fieldOf("id").forGetter(ItemStackTemplate::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ItemStackTemplate::count),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStackTemplate::components)
    ).apply(inst, ItemStackTemplate::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderRegistry(Registries.ITEM), ItemStackTemplate::item,
            ByteBufCodecs.VAR_INT, ItemStackTemplate::count,
            DataComponentPatch.STREAM_CODEC, ItemStackTemplate::components,
            ItemStackTemplate::new);

    /// The item (with its count and data) of a stack that isn't empty.
    public static ItemStackTemplate fromNonEmptyStack(ItemStack stack) {
        return new ItemStackTemplate(stack.getItemHolder(), stack.getCount(), stack.getComponentsPatch());
    }

    /// A new stack of this item.
    public ItemStack create() {
        return new ItemStack(item, count, components);
    }
}
