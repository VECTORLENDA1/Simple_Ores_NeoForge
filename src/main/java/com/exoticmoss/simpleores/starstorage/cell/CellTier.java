package com.exoticmoss.simpleores.starstorage.cell;

/// The 8 kinds of Star Cells (they keep the items of the Magnetar network). Each one keeps 4 times more than the
/// one before it, and is made from it (the items inside go to the new cell); the last one has no limit.
/// A cell keeps ANY number of different kinds of items: only the total amount of items is limited.
/// Inside a Stellar Drive each cell is drawn as a small star with these colors (see StellarDriveRenderer);
/// the Singularity is a small black hole.
///
/// The Fluid Cells (they keep liquids) use the same tiers, with their own capacity (in mB, 1000 mB = 1 bucket)
/// and their own colors (blue, like water).
///
/// Colors use the format 0xAARRGGBB (0x00000000 = no disk).
public enum CellTier {
    //             capacity (items)     core color   spot color   glow color   disk color
    RED_DWARF(     4_096,               0xFFFFC8B8,  0xFFE04030,  0xFFFF7050,  0x00000000,
    //             fluid capacity (mB)  fluid colors: core, spot, glow, disk
                   65_536L,             0xFFD0ECFF,  0xFF5090D0,  0xFF80B8F0,  0x00000000),
    WHITE_DWARF(   16_384,              0xFFF4F8FF,  0xFFB8CCFF,  0xFFDDE8FF,  0x00000000,
                   262_144L,            0xFFD8F4FF,  0xFF3FA0E0,  0xFF80D0FF,  0x00000000),
    BLUE_DWARF(    65_536,              0xFFD0E0FF,  0xFF4060FF,  0xFF7090FF,  0x00000000,
                   1_048_576L,          0xFFB8D4FF,  0xFF2050D0,  0xFF5080F0,  0x00000000),
    PULSAR(        262_144,             0xFFF0FFFF,  0xFF50D8E0,  0xFF90FFF0,  0x00000000,
                   4_194_304L,          0xFFC0FFF8,  0xFF20B0C0,  0xFF60E0E8,  0x00000000),
    NEUTRON(       1_048_576,           0xFFC8F6FF,  0xFF3FA8FF,  0xFF60D0FF,  0x00000000,
                   16_777_216L,         0xFF9CFFF0,  0xFF10A090,  0xFF40E0C8,  0x00000000),
    QUARK(         4_194_304,           0xFFFFE0FF,  0xFFC040E0,  0xFFE070FF,  0x00000000,
                   67_108_864L,         0xFFB8F0FF,  0xFF2080B0,  0xFF50B0E0,  0x00000000),
    PREON(         16_777_216,          0xFFFFF4C0,  0xFFFFB020,  0xFFFFD060,  0x00000000,
                   268_435_456L,        0xFFA0FFE0,  0xFF00A070,  0xFF40E0B0,  0x00000000),
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
