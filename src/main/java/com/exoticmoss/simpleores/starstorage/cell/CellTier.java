package com.exoticmoss.simpleores.starstorage.cell;

/// The 8 kinds of Star Cells (they keep the items of the Magnetar network). Each one keeps 4 times more than the
/// one before it, and is made from it (the items inside go to the new cell); the last one has no limit.
/// A cell keeps ANY number of different kinds of items: only the total amount of items is limited.
/// Inside a Stellar Drive each cell is drawn as a small star with these colors (see StellarDriveRenderer);
/// the Singularity is a small black hole.
///
/// The Fluid Cells (they keep liquids and gases) use the same tiers, with their own capacity (in mB, 1000 mB =
/// 1 bucket) and their own colors: a light blue star, like water, with the spots of the color of its tier.
///
/// Colors use the format 0xAARRGGBB (0x00000000 = no disk).
public enum CellTier {
    //             capacity (items)     core color   spot color   glow color   disk color
    RED_DWARF(     4_096,              0xFFFFC8B8,  0xFFE04030,  0xFFFF7050,  0x00000000,
    //             fluid capacity (mB)  fluid colors: core, spot, glow, disk
                   65_536L,             0xFFD8F4FF,  0xFFE04030,  0xFFB09CA8,  0x00000000),
    WHITE_DWARF(   16_384,              0xFFF4F8FF,  0xFFB8CCFF,  0xFFDDE8FF,  0x00000000,
                   262_144L,            0xFFD8F4FF,  0xFFB8CCFF,  0xFF9ED8FF,  0x00000000),
    BLUE_DWARF(    65_536,              0xFFD0E0FF,  0xFF4060FF,  0xFF7090FF,  0x00000000,
                   1_048_576L,          0xFFD8F4FF,  0xFF4060FF,  0xFF68ACFF,  0x00000000),
    PULSAR(        262_144,             0xFFF6FFD8,  0xFFA8D820,  0xFFD0F060,  0x00000000,
                   4_194_304L,          0xFFD8F4FF,  0xFFA8D820,  0xFF98DCB0,  0x00000000),
    NEUTRON(       1_048_576,          0xFFFFE0F0,  0xFFE0408A,  0xFFFF70B0,  0x00000000,
                   16_777_216L,         0xFFD8F4FF,  0xFFE0408A,  0xFFB09CD8,  0x00000000),
    QUARK(         4_194_304,          0xFFE0FFE8,  0xFF20B060,  0xFF60E890,  0x00000000,
                   67_108_864L,         0xFFD8F4FF,  0xFF20B060,  0xFF60D8C8,  0x00000000),
    PREON(         16_777_216,         0xFFFFE8D0,  0xFFB85A20,  0xFFE09050,  0x00000000,
                   268_435_456L,        0xFFD8F4FF,  0xFFB85A20,  0xFFA0ACA8,  0x00000000),
    SINGULARITY(   Integer.MAX_VALUE,   0xFF000000,  0xFF0A0410,  0xFF6A2AB0,  0xFFB070FF,
                   Long.MAX_VALUE / 2,  0xFF000000,  0xFF020C18,  0xFF2A6AB0,  0xFF50C8FF);

    /// How many items fit in the cell (all kinds together).
    public final int capacity;
    public final int coreColor;
    public final int spotColor;
    public final int glowColor;
    public final int diskColor;
    /// How many mB fit in a Fluid Cell of this tier (all liquids together).
    public final long fluidCapacity;
    public final int fluidCoreColor;
    public final int fluidSpotColor;
    public final int fluidGlowColor;
    public final int fluidDiskColor;

    CellTier(int capacity, int coreColor, int spotColor, int glowColor, int diskColor,
             long fluidCapacity, int fluidCoreColor, int fluidSpotColor, int fluidGlowColor, int fluidDiskColor) {
        this.capacity = capacity;
        this.coreColor = coreColor;
        this.spotColor = spotColor;
        this.glowColor = glowColor;
        this.diskColor = diskColor;
        this.fluidCapacity = fluidCapacity;
        this.fluidCoreColor = fluidCoreColor;
        this.fluidSpotColor = fluidSpotColor;
        this.fluidGlowColor = fluidGlowColor;
        this.fluidDiskColor = fluidDiskColor;
    }
}
