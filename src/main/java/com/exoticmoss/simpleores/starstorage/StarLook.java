package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/// How the stars of the star storage look (the Magnetar Core and the Stellar Terminal).
/// They are drawn like the Gravity Cores (see CoreDrawing). To change how one looks, just change the numbers here.
///
/// Colors use the format 0xAARRGGBB (0x00000000 = no beams).
public enum StarLook implements StringRepresentable {
    //         id            core color   spot color   glow color   beam color   radius  beam length  item radius
    /// The Magnetar: a small, very bright violet star with two magnetic beams spinning around it.
    MAGNETAR(  "magnetar",   0xFFEAF2FF,  0xFF8A6CFF,  0xFFA070FF,  0xFFC9A6FF,  0.5f,   3.0f,        0.5f),
    /// The Stellar Terminal: a calm teal star, without beams.
    TERMINAL(  "terminal",   0xFFDAFFF6,  0xFF2FC79E,  0xFF46E8C4,  0x00000000,  0.5f,   0f,          0.5f);

    public static final Codec<StarLook> CODEC = StringRepresentable.fromEnum(StarLook::values);
    /// Color of the light shell around the star while the player looks at it (instead of the box outline).
    public static final int HIGHLIGHT_COLOR = 0x14FFFFFF;

    public final String id;
    public final int coreColor;
    public final int spotColor;
    public final int glowColor;
    public final int beamColor;
    /// Radius of the star on the block (0.5 = one block wide) and length of the beams (blocks).
    public final float radius;
    public final float beamLength;
    /// Radius of the star of the item (in the hotbar, in the hand...).
    public final float itemRadius;

    StarLook(String id, int coreColor, int spotColor, int glowColor, int beamColor, float radius, float beamLength, float itemRadius) {
        this.id = id;
        this.coreColor = coreColor;
        this.spotColor = spotColor;
        this.glowColor = glowColor;
        this.beamColor = beamColor;
        this.radius = radius;
        this.beamLength = beamLength;
        this.itemRadius = itemRadius;
    }

    /// The part of the block that can be clicked: a box just around the star (blocks can only have boxes,
    /// so it is the smallest box with the whole star inside). The box itself is not drawn: see StarOutline.
    public VoxelShape shape() {
        double from = 16 * (0.5 - radius), to = 16 * (0.5 + radius);
        return Block.box(from, from, from, to, to, to);
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
