package com.vector.simpleores.screen.custom;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class AtomicCraftingTableScreen extends AbstractContainerScreen<AtomicCraftingTableMenu> {
    public static final Identifier GUI_TEXTURE =
            Identifier.fromNamespaceAndPath("simpleores","textures/gui/atomic_crafting_table/atomic_crafting_table_gui.png");

    public AtomicCraftingTableScreen(AtomicCraftingTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        // imageWidth/imageHeight agora sao final e passam-se no construtor
        super(pMenu, pPlayerInventory, pTitle, 249, 279);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.extractBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // A textura desta GUI e 512x512, por isso indicamos o tamanho real da textura
        pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 512, 512);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor pGuiGraphics, int pMouseX, int pMouseY) {
        // Player inventory label (bottom left)
        int invX = 44;
        int invY = 185;
        // As cores de texto agora sao ARGB: sem o 0xFF de alpha o texto fica invisivel
        pGuiGraphics.text(font, "Inventory", invX, invY, 0xFF404040, false);

        // Screen title label (top left), drawn the same way as the inventory label
        int titleX = 17;
        int titleY = 6;
        pGuiGraphics.text(font, title, titleX, titleY, 0xFF404040, false);
    }
}
