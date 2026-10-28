package com.exoticmoss.simpleores.starstorage.network;

import net.minecraft.core.BlockPos;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalLayout;

/// A menu that shows the items of the Magnetar network (the Stellar Terminal and the Quasar):
/// the items can be taken and stored with TerminalAction, and the screen is TerminalScreen.
public interface NetworkMenu {
    /// Where the block of the menu is (the network is the one of the Magnetar whose field reaches it).
    BlockPos networkPos();

    /// Where everything is in the screen.
    TerminalLayout layout();

    /// The index of the result slot (drawn bigger).
    int resultSlot();
}
