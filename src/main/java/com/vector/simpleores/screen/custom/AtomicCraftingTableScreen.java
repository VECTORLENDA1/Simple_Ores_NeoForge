package com.vector.simpleores.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AtomicCraftingTableScreen extends AbstractContainerScreen<AtomicCraftingTableMenu> {
    public static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("simpleores","textures/gui/atomic_crafting_table/atomic_crafting_table_gui.png");

    public AtomicCraftingTableScreen(AtomicCraftingTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 249;
        this.imageHeight = 279;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f,1.0f ,1.0f ,1.0f);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Importante: este overload padrão assume atlas 256x256.
        // Como a sua textura de GUI é 512x512, use o overload que informa o tamanho da textura
        // para evitar recortes/escala errada e garantir que o jogo "reconheça" a imagem inteira.
        pGuiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 512, 512);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        // Player inventory label (bottom left)
        int invX = 44;
        int invY = 185;
        pGuiGraphics.drawString(font, "Inventory", invX, invY, 0x404040, false);

        // Screen title label (top left), drawn the same way as the inventory label
        int titleX = 17;
        int titleY = 6;
        pGuiGraphics.drawString(font, title, titleX, titleY, 0x404040, false);
    }


    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}
