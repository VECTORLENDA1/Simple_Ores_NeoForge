package com.exoticmoss.simpleores.starstorage.cell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/// The liquids kept inside a Fluid Cell: every liquid once, with its amount in mB (1000 mB = 1 bucket).
/// Any liquid of the game works (water, lava, milk, and the liquids of other mods).
public record FluidContents(List<Entry> entries) {
    public static final FluidContents EMPTY = new FluidContents(List.of());

    public record Entry(Fluid fluid, long amount) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(Entry::fluid),
                Codec.LONG.fieldOf("amount").forGetter(Entry::amount)
        ).apply(inst, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.FLUID), Entry::fluid,
                ByteBufCodecs.VAR_LONG, Entry::amount,
                Entry::new);
    }

    public static final Codec<FluidContents> CODEC = Entry.CODEC.listOf().xmap(FluidContents::new, FluidContents::entries);
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidContents> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(FluidContents::new, FluidContents::entries);

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /// All the mB inside (all liquids together).
    public long total() {
        long total = 0;
        for (Entry entry : entries) total += entry.amount();
        return total;
    }

    public long amountOf(Fluid fluid) {
        for (Entry entry : entries) {
            if (entry.fluid() == fluid) return entry.amount();
        }
        return 0;
    }

    /// A new list with "amount" more (or less, if negative) of this liquid. A liquid with 0 mB goes away.
    public FluidContents plus(Fluid fluid, long amount) {
        List<Entry> list = new ArrayList<>();
        boolean found = false;
        for (Entry entry : entries) {
            if (entry.fluid() == fluid) {
                found = true;
                long left = entry.amount() + amount;
                if (left > 0) list.add(new Entry(fluid, left));
            } else {
                list.add(entry);
            }
        }
        if (!found && amount > 0) list.add(new Entry(fluid, amount));
        return new FluidContents(list);
    }
}
