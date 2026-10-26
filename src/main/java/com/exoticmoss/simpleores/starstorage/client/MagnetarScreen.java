package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.item.ModItems;
import com.exoticmoss.simpleores.starstorage.MagnetarBlockEntity;
import com.exoticmoss.simpleores.starstorage.MagnetarMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

/// The screen of the Magnetar Core: its upgrade slots (speed, range, and the Wormhole Links) and the inventory.
/// An empty upgrade slot tells (tooltip) what goes there.
public class MagnetarScreen extends AbstractContainerScreen<MagnetarMenu> {
    private static final int PANEL = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF8060C0;
    private static final int SLOT_COLOR = 0xFF2A2A38;
    private static final int SPEED_COLOR = 0xFFE0A030;
    private static final int RANGE_COLOR = 0xFFC060E0;
    private static final int LINK_COLOR = 0xFF40B0E0;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;

    public MagnetarScreen(MagnetarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MagnetarMenu.WIDTH, MagnetarMenu.HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, PANEL_BORDER);
        graphics.centeredText(font, title, leftPos + imageWidth / 2, topPos + 7, TEXT);
        graphics.text(font, playerInventoryTitle, leftPos + 8, topPos + MagnetarMenu.INVENTORY_TOP - 11, TEXT_DIM);

        Slot hoveredEmpty = null;
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, SLOT_COLOR);
            if (slot.index < MagnetarBlockEntity.SLOTS) {
                graphics.outline(x - 1, y - 1, 18, 18, color(slot.index));
                if (!slot.hasItem() && mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) hoveredEmpty = slot;
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);

        if (hoveredEmpty != null && menu.getCarried().isEmpty()) {
            Item item = hoveredEmpty.index == MagnetarBlockEntity.SPEED_SLOT ? ModItems.SOLAR_WIND_UPGRADE.get()
                    : hoveredEmpty.index == MagnetarBlockEntity.RANGE_SLOT ? ModItems.MAGNETIC_FIELD_UPGRADE.get() : ModItems.WORMHOLE_LINK.get();
            graphics.setTooltipForNextFrame(font, List.of(item.getDefaultInstance().getHoverName()), Optional.empty(), mouseX, mouseY);
        }
    }

    private static int color(int slot) {
        if (slot == MagnetarBlockEntity.SPEED_SLOT) return SPEED_COLOR;
        if (slot == MagnetarBlockEntity.RANGE_SLOT) return RANGE_COLOR;
        return LINK_COLOR;
    }

    /// The title and "Inventory" are drawn in extractRenderState.
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }
}
