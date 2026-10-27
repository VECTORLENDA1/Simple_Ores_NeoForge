package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.item.ModItems;
import com.exoticmoss.simpleores.starstorage.MagnetarBlockEntity;
import com.exoticmoss.simpleores.starstorage.MagnetarInfo;
import com.exoticmoss.simpleores.starstorage.MagnetarMenu;
import com.exoticmoss.simpleores.starstorage.StarFluids;
import com.exoticmoss.simpleores.starstorage.StarNetwork;
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
        drawInfo(graphics);

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

    /// How the network is (sent by the server every second).
    private void drawInfo(GuiGraphicsExtractor graphics) {
        MagnetarInfo info = MagnetarInfo.last(menu.pos);
        if (info == null) return;
        int x = leftPos + 8, y = topPos + MagnetarMenu.INFO_TOP;
        graphics.text(font, Component.translatable("screen.simpleores.magnetar.field", info.radius()), x, y, TEXT);
        graphics.text(font, Component.translatable("screen.simpleores.magnetar.drives", info.drives(), info.cells()), x, y + 11, TEXT_DIM);
        drawBar(graphics, x, y + 23, Component.translatable("screen.simpleores.magnetar.items"),
                StarNetwork.format(info.used()) + " / " + StarNetwork.format(info.capacity()), info.used(), info.capacity(), 0xFF8060C0);
        drawBar(graphics, x, y + 40, Component.translatable("screen.simpleores.magnetar.fluids"),
                StarFluids.buckets(info.fluidUsed()) + " / " + StarFluids.buckets(info.fluidCapacity()), info.fluidUsed(), info.fluidCapacity(), 0xFF3FA0E0);
    }

    /// A name, the amounts on the right, and a bar under them.
    private void drawBar(GuiGraphicsExtractor graphics, int x, int y, Component name, String amounts, long used, long capacity, int color) {
        int width = imageWidth - 16;
        graphics.text(font, name, x, y, TEXT_DIM);
        graphics.text(font, Component.literal(amounts), x + width - font.width(amounts), y, TEXT);
        graphics.fill(x, y + 10, x + width, y + 14, SLOT_COLOR);
        double full = capacity <= 0 ? 0 : Math.min(1, used / (double) capacity);
        int fill = (int) Math.round(width * full);
        if (fill > 0) graphics.fill(x, y + 10, x + fill, y + 14, color);
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
