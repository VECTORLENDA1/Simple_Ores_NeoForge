package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/// How the stars of the star storage look (the Magnetar Core and the Stellar Terminal).
/// They are drawn like the Gravity Cores (see CoreDrawing). To change how one looks, just change the numbers here.
///
/// Colors use the format 0xAARRGGBB (0x00000000 = no beams).
public enum StarLook implements StringRepresentable {
    //         id            core color   spot color   glow color   beam color   radius  beam length  item radius
    /// The Magnetar: a small, very bright violet star with two magnetic beams spinning around it.
    MAGNETAR(  "magnetar",   0xFFEAF2FF,  0xFF8A6CFF,  0xFFA070FF,  0xFFC9A6FF,  0.36f,  2.5f,        0.5f),
    /// The Stellar Terminal: a calm teal star, without beams.
    TERMINAL(  "terminal",   0xFFDAFFF6,  0xFF2FC79E,  0xFF46E8C4,  0x00000000,  0.30f,  0f,          0.5f);

    public static final Codec<StarLook> CODEC = StringRepresentable.fromEnum(StarLook::values);

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

    @Override
    public String getSerializedName() {
        return id;
    }
}
