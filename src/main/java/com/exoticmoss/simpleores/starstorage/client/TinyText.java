package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.util.gui.GuiGraphicsExtractor;

/// A very small pixel font (3x5 letters) for the "CRAFT" label of the items that can be crafted,
/// so it fits under the item in a cell of the network.
public final class TinyText {
    /// Each letter: 5 rows of 3 pixels (1 = drawn).
    private static final String[] C = {"111", "100", "100", "100", "111"};
    private static final String[] R = {"110", "101", "110", "101", "101"};
    private static final String[] A = {"010", "101", "111", "101", "101"};
    private static final String[] F = {"111", "100", "110", "100", "100"};
    private static final String[] T = {"111", "010", "010", "010", "010"};
    private static final String[][] CRAFT = {C, R, A, F, T};

    private TinyText() {
    }

    /// Width of "CRAFT" in pixels.
    public static final int CRAFT_WIDTH = CRAFT.length * 4 - 1;

    /// Draws "CRAFT" with its top left corner at x, y.
    public static void craft(GuiGraphicsExtractor graphics, int x, int y, int color) {
        for (int letter = 0; letter < CRAFT.length; letter++) {
            for (int row = 0; row < 5; row++) {
                for (int column = 0; column < 3; column++) {
                    if (CRAFT[letter][row].charAt(column) == '1') {
                        int px = x + letter * 4 + column, py = y + row;
                        graphics.fill(px, py, px + 1, py + 1, color);
                    }
                }
            }
        }
    }
}
