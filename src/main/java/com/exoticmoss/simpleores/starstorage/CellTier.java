package com.exoticmoss.simpleores.starstorage;

/// The 3 kinds of Star Cells (they keep the items of the Magnetar network).
/// Named after dead stars, from the least to the most compressed matter.
/// A cell keeps ANY number of different kinds of items: only the total amount of items is limited.
/// Inside a Stellar Drive each cell is drawn as a small star with these colors (see StellarDriveRenderer).
///
/// The Fluid Cells (they keep liquids) use the same tiers, with their own capacity (in mB, 1000 mB = 1 bucket)
/// and their own colors (blue, like water).
///
/// Colors use the format 0xAARRGGBB (0x00000000 = no disk).
public enum CellTier {
    //             capacity (items)     core color   spot color   glow color   disk color
    WHITE_DWARF(   16_384,              0xFFF4F8FF,  0xFFB8CCFF,  0xFFDDE8FF,  0x00000000,
    //             fluid capacity (mB)  fluid colors: core, spot, glow, disk
                   1_024_000L,          0xFFD8F4FF,  0xFF3FA0E0,  0xFF80D0FF,  0x00000000),
    NEUTRON(       1_048_576,           0xFFC8F6FF,  0xFF3FA8FF,  0xFF60D0FF,  0x00000000,
                   65_536_000L,         0xFF9CFFF0,  0xFF10A090,  0xFF40E0C8,  0x00000000),
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
