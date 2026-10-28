package com.exoticmoss.simpleores.util.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Optional;

/// Draws on the screen in the order of the calls (like a painter): what is drawn later is on top, also on top of the
/// items (the items are 3D and drawn in front of the rest, so the depth is cleared before drawing over them).
/// The tooltip is kept and drawn at the end (flush), on top of everything.
public final class GuiGraphicsExtractor {
    private final GuiGraphics graphics;
    private boolean itemsDrawn;
    private Font tooltipFont;
    private List<Component> tooltip;
    private Optional<TooltipComponent> tooltipImage = Optional.empty();
    private int tooltipX, tooltipY;

    public GuiGraphicsExtractor(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public GuiGraphics graphics() {
        return graphics;
    }

    /// Before drawing flat things over items: the items drawn before stay behind.
    private void flat() {
        if (!itemsDrawn) return;
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        itemsDrawn = false;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        flat();
        graphics.fill(x1, y1, x2, y2, color);
    }

    public void outline(int x, int y, int width, int height, int color) {
        flat();
        graphics.renderOutline(x, y, width, height, color);
    }

    public void text(Font font, Component text, int x, int y, int color) {
        flat();
        graphics.drawString(font, text, x, y, color);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        flat();
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, String text, int x, int y, int color) {
        flat();
        graphics.drawString(font, text, x, y, color);
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        flat();
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        flat();
        graphics.drawString(font, text, x, y, color);
    }

    public void centeredText(Font font, Component text, int x, int y, int color) {
        flat();
        graphics.drawCenteredString(font, text, x, y, color);
    }

    public void centeredText(Font font, String text, int x, int y, int color) {
        flat();
        graphics.drawCenteredString(font, text, x, y, color);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        itemsDrawn = true;
    }

    /// After something else drew items (the slots of the screen).
    public void afterItems() {
        itemsDrawn = true;
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        graphics.enableScissor(x1, y1, x2, y2);
    }

    public void disableScissor() {
        graphics.disableScissor();
    }

    public void setTooltipForNextFrame(Font font, List<Component> lines, Optional<TooltipComponent> image, int x, int y) {
        tooltipFont = font;
        tooltip = lines;
        tooltipImage = image;
        tooltipX = x;
        tooltipY = y;
    }

    public void setTooltipForNextFrame(Font font, Component line, int x, int y) {
        setTooltipForNextFrame(font, List.of(line), Optional.empty(), x, y);
    }

    /// Draws the kept tooltip (call at the end of the frame).
    public void flush() {
        if (tooltip != null && !tooltip.isEmpty()) graphics.renderTooltip(tooltipFont, tooltip, tooltipImage, tooltipX, tooltipY);
        tooltip = null;
    }
}
