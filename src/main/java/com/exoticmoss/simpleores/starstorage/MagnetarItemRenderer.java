package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/// Draws the Magnetar Core ITEM as a round 3D star (like the Gravity Cores).
/// Used from assets/simpleores/items/magnetar_core.json ("type": "simpleores:magnetar").
public class MagnetarItemRenderer implements NoDataSpecialModelRenderer {
    private static final float ITEM_RADIUS = 0.5f;

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        float time = (Util.getMillis() % 1_000_000L) / 50f;
        CoreDrawing.Shading shading = (p, x, y, z) -> p.normal().transform(new Vector3f(x, y, z)).z();
        // No beams on the item (they would be much bigger than the slot)
        CoreDrawing.drawStar(pose, collector, ITEM_RADIUS, time * 1.5f, MagnetarRenderer.CORE_COLOR, MagnetarRenderer.SPOT_COLOR,
                MagnetarRenderer.GLOW_COLOR, 0x00000000, MagnetarRenderer.BEAM_COLOR, 0, shading);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (float x : new float[]{-ITEM_RADIUS, ITEM_RADIUS}) {
            for (float y : new float[]{-ITEM_RADIUS, ITEM_RADIUS}) {
                for (float z : new float[]{-ITEM_RADIUS, ITEM_RADIUS}) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    /// The part read from the item model file: { "type": "simpleores:magnetar" }
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new MagnetarItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
