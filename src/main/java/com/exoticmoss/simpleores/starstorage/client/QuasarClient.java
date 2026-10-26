package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.Pattern;
import com.exoticmoss.simpleores.starstorage.QuasarPatterns;
import net.minecraft.core.BlockPos;

import java.util.List;

/// Client side of the Quasar messages: the recipes of the Quasar whose screen is open.
public final class QuasarClient {
    private static QuasarPatterns last;

    private QuasarClient() {
    }

    public static void receive(QuasarPatterns message) {
        last = message;
    }

    /// The recipes of the Quasar at "pos" (empty if they didn't come yet).
    public static List<Pattern> patterns(BlockPos pos) {
        return last != null && last.pos().equals(pos) ? last.patterns() : List.of();
    }
}
