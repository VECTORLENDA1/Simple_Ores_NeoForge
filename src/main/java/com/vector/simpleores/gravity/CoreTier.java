package com.vector.simpleores.gravity;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/// The 4 tiers (levels) of the Gravity Core.
/// To change how a tier looks or behaves, just change the numbers here.
///
/// Colors use the format 0xAARRGGBB:
///   AA = alpha (FF = solid, 00 = invisible), RR = red, GG = green, BB = blue.
///   Use 0x00000000 to turn a feature off (e.g. a tier without an accretion disk).
public enum CoreTier implements StringRepresentable {
    //          id                level       core color           spot color           glow color            disk color           beam color           size           orbit radius    orbit speed    max items   light
    SUN(        "sun",        1,     0xFFFFE27A, 0xFFFF8C1A, 0xFFFFA238, 0x00000000, 0x00000000, 1.5f, 1.7f, 0.8f, 6, 15),

    RED_GIANT(  "red_giant",  2,     0xFFE8501E, 0xFF8E1606, 0xFFFF5A1F, 0x00000000, 0x00000000, 1.8f, 1.9f, 0.5f, 9, 12),

    PULSAR(     "pulsar",     3,     0xFFF5FBFF, 0xFF8FCBFF, 0xFF3D8BFF, 0x00000000, 0xFFCDEBFF, 2.1f, 2.1f, 2.0f, 12, 15),

    BLACK_HOLE( "black_hole", 4,     0xFF000000, 0xFF050208, 0xFF1B0B2E, 0xFFFFB04A, 0x00000000, 2.5f, 2.4f, 1.0f, 16, 4);

    /// Reads/writes a tier by its id (used to save it and to send it to the client).
    public static final Codec<CoreTier> CODEC = StringRepresentable.fromEnum(CoreTier::values);

    /// Name used in the block IDs (e.g. "gravity_core_sun").
    public final String id;
    /// Tier level. A recipe with "tier": 2 only works in cores of level 2 or higher.
    public final int level;
    /// Main color of the sphere.
    public final int coreColor;
    /// Second color of the sphere, used for the moving "spots" on its surface.
    public final int spotColor;
    /// Color of the transparent glow (corona) around the sphere.
    public final int glowColor;
    /// Color of the accretion disk (the flat glowing ring). 0x00000000 = no disk.
    public final int diskColor;
    /// Color of the light beams coming out of the poles. 0x00000000 = no beams.
    public final int beamColor;
    /// Diameter of the sphere in blocks (1.0 = one full block).
    public final float coreSize;
    /// Distance (in blocks) from the center of the sphere to the orbiting items.
    public final float orbitRadius;
    /// Orbit speed (1.0 = normal, 2.0 = twice as fast).
    public final float orbitSpeed;
    /// How many different item stacks can orbit at the same time.
    public final int maxItems;
    /// Light emitted by the block (0 to 15).
    public final int light;

    CoreTier(String id, int level, int coreColor, int spotColor, int glowColor, int diskColor, int beamColor,
             float coreSize, float orbitRadius, float orbitSpeed, int maxItems, int light) {
        this.id = id;
        this.level = level;
        this.coreColor = coreColor;
        this.spotColor = spotColor;
        this.glowColor = glowColor;
        this.diskColor = diskColor;
        this.beamColor = beamColor;
        this.coreSize = coreSize;
        this.orbitRadius = orbitRadius;
        this.orbitSpeed = orbitSpeed;
        this.maxItems = maxItems;
        this.light = light;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
