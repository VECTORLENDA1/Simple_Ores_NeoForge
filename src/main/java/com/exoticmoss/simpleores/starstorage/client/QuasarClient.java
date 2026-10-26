package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.Pattern;
import com.exoticmoss.simpleores.starstorage.QuasarJobs;
import com.exoticmoss.simpleores.starstorage.QuasarPatterns;
import net.minecraft.core.BlockPos;

import java.util.List;

/// Client side of the Quasar messages: the recipes of the Quasar whose screen is open.
public final class QuasarClient {
    private static QuasarPatterns last;
    private static QuasarJobs jobs;

    private QuasarClient() {
    }

    public static void receive(QuasarPatterns message) {
        last = message;
    }

    public static void receiveJobs(QuasarJobs message) {
        jobs = message;
    }

    /// The auto-crafts being made (seen from the Quasar at "pos").
    public static List<QuasarJobs.View> jobs(BlockPos pos) {
        return jobs != null && jobs.pos().equals(pos) ? jobs.jobs() : List.of();
    }

    /// The recipes of the Quasar at "pos" (empty if they didn't come yet).
    public static List<Pattern> patterns(BlockPos pos) {
        return last != null && last.pos().equals(pos) ? last.patterns() : List.of();
    }
}
