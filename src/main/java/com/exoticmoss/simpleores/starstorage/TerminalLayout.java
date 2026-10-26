package com.exoticmoss.simpleores.starstorage;

/// Where everything is in the Stellar Terminal screen (pixels from the top left corner of the panel).
/// Used by the menu (the slots) and by the screen (the drawing), so they always match.
///  - top: cells (left), search and sort (right)
///  - middle: the items of the network ("rows" rows, fewer in small windows)
///  - bottom: the inventory (left) and the crafting grid (right)
public record TerminalLayout(int rows) {
    public static final int WIDTH = 292;
    /// Network grid: columns and size of one cell.
    public static final int COLUMNS = 10;
    public static final int CELL_W = 26;
    public static final int CELL_H = 28;
    public static final int SLOT = 18;
    public static final int MIN_ROWS = 2;
    public static final int MAX_ROWS = 5;
    /// Top part: cells panel and search.
    public static final int TOP = 20;
    public static final int NETWORK_TOP = 82;
    /// Height of everything except the rows of the network.
    public static final int FIXED_HEIGHT = NETWORK_TOP + 30 + 3 * SLOT + 4 + SLOT + 10;

    public int height() {
        return FIXED_HEIGHT + rows * CELL_H;
    }

    /// Top of the inventory (and of the crafting grid).
    public int inventoryTop() {
        return NETWORK_TOP + rows * CELL_H + 30;
    }

    public static int inventoryLeft() {
        return 10;
    }

    public static int craftLeft() {
        return 180;
    }

    public int resultX() {
        return 254;
    }

    public int resultY() {
        return inventoryTop() + SLOT;
    }
}
