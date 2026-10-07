package com.vector.simpleores.screen.custom;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SimpleCraftingTableScreen extends AbstractContainerScreen<SimpleCraftingTableMenu> {
    public static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("simpleores","textures/gui/simple_crafting_table/simple_crafting_table_gui.png");

    public SimpleCraftingTableScreen(SimpleCraftingTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        // imageWidth/imageHeight are now final and are passed in the constructor
        super(pMenu, pPlayerInventory, pTitle, 176, 206);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.extractBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY) {
        int x = 7;
        int y = 111;
        // Text colors are now ARGB: without the 0xFF alpha the text is invisible
        pGuiGraphics.text(font, "Inventory", x, y, 0xFF404040, false);

        pGuiGraphics.text(font, title, (imageWidth - font.width(title)) - 54, 6, 0xFF404040, false);
    }
}
