package com.exoticmoss.simpleores.starstorage.cell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;

import com.exoticmoss.simpleores.starstorage.gas.Gas;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/// The liquids kept inside a Fluid Cell: every liquid once, with its amount in mB (1000 mB = 1 bucket).
/// Any liquid of the game works (water, lava, milk, and the liquids of other mods). The cell also keeps the gases of
/// Mekanism ("gases", in mB too): liquids and gases share the room of the cell.
public record FluidContents(List<Entry> entries, List<Gas> gases) {
    public static final FluidContents EMPTY = new FluidContents(List.of(), List.of());

    public FluidContents(List<Entry> entries) {
        this(entries, List.of());
    }

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

    /// (the cells of before the gases have only the list of the liquids)
    public static final Codec<FluidContents> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(inst -> inst.group(
                    Entry.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(FluidContents::entries),
                    Gas.CODEC.listOf().optionalFieldOf("gases", List.of()).forGetter(FluidContents::gases)
            ).apply(inst, FluidContents::new)),
            Entry.CODEC.listOf().xmap(FluidContents::new, FluidContents::entries));
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidContents> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidContents::entries,
            Gas.STREAM_CODEC.apply(ByteBufCodecs.list()), FluidContents::gases,
            FluidContents::new);

    public boolean isEmpty() {
        return entries.isEmpty() && gases.isEmpty();
    }

    /// All the mB inside (all liquids and gases together).
    public long total() {
        long total = 0;
        for (Entry entry : entries) total += entry.amount();
        for (Gas gas : gases) total += gas.amount();
        return total;
    }

    public long amountOfGas(ResourceLocation gas) {
        for (Gas entry : gases) {
            if (entry.id().equals(gas)) return entry.amount();
        }
        return 0;
    }

    /// A new list with "amount" more (or less, if negative) of this gas. A gas with 0 mB goes away.
    public FluidContents plusGas(ResourceLocation gas, long amount) {
        List<Gas> list = new ArrayList<>();
        boolean found = false;
        for (Gas entry : gases) {
            if (entry.id().equals(gas)) {
                found = true;
                long left = entry.amount() + amount;
                if (left > 0) list.add(new Gas(gas, left));
            } else {
                list.add(entry);
            }
        }
        if (!found && amount > 0) list.add(new Gas(gas, amount));
        return new FluidContents(entries, list);
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
        return new FluidContents(list, gases);
    }
}
