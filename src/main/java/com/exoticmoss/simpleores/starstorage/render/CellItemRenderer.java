package com.exoticmoss.simpleores.starstorage.render;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.exoticmoss.simpleores.util.render.SubmitNodeCollector;
import com.exoticmoss.simpleores.util.render.ARGB;
import net.minecraft.Util;
import org.joml.Vector3f;

import java.util.Locale;
import com.exoticmoss.simpleores.starstorage.cell.CellTier;

/// Draws a Star Cell ITEM as the same small star seen inside the Stellar Drive (colors from CellTier).
/// Used from the item model files: { "type": "simpleores:star_cell", "tier": "neutron" }
/// (Fluid Cells: { "type": "simpleores:star_cell", "tier": "neutron", "fluid": true }: blue colors).
public class CellItemRenderer {
    private static final float RADIUS = 0.42f;
    /// Smaller star for the cells with a disk (the Singularity), so the disk also fits in the slot.
    private static final float RADIUS_WITH_DISK = 0.28f;

    private final CellTier tier;
    /// Fluid Cell: the blue colors of the tier.
    private final boolean fluid;

    public CellItemRenderer(CellTier tier, boolean fluid) {
        this.tier = tier;
        this.fluid = fluid;
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        float time = (Util.getMillis() % 1_000_000L) / 50f;
        int disk = fluid ? tier.fluidDiskColor : tier.diskColor;
        float radius = ARGB.alpha(disk) > 0 ? RADIUS_WITH_DISK : RADIUS;
        CoreDrawing.Shading shading = (p, x, y, z) -> p.normal().transform(new Vector3f(x, y, z)).z();
        if (fluid) {
            CoreDrawing.drawStar(pose, collector, radius, time, tier.fluidCoreColor, tier.fluidSpotColor, tier.fluidGlowColor,
                    disk, 0x00000000, 0, shading);
        } else {
            CoreDrawing.drawStar(pose, collector, radius, time, tier.coreColor, tier.spotColor, tier.glowColor,
                    disk, 0x00000000, 0, shading);
        }
    }
}
