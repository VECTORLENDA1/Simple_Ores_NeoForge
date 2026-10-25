package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.TerminalContents;
import net.minecraft.client.Minecraft;

/// Client side of the terminal messages (kept apart so the server never loads the screen classes).
public final class TerminalClient {
    private TerminalClient() {
    }

    /// The server sent the contents of the network: open the screen, or update the one that is open.
    public static void receive(TerminalContents message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TerminalScreen screen && screen.pos().equals(message.pos())) {
            screen.update(message);
        } else if (message.open()) {
            minecraft.gui.setScreen(new TerminalScreen(message));
        }
    }
}
