package com.exoticmoss.simpleores.util.gui;

import net.minecraft.client.gui.screens.Screen;

/// A click of the mouse: where and which button (0 = left, 1 = right).
public record MouseButtonEvent(double x, double y, int button) {
    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public boolean hasControlDown() {
        return Screen.hasControlDown();
    }
}
