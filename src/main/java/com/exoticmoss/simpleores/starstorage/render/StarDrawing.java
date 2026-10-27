package com.exoticmoss.simpleores.starstorage.render;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;

/// Draws a star of the star storage with its StarLook (used by StarBlockRenderer and StarItemRenderer).
/// The Binary Star is two small stars turning around each other; the others are one star.
public final class StarDrawing {
    /// The blue star of the Binary Star (the orange one uses the colors of StarLook.BINARY).
    public static final int COMPANION_CORE = 0xFFE4EEFF;
    public static final int COMPANION_SPOT = 0xFF4F8CFF;
    public static final int COMPANION_GLOW = 0xFF6FA0FF;
    /// Size of each star of the Binary Star (part of the radius) and how far they are from the middle.
    private static final float BINARY_SIZE = 0.55f;
    private static final float BINARY_ORBIT = 0.5f;
    /// How fast the two stars of the Binary Star turn (degrees per tick).
    private static final float BINARY_SPEED = 3f;

    private StarDrawing() {
    }

    public static void draw(StarLook look, PoseStack pose, SubmitNodeCollector collector, float radius, float time,
                            float beamLength, CoreDrawing.Shading shading) {
        if (look != StarLook.BINARY) {
            CoreDrawing.drawStar(pose, collector, radius, time, look.coreColor, look.spotColor, look.glowColor,
                    look.diskColor, look.beamColor, beamLength, shading);
            return;
        }
        float size = radius * BINARY_SIZE;
        float orbit = radius * BINARY_ORBIT;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(time * BINARY_SPEED));
        pose.mulPose(Axis.XP.rotationDegrees(12));
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.translate(i == 0 ? orbit : -orbit, 0.03f * Mth.sin(time * 0.1f + i * Mth.PI), 0);
            if (i == 0) {
                CoreDrawing.drawStar(pose, collector, size, time, look.coreColor, look.spotColor, look.glowColor, 0, 0, 0, shading);
            } else {
                CoreDrawing.drawStar(pose, collector, size * 0.85f, time + 50, COMPANION_CORE, COMPANION_SPOT, COMPANION_GLOW, 0, 0, 0, shading);
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
