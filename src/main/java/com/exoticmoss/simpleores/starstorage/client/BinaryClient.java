package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.BinaryView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/// Client side of the Binary Star messages (kept apart so the server never loads the screen classes).
public final class BinaryClient {
    private static BinaryView last;

    private BinaryClient() {
    }

    /// The server sent the view of a Binary Star: update the screen that is open.
    public static void receive(BinaryView message) {
        last = message;
        if (Minecraft.getInstance().gui.screen() instanceof BinaryScreen screen && screen.getMenu().pos.equals(message.pos())) {
            screen.update(message);
        }
    }

    /// The last view of the Binary Star at "pos" (empty if none came yet).
    public static BinaryView last(BlockPos pos) {
        return last != null && last.pos().equals(pos) ? last : new BinaryView(pos, false, 0, 0, 0, 0, List.of(), com.exoticmoss.simpleores.starstorage.BinaryStarBlockEntity.RADIUS);
    }

    /// Height of the panel: as tall as fits in the window (at most 300 pixels).
    public static int height() {
        return Math.min(Minecraft.getInstance().getWindow().getGuiScaledHeight() - 20, 300);
    }
}
