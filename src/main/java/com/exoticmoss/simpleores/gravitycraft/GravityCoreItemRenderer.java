package com.exoticmoss.simpleores.gravitycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.exoticmoss.simpleores.util.render.SubmitNodeCollector;
import com.exoticmoss.simpleores.util.render.ARGB;
import net.minecraft.Util;
import org.joml.Vector3f;


/// Draws the Gravity Core ITEM as a round 3D star (in the hotbar, in the hand and dropped on the ground),
/// instead of a flat picture.
///
/// It is used by SpecialItemRenderer (the item models are "builtin/entity"). Size, angle and position of the item
/// in each place (hotbar, hand, ground...) come from the "display" part of models/item/gravity_core_<tier>.json.
public class GravityCoreItemRenderer {
    /// Size of the sphere of the item (0.5 = one block wide).
    private static final float ITEM_RADIUS = 0.6f;
    /// Smaller sphere for tiers with an accretion disk, so the disk also fits in the slot.
    private static final float ITEM_RADIUS_WITH_DISK = 0.35f;

    private final CoreTier tier;

    public GravityCoreItemRenderer(CoreTier tier) {
        this.tier = tier;
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        /// Items have no world time, so the animation uses the real clock (in ticks: 1 tick = 50 ms)
        float time = (Util.getMillis() % 1_000_000L) / 50f;
        float radius = ARGB.alpha(tier.diskColor) > 0 ? ITEM_RADIUS_WITH_DISK : ITEM_RADIUS;

        /// Brighter where the sphere faces the screen, darker at the edges, so it looks round
        CoreDrawing.Shading shading = (p, x, y, z) -> p.normal().transform(new Vector3f(x, y, z)).z();

        /// No light beams on the item (they would be much bigger than the slot)
        CoreDrawing.drawStar(pose, collector, radius, time, tier.coreColor, tier.spotColor, tier.glowColor,
                tier.diskColor, tier.beamColor, 0, shading);
    }
}
