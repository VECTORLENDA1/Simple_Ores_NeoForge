package com.exoticmoss.simpleores.starstorage.render;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.exoticmoss.simpleores.util.render.SubmitNodeCollector;
import net.minecraft.Util;
import org.joml.Vector3f;


/// Draws the ITEM of a star of the star storage (Magnetar Core, Stellar Terminal) as a round 3D star.
/// Used from the item model files: { "type": "simpleores:storage_star", "star": "magnetar" }
/// The Star Cells use CellItemRenderer (the same small star seen inside the Stellar Drive).
public class StarItemRenderer {
    private final StarLook look;

    public StarItemRenderer(StarLook look) {
        this.look = look;
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        float time = (Util.getMillis() % 1_000_000L) / 50f;
        CoreDrawing.Shading shading = (p, x, y, z) -> p.normal().transform(new Vector3f(x, y, z)).z();
        // No beams on the item (they would be much bigger than the slot)
        StarDrawing.draw(look, pose, collector, look.itemRadius, time * 1.5f, 0, shading);
    }
}
