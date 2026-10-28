package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.item.ModItems;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarBlockEntity;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarInfo;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarMenu;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;
import com.exoticmoss.simpleores.util.gui.GuiGraphicsExtractor;
import com.exoticmoss.simpleores.util.gui.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
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
        super(menu, inventory, title);
        this.imageWidth = MagnetarMenu.WIDTH;
        this.imageHeight = MagnetarMenu.HEIGHT;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        NetworkTabs.draw(graphics, font, leftPos, topPos, imageWidth, 0, menu.pos, mouseX, mouseY);
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
        super.render(graphics.graphics(), mouseX, mouseY, a);
        graphics.afterItems();

        if (hoveredEmpty != null && menu.getCarried().isEmpty()) {
            Item item = hoveredEmpty.index == MagnetarBlockEntity.SPEED_SLOT ? ModItems.SOLAR_WIND_UPGRADE.get()
                    : hoveredEmpty.index == MagnetarBlockEntity.RANGE_SLOT ? ModItems.MAGNETIC_FIELD_UPGRADE.get() : ModItems.WORMHOLE_LINK.get();
            graphics.setTooltipForNextFrame(font, List.of(item.getDefaultInstance().getHoverName()), Optional.empty(), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(com.exoticmoss.simpleores.util.gui.MouseButtonEvent event, boolean doubleClick) {
        if (NetworkTabs.click(event.x(), event.y(), leftPos, topPos, imageWidth, 0, menu.pos)) return true;
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    protected void init() {
        super.init();
        // Shows (or hides) the area of the field, around the Magnetar in the world
        addRenderableWidget(Button.builder(areaLabel(), b -> {
            MagnetarInfo info = MagnetarInfo.last(menu.pos);
            AreaDisplay.toggle(menu.pos, info == null ? StarNetwork.FIELD_RADIUS : info.radius());
            b.setMessage(areaLabel());
        }).bounds(leftPos + imageWidth - 68, topPos + MagnetarMenu.INFO_TOP - 2, 60, 14).build());
    }

    private Component areaLabel() {
        return Component.translatable(AreaDisplay.isShown(menu.pos) ? "screen.simpleores.area.hide" : "screen.simpleores.area.show");
    }

    /// How the network is (sent by the server every second).
    private void drawInfo(GuiGraphicsExtractor graphics) {
        MagnetarInfo info = MagnetarInfo.last(menu.pos);
        if (info == null) return;
        AreaDisplay.update(menu.pos, info.radius());
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
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
    }

    // ---------------------------------------------------------------- 1.21.1 screen methods

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(guiGraphics);
        extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.flush();
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /// The dark background is drawn first in render (the panels are drawn by extractRenderState).
    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClicked(new MouseButtonEvent(mouseX, mouseY, button), false);
    }
}
