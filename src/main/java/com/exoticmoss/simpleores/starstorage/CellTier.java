package com.exoticmoss.simpleores.starstorage;

/// The 3 kinds of Star Cells (they keep the items of the Magnetar network).
/// Named after dead stars, from the least to the most compressed matter.
/// A cell keeps ANY number of different kinds of items: only the total amount of items is limited.
/// Inside a Stellar Drive each cell is drawn as a small star with these colors (see StellarDriveRenderer).
///
/// Colors use the format 0xAARRGGBB (0x00000000 = no disk).
public enum CellTier {
    //             capacity (items)     core color   spot color   glow color   disk color
    WHITE_DWARF(   16_384,              0xFFF4F8FF,  0xFFB8CCFF,  0xFFDDE8FF,  0x00000000),
    NEUTRON(       1_048_576,           0xFFC8F6FF,  0xFF3FA8FF,  0xFF60D0FF,  0x00000000),
    SINGULARITY(   Integer.MAX_VALUE,   0xFF000000,  0xFF0A0410,  0xFF6A2AB0,  0xFFB070FF);

    /// How many items fit in the cell (all kinds together).
    public final int capacity;
    public final int coreColor;
    public final int spotColor;
    public final int glowColor;
    public final int diskColor;

    CellTier(int capacity, int coreColor, int spotColor, int glowColor, int diskColor) {
        this.capacity = capacity;
        this.coreColor = coreColor;
        this.spotColor = spotColor;
        this.glowColor = glowColor;
        this.diskColor = diskColor;
    }
}
