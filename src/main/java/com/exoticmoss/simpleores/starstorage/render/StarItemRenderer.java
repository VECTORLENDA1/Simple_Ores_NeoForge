package com.exoticmoss.simpleores.starstorage.render;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/// Draws the ITEM of a star of the star storage (Magnetar Core, Stellar Terminal) as a round 3D star.
/// Used from the item model files: { "type": "simpleores:storage_star", "star": "magnetar" }
/// The Star Cells use CellItemRenderer (the same small star seen inside the Stellar Drive).
public class StarItemRenderer implements NoDataSpecialModelRenderer {
    private final StarLook look;

    public StarItemRenderer(StarLook look) {
        this.look = look;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        float time = (Util.getMillis() % 1_000_000L) / 50f;
        CoreDrawing.Shading shading = (p, x, y, z) -> p.normal().transform(new Vector3f(x, y, z)).z();
        // No beams on the item (they would be much bigger than the slot)
        StarDrawing.draw(look, pose, collector, look.itemRadius, time * 1.5f, 0, shading);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float r = look.itemRadius;
        for (float x : new float[]{-r, r}) {
            for (float y : new float[]{-r, r}) {
                for (float z : new float[]{-r, r}) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    public record Unbaked(StarLook look) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                StarLook.CODEC.fieldOf("star").forGetter(Unbaked::look)
        ).apply(inst, Unbaked::new));

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new StarItemRenderer(look);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
