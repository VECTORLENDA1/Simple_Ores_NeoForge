package com.exoticmoss.simpleores.starstorage;

/// The 3 kinds of Star Cells (the "disks" that keep the items of the Magnetar network).
/// Named after dead stars, from the least to the most compressed matter.
/// To change how much a cell keeps, just change the numbers here.
public enum CellTier {
    //             capacity (items)     different items
    WHITE_DWARF(   16_384,              32),
    NEUTRON(       1_048_576,           64),
    /// Only ONE kind of item, but (almost) without limit.
    SINGULARITY(   Integer.MAX_VALUE,   1);

    /// How many items fit in the cell (all kinds together).
    public final int capacity;
    /// How many different kinds of items the cell can keep.
    public final int maxTypes;

    CellTier(int capacity, int maxTypes) {
        this.capacity = capacity;
        this.maxTypes = maxTypes;
    }
}
