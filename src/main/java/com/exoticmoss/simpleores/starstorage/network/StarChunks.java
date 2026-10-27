package com.exoticmoss.simpleores.starstorage.network;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/// Keeps chunks loaded for the Magnetars with Wormhole Links (see MagnetarBlockEntity.updateChunks):
/// the chunks of their own field and of the fields of the Magnetars they point to.
public final class StarChunks {
    public static final TicketController CONTROLLER =
            new TicketController(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "wormhole_link"));

    private StarChunks() {
    }

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }
}
