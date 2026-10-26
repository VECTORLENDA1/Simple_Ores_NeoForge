package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.BinaryView;
import net.minecraft.client.Minecraft;

/// Client side of the Binary Star messages (kept apart so the server never loads the screen classes).
public final class BinaryClient {
    private BinaryClient() {
    }

    /// The server sent the view of a Binary Star: open the screen, or update the one that is open.
    public static void receive(BinaryView message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof BinaryScreen screen && screen.pos().equals(message.pos())) {
            screen.update(message);
        } else if (message.open()) {
            minecraft.gui.setScreen(new BinaryScreen(message));
        }
    }
}
