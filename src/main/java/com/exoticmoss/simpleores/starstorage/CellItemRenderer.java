package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Locale;
import java.util.function.Consumer;

/// Draws a Star Cell ITEM as the same small star seen inside the Stellar Drive (colors from CellTier).
/// Used from the item model files: { "type": "simpleores:star_cell", "tier": "neutron" }
public class CellItemRenderer implements NoDataSpecialModelRenderer {
    private static final float RADIUS = 0.42f;
    /// Smaller star for the cells with a disk (the Singularity), so the disk also fits in the slot.
    private static final float RADIUS_WITH_DISK = 0.28f;

    private final CellTier tier;

    public CellItemRenderer(CellTier tier) {
        this.tier = tier;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        float time = (Util.getMillis() % 1_000_000L) / 50f;
        float radius = ARGB.alpha(tier.diskColor) > 0 ? RADIUS_WITH_DISK : RADIUS;
        CoreDrawing.Shading shading = (p, x, y, z) -> p.normal().transform(new Vector3f(x, y, z)).z();
        CoreDrawing.drawStar(pose, collector, radius, time, tier.coreColor, tier.spotColor, tier.glowColor,
                tier.diskColor, 0x00000000, 0, shading);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (float x : new float[]{-RADIUS, RADIUS}) {
            for (float y : new float[]{-RADIUS, RADIUS}) {
                for (float z : new float[]{-RADIUS, RADIUS}) {
                    output.accept(new Vector3f(x, y, z));
                }
            }
        }
    }

    /// The tier read from the item model file ("white_dwarf", "neutron" or "singularity").
    private static final Codec<CellTier> TIER_CODEC = Codec.STRING.xmap(
            name -> CellTier.valueOf(name.toUpperCase(Locale.ROOT)), tier -> tier.name().toLowerCase(Locale.ROOT));

    public record Unbaked(CellTier tier) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                TIER_CODEC.fieldOf("tier").forGetter(Unbaked::tier)
        ).apply(inst, Unbaked::new));

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new CellItemRenderer(tier);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
