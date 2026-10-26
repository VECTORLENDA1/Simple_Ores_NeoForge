package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.MagnetarView;
import net.minecraft.client.Minecraft;

/// Client side of the Magnetar messages (kept apart so the server never loads the screen classes).
public final class MagnetarClient {
    private MagnetarClient() {
    }

    /// The server sent the view of a Magnetar: open the screen, or update the one that is open.
    public static void receive(MagnetarView message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof MagnetarScreen screen && screen.pos().equals(message.pos())) {
            screen.update(message);
        } else if (message.open()) {
            minecraft.gui.setScreen(new MagnetarScreen(message));
        }
    }
}
