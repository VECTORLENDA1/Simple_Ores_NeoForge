package com.exoticmoss.simpleores.util.render;

import net.minecraft.util.Mth;

/// Colors as 0xAARRGGBB numbers (the helpers later versions of Minecraft have in net.minecraft.util.ARGB).
public final class ARGB {
    private ARGB() {
    }

    public static int alpha(int color) {
        return color >>> 24;
    }

    public static int red(int color) {
        return color >> 16 & 0xFF;
    }

    public static int green(int color) {
        return color >> 8 & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int color(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    /// The color with another transparency.
    public static int color(int alpha, int color) {
        return (alpha & 0xFF) << 24 | color & 0xFFFFFF;
    }

    /// Between two colors (0 = the first, 1 = the second).
    public static int srgbLerp(float delta, int from, int to) {
        return color(Mth.lerpInt(delta, alpha(from), alpha(to)), Mth.lerpInt(delta, red(from), red(to)),
                Mth.lerpInt(delta, green(from), green(to)), Mth.lerpInt(delta, blue(from), blue(to)));
    }

    /// The color darker (scale < 1) or brighter, the same transparency.
    public static int scaleRGB(int color, float scale) {
        return color(alpha(color), Mth.clamp((int) (red(color) * scale), 0, 255), Mth.clamp((int) (green(color) * scale), 0, 255),
                Mth.clamp((int) (blue(color) * scale), 0, 255));
    }
}
