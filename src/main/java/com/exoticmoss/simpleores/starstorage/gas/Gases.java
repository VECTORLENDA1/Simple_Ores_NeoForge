package com.exoticmoss.simpleores.starstorage.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/// The gases of Mekanism, seen without Mekanism: everything here works (and gives nothing) when Mekanism is not
/// installed. The real work is done by MekanismGases, which is only used when Mekanism is there.
public final class Gases {
    private static Boolean loaded;

    private Gases() {
    }

    /// The tanks of gases of a block (a machine, a tank...), seen from a side.
    public interface Tanks {
        int count();

        /// The gas in a tank (null = empty).
        @Nullable ResourceLocation gas(int tank);

        long amount(int tank);

        long capacity(int tank);

        /// Puts up to "amount" mB of the gas in the tanks. Returns how many mB (would) go in.
        long fill(ResourceLocation gas, long amount, boolean simulate);

        /// Takes up to "amount" mB of the gas out of the tanks. Returns how many mB (would) come out.
        long drain(ResourceLocation gas, long amount, boolean simulate);
    }

    public static boolean available() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("mekanism");
        return loaded;
    }

    /// The tanks of gases of the block at "pos" (null = it has none, or Mekanism is not installed).
    public static @Nullable Tanks tanks(Level level, BlockPos pos, @Nullable Direction side) {
        return available() ? MekanismGases.tanks(level, pos, side) : null;
    }

    /// The name of a gas (its id when Mekanism is not installed).
    public static Component name(ResourceLocation gas) {
        return available() ? MekanismGases.name(gas) : Component.literal(gas.toString());
    }

    /// The color of a gas (0xAARRGGBB).
    public static int color(ResourceLocation gas) {
        return available() ? MekanismGases.color(gas) : 0xFFA0A0A0;
    }

    /// A gas shown by JEI in a recipe (null = it is not a gas).
    public static @Nullable Gas fromIngredient(Object ingredient) {
        return available() ? MekanismGases.fromIngredient(ingredient) : null;
    }
}
