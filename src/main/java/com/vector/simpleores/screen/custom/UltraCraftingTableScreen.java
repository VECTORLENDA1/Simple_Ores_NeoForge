package com.vector.simpleores.screen.custom;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class UltraCraftingTableScreen extends AbstractContainerScreen<UltraCraftingTableMenu> {
    public static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("simpleores","textures/gui/ultra_crafting_table/ultra_crafting_table_gui.png");

    public UltraCraftingTableScreen(UltraCraftingTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        // imageWidth/imageHeight agora sao final e passam-se no construtor
        super(pMenu, pPlayerInventory, pTitle, 212, 242);
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
        // Player inventory label (bottom left)
        int invX = 27;
        int invY = 145;
        // As cores de texto agora sao ARGB: sem o 0xFF de alpha o texto fica invisivel
        pGuiGraphics.text(font, "Inventory", invX, invY, 0xFF404040, false);

        // Screen title label (top left), drawn the same way as the inventory label
        int titleX = 17;
        int titleY = 6;
        pGuiGraphics.text(font, title, titleX, titleY, 0xFF404040, false);
    }
}
