package com.vector.simpleores.gravitycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vector.simpleores.SimpleOres;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/// Draws the "star" of a Gravity Core: the sphere, its glow, the accretion disk and the light beams.
/// Used by GravityCoreRenderer (the block) and by GravityCoreItemRenderer (the item in the inventory/hand/ground).
///
/// The numbers here control the "style" of the drawing. The colors and sizes come from CoreTier.
public class CoreDrawing {
    /// Plain white texture: the colors come from CoreTier.
    private static final Identifier WHITE = Identifier.fromNamespaceAndPath(SimpleOres.MODID, "textures/entity/gravity_core.png");
    /// Maximum light: the core always glows, even in the dark.
    public static final int FULL_BRIGHT = 0xF000F0;
    /// How many pieces the sphere is made of. More = rounder (but slower to draw).
    private static final int SPHERE_DETAIL = 32;
    /// How tilted (in degrees) the accretion disk is.
    private static final float DISK_TILT = 15;

    /// Tells how much a point of the sphere faces the viewer: 1 = facing it, 0 = at the edge.
    /// Used to make the edges of the sphere darker, which makes it look round (like real stars).
    /// nx, ny, nz is the direction the point faces (a vector of length 1).
    public interface Shading {
        float facing(PoseStack.Pose pose, float nx, float ny, float nz);
    }

    /// Draws a complete star centered on the current position of the pose.
    ///
    /// @param time       animation time in ticks (moves the spots, spins the disk and beams)
    /// @param beamLength length of the light beams in blocks (0 = don't draw beams)
    public static void drawStar(PoseStack pose, SubmitNodeCollector collector, float radius, float time,
                                int coreColor, int spotColor, int glowColor, int diskColor, int beamColor,
                                float beamLength, Shading shading) {
        RenderType type = RenderTypes.entityTranslucentEmissive(WHITE);

        // ---- Sphere with moving spots
        collector.submitCustomGeometry(pose, type, (p, v) -> sphere(p, v, radius, time, coreColor, spotColor, shading));

        // ---- Glow (corona): two transparent layers, the outer one fainter
        collector.submitCustomGeometry(pose, type, (p, v) -> glowSphere(p, v, radius * 1.12f, ARGB.color(100, glowColor)));
        collector.submitCustomGeometry(pose, type, (p, v) -> glowSphere(p, v, radius * 1.35f, ARGB.color(40, glowColor)));

        // ---- Accretion disk (only tiers with a disk color, like the Black Hole)
        if (ARGB.alpha(diskColor) > 0) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(DISK_TILT));
            pose.mulPose(Axis.YP.rotationDegrees(time * 2));
            int outer = ARGB.color(0, glowColor); // fades out to transparent at the edge
            collector.submitCustomGeometry(pose, type, (p, v) -> ring(p, v, radius * 1.15f, radius * 2.3f, diskColor, outer));
            pose.popPose();
        }

        // ---- Light beams (only tiers with a beam color, like the Pulsar): spin around like a lighthouse
        if (ARGB.alpha(beamColor) > 0 && beamLength > 0) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(time * 12));
            pose.mulPose(Axis.ZP.rotationDegrees(25));
            collector.submitCustomGeometry(pose, type, (p, v) -> beam(p, v, radius, beamLength, 1, beamColor));
            collector.submitCustomGeometry(pose, type, (p, v) -> beam(p, v, radius, beamLength, -1, beamColor));
            pose.popPose();
        }
    }

    // ---------------------------------------------------------------- shapes

    /// The main sphere. Each point mixes the core and spot colors, and gets darker near the edges.
    private static void sphere(PoseStack.Pose pose, VertexConsumer v, float radius, float time,
                               int coreColor, int spotColor, Shading shading) {
        forEachSphereQuad((x, y, z) -> {
            int color = ARGB.srgbLerp(spotPattern(x, y, z, time), coreColor, spotColor);
            float facing = Math.max(0, shading.facing(pose, x, y, z));
            color = ARGB.scaleRGB(color, 0.55f + 0.45f * facing);
            vertex(pose, v, x * radius, y * radius, z * radius, x, y, z, color);
        });
    }

    /// A sphere with a single color (used for the glow layers).
    private static void glowSphere(PoseStack.Pose pose, VertexConsumer v, float radius, int color) {
        forEachSphereQuad((x, y, z) -> vertex(pose, v, x * radius, y * radius, z * radius, x, y, z, color));
    }

    /// A value from 0 to 1 that changes slowly over the surface and over time: the "spots" of the star.
    /// It's just a few sine waves mixed together. Change the numbers to get bigger/smaller/faster spots.
    private static float spotPattern(float x, float y, float z, float time) {
        float a = Mth.sin(x * 4 + time * 0.020f) * Mth.sin(y * 5 - time * 0.015f);
        float b = Mth.sin(z * 6 + y * 3 + time * 0.010f);
        return Mth.clamp(0.4f + 0.35f * a + 0.25f * b, 0, 1);
    }

    /// Something that receives one point of the sphere (x, y, z are on a sphere of radius 1).
    private interface SpherePoint {
        void accept(float x, float y, float z);
    }

    /// Walks over the sphere in small squares (latitude x longitude) and gives the 4 corners of each square.
    private static void forEachSphereQuad(SpherePoint point) {
        for (int lat = 0; lat < SPHERE_DETAIL; lat++) {
            float t1 = Mth.PI * lat / SPHERE_DETAIL;
            float t2 = Mth.PI * (lat + 1) / SPHERE_DETAIL;
            for (int lon = 0; lon < SPHERE_DETAIL; lon++) {
                float p1 = Mth.TWO_PI * lon / SPHERE_DETAIL;
                float p2 = Mth.TWO_PI * (lon + 1) / SPHERE_DETAIL;
                spherePoint(point, t1, p1);
                spherePoint(point, t1, p2);
                spherePoint(point, t2, p2);
                spherePoint(point, t2, p1);
            }
        }
    }

    private static void spherePoint(SpherePoint point, float theta, float phi) {
        point.accept(Mth.sin(theta) * Mth.cos(phi), Mth.cos(theta), Mth.sin(theta) * Mth.sin(phi));
    }

    /// A flat ring (visible from both sides) that goes from the inner color to the outer color.
    private static void ring(PoseStack.Pose pose, VertexConsumer v, float inner, float outer, int innerColor, int outerColor) {
        int pieces = SPHERE_DETAIL * 2;
        for (int i = 0; i < pieces; i++) {
            float a1 = Mth.TWO_PI * i / pieces;
            float a2 = Mth.TWO_PI * (i + 1) / pieces;
            // top side
            vertex(pose, v, Mth.cos(a1) * inner, 0, Mth.sin(a1) * inner, 0, 1, 0, innerColor);
            vertex(pose, v, Mth.cos(a1) * outer, 0, Mth.sin(a1) * outer, 0, 1, 0, outerColor);
            vertex(pose, v, Mth.cos(a2) * outer, 0, Mth.sin(a2) * outer, 0, 1, 0, outerColor);
            vertex(pose, v, Mth.cos(a2) * inner, 0, Mth.sin(a2) * inner, 0, 1, 0, innerColor);
            // bottom side (same points in reverse order)
            vertex(pose, v, Mth.cos(a2) * inner, 0, Mth.sin(a2) * inner, 0, -1, 0, innerColor);
            vertex(pose, v, Mth.cos(a2) * outer, 0, Mth.sin(a2) * outer, 0, -1, 0, outerColor);
            vertex(pose, v, Mth.cos(a1) * outer, 0, Mth.sin(a1) * outer, 0, -1, 0, outerColor);
            vertex(pose, v, Mth.cos(a1) * inner, 0, Mth.sin(a1) * inner, 0, -1, 0, innerColor);
        }
    }

    /// One light beam coming out of a pole: two crossed flat strips that fade out at the tip.
    /// direction = 1 for the top pole, -1 for the bottom pole.
    private static void beam(PoseStack.Pose pose, VertexConsumer v, float radius, float length, int direction, int color) {
        float start = radius * 0.8f * direction;
        float end = (radius + length) * direction;
        float width = radius * 0.25f;
        int base = ARGB.color(180, color);
        int tip = ARGB.color(0, color);

        // strip facing X, then strip facing Z; each one drawn on both sides
        for (int side = 0; side < 2; side++) {
            float dx = side == 0 ? width : 0;
            float dz = side == 0 ? 0 : width;
            vertex(pose, v, -dx, start, -dz, 0, 0, 1, base);
            vertex(pose, v, dx, start, dz, 0, 0, 1, base);
            vertex(pose, v, dx * 0.2f, end, dz * 0.2f, 0, 0, 1, tip);
            vertex(pose, v, -dx * 0.2f, end, -dz * 0.2f, 0, 0, 1, tip);

            vertex(pose, v, -dx * 0.2f, end, -dz * 0.2f, 0, 0, -1, tip);
            vertex(pose, v, dx * 0.2f, end, dz * 0.2f, 0, 0, -1, tip);
            vertex(pose, v, dx, start, dz, 0, 0, -1, base);
            vertex(pose, v, -dx, start, -dz, 0, 0, -1, base);
        }
    }

    /// Adds one corner (vertex) to the shape being drawn.
    private static void vertex(PoseStack.Pose pose, VertexConsumer v, float x, float y, float z,
                               float nx, float ny, float nz, int color) {
        v.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, nx, ny, nz);
    }
}
