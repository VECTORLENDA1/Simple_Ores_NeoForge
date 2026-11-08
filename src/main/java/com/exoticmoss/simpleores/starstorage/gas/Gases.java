package com.exoticmoss.simpleores.starstorage.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/// The gases of Mekanism, seen without Mekanism. Mekanism is not made for this version of Minecraft, so there are no
/// gases here: everything works and gives nothing (the gases of the cells, the recipes and the Binary Star stay
/// ready for when a mod with gases comes).
public final class Gases {
    private Gases() {
    }

    /// The tanks of gases of a block (a machine, a tank...), seen from a side.
    public interface Tanks {
        int count();

        /// The gas in a tank (null = empty).
        @Nullable Identifier gas(int tank);

        long amount(int tank);

        long capacity(int tank);

        /// Puts up to "amount" mB of the gas in the tanks. Returns how many mB (would) go in.
        long fill(Identifier gas, long amount, boolean simulate);

        /// Takes up to "amount" mB of the gas out of the tanks. Returns how many mB (would) come out.
        long drain(Identifier gas, long amount, boolean simulate);
    }

    public static boolean available() {
        return false;
    }

    /// The tanks of gases of the block at "pos" (null = it has none, or Mekanism is not installed).
    public static @Nullable Tanks tanks(Level level, BlockPos pos, @Nullable Direction side) {
        return null;
    }

    /// The name of a gas (its id when Mekanism is not installed).
    public static Component name(Identifier gas) {
        return Component.literal(gas.toString());
    }

    /// The color of a gas (0xAARRGGBB).
    public static int color(Identifier gas) {
        return 0xFFA0A0A0;
    }

    /// The gas as Mekanism sees it (its ChemicalStack, e.g. for JEI; null = Mekanism is not installed).
    public static @Nullable Object stack(Identifier gas, long amount) {
        return null;
    }

    /// The texture of a gas (in the blocks atlas; null = none).
    public static @Nullable Identifier icon(Identifier gas) {
        return null;
    }

    /// A gas shown by JEI in a recipe (null = it is not a gas).
    public static @Nullable Gas fromIngredient(Object ingredient) {
        return null;
    }
}
