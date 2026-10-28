package com.exoticmoss.simpleores.starstorage.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/// An item (or a liquid) drawn by a screen of the star storage under the mouse, for JEI: with it the keys of JEI
/// (R = recipes, U = uses) and its clicks work on the items of the network, the recipes, the auto-crafts...
/// "x", "y", "size" = where it is drawn on the screen.
public record JeiTarget(ItemStack item, @Nullable Fluid fluid, int x, int y, int size) {
    /// A screen that can tell JEI what is under the mouse.
    public interface Source {
        @Nullable JeiTarget jeiTargetAt(double mouseX, double mouseY);
    }

    public static @Nullable JeiTarget of(ItemStack item, int x, int y) {
        return item.isEmpty() ? null : new JeiTarget(item, null, x, y, 16);
    }
}
