package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.terminal.CraftRequest;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalContents;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/// Client side of the terminal messages (kept apart so the server never loads the screen classes).
public final class TerminalClient {
    /// The last contents the server sent (the screen may open just after they arrive).
    private static TerminalContents last;

    private TerminalClient() {
    }

    /// The server sent the contents of the network: update the screen that is open.
    public static void receive(TerminalContents message) {
        last = message;
        if (Minecraft.getInstance().screen instanceof TerminalScreen<?> screen && screen.getMenu().networkPos().equals(message.pos())) {
            screen.update(message);
        }
    }

    /// The answer of the server to a craft request.
    public static void receiveReply(CraftRequest.Reply reply) {
        if (Minecraft.getInstance().screen instanceof TerminalScreen<?> screen) screen.onCraftReply(reply);
    }

    /// The last contents of the terminal at "pos" (empty if none came yet).
    public static TerminalContents last(BlockPos pos) {
        return last != null && last.pos().equals(pos) ? last : TerminalContents.empty(pos);
    }

    /// How many rows of items fit in the window (smaller windows show fewer).
    public static int rows() {
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Mth.clamp((height - 16 - TerminalLayout.FIXED_HEIGHT) / TerminalLayout.CELL_H, TerminalLayout.MIN_ROWS, TerminalLayout.MAX_ROWS);
    }
}
