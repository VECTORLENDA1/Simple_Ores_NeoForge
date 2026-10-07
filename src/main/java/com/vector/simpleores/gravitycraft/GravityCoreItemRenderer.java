package com.vector.simpleores.gravitycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/// Draws the Gravity Core ITEM as a round 3D star (in the hotbar, in the hand and dropped on the ground),
/// instead of a flat picture.
///
/// It is used from the item model files: assets/simpleores/items/gravity_core_<tier>.json
/// ("type": "simpleores:gravity_core"). Size, angle and position of the item in each place
/// (hotbar, hand, ground...) come from the "display" part of models/item/gravity_core_<tier>.json.
public class GravityCoreItemRenderer implements NoDataSpecialModelRenderer {
    /// Size of the sphere of the item (0.5 = one block wide).
    private static final float ITEM_RADIUS = 0.6f;
    /// Smaller sphere for tiers with an accretion disk, so the disk also fits in the slot.
    private static final float ITEM_RADIUS_WITH_DISK = 0.35f;

    private final CoreTier tier;

    public GravityCoreItemRenderer(CoreTier tier) {
        this.tier = tier;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        // Items have no world time, so the animation uses the real clock (in ticks: 1 tick = 50 ms)
        float time = (Util.getMillis() % 1_000_000L) / 50f;
        float radius = ARGB.alpha(tier.diskColor) > 0 ? ITEM_RADIUS_WITH_DISK : ITEM_RADIUS;

        // Brighter where the sphere faces the screen, darker at the edges, so it looks round
        CoreDrawing.Shading shading = (p, x, y, z) -> p.normal().transform(new Vector3f(x, y, z)).z();

        // No light beams on the item (they would be much bigger than the slot)
        CoreDrawing.drawStar(pose, collector, radius, time, tier.coreColor, tier.spotColor, tier.glowColor,
                tier.diskColor, tier.beamColor, 0, shading);
    }

    /// The space the item takes up (a box around the sphere).
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

    /// The part that is read from the item model file: { "type": "simpleores:gravity_core", "tier": "sun" }
    public record Unbaked(CoreTier tier) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                CoreTier.CODEC.fieldOf("tier").forGetter(Unbaked::tier)
        ).apply(inst, Unbaked::new));

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new GravityCoreItemRenderer(tier);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
