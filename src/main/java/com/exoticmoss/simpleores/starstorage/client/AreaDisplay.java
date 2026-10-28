package com.exoticmoss.simpleores.starstorage.client;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/// Client: the areas shown with the "Show area" button of the Magnetar and of the Binary Star (a light gray box
/// around the block, drawn by StarBlockRenderer) until the button is pressed again.
public final class AreaDisplay {
    /// Block -> radius of its area (blocks in every direction).
    private static final Map<BlockPos, Integer> SHOWN = new HashMap<>();

    private AreaDisplay() {
    }

    public static boolean isShown(BlockPos pos) {
        return SHOWN.containsKey(pos);
    }

    /// The radius of the area shown for this block (0 = not shown).
    public static int radius(BlockPos pos) {
        return SHOWN.getOrDefault(pos, 0);
    }

    public static void toggle(BlockPos pos, int radius) {
        if (SHOWN.remove(pos) == null) SHOWN.put(pos.immutable(), radius);
    }

    /// The radius changed (upgrades): the box shown changes too.
    public static void update(BlockPos pos, int radius) {
        if (SHOWN.containsKey(pos)) SHOWN.put(pos.immutable(), radius);
    }
}
