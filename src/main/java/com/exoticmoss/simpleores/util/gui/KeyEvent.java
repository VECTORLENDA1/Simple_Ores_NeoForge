package com.exoticmoss.simpleores.util.gui;

import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/// A key pressed on the keyboard.
public record KeyEvent(int key, int scancode, int modifiers) {
    public boolean isEscape() {
        return key == GLFW.GLFW_KEY_ESCAPE;
    }

    public boolean isConfirmation() {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public boolean hasControlDown() {
        return Screen.hasControlDown();
    }
}
