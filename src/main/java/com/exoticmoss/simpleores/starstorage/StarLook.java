package com.exoticmoss.simpleores.starstorage;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/// How the stars of the star storage look (Magnetar Core, Stellar Terminal, Binary Star, Quasar).
/// They are drawn like the Gravity Cores (see CoreDrawing and StarDrawing). To change how one looks, just change the numbers here.
///
/// Colors use the format 0xAARRGGBB (0x00000000 = no beams / no disk).
public enum StarLook implements StringRepresentable {
    //         id            core color   spot color   glow color   beam color   disk color   radius  beam length  item radius
    /// The Magnetar: a small, very bright violet star with two magnetic beams spinning around it.
    MAGNETAR(  "magnetar",   0xFFEAF2FF,  0xFF8A6CFF,  0xFFA070FF,  0xFFC9A6FF,  0x00000000,  0.35f,  1.2f,        0.5f),
    /// The Stellar Terminal: a calm teal star, without beams.
    TERMINAL(  "terminal",   0xFFDAFFF6,  0xFF2FC79E,  0xFF46E8C4,  0x00000000,  0x00000000,  0.35f,  0f,          0.5f),
    /// The Stellar Terminal level 2: a golden star.
    TERMINAL_2("terminal_2", 0xFFFFF6D8,  0xFFFFB020,  0xFFFFC850,  0x00000000,  0x00000000,  0.35f,  0f,          0.5f),
    /// The Binary Star: two small stars (an orange one and a blue one, see StarDrawing.COMPANION_*) turning
    /// around each other, giving matter to each other: it moves the items in and out of the blocks around it.
    BINARY(    "binary",     0xFFFFE2B0,  0xFFFF9A3C,  0xFFFFB45A,  0x00000000,  0x00000000,  0.35f,  0f,          0.5f),
    /// The Quasar: the brightest thing of the universe, with its disk and two jets. The brain of the auto-craft.
    QUASAR(    "quasar",     0xFFFFFFFF,  0xFFA8D0FF,  0xFF7FB2FF,  0xFFD8ECFF,  0xFFFFC070,  0.25f,  1.0f,        0.32f);

    public static final Codec<StarLook> CODEC = StringRepresentable.fromEnum(StarLook::values);
    /// Color of the light shell around the star while the player looks at it (instead of the box outline).
    public static final int HIGHLIGHT_COLOR = 0x14FFFFFF;

    public final String id;
    public final int coreColor;
    public final int spotColor;
    public final int glowColor;
    public final int beamColor;
    public final int diskColor;
    /// Radius of the star on the block (0.5 = one block wide) and length of the beams (blocks).
    public final float radius;
    public final float beamLength;
    /// Radius of the star of the item (in the hotbar, in the hand...).
    public final float itemRadius;

    StarLook(String id, int coreColor, int spotColor, int glowColor, int beamColor, int diskColor, float radius, float beamLength, float itemRadius) {
        this.id = id;
        this.coreColor = coreColor;
        this.spotColor = spotColor;
        this.glowColor = glowColor;
        this.beamColor = beamColor;
        this.diskColor = diskColor;
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
