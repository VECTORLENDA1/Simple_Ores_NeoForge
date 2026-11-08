package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.horizon.EventHorizonBlockEntity;
import com.exoticmoss.simpleores.starstorage.horizon.EventHorizonMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/// The screen of the Event Horizon: its filter (9 pictures of items) and its priority (the buttons change it by one,
/// with shift by ten), and the inventory.
public class EventHorizonScreen extends AbstractContainerScreen<EventHorizonMenu> {
    private static final int PANEL = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF8060C0;
    private static final int SLOT_COLOR = 0xFF2A2A38;
    private static final int FILTER_COLOR = 0xFFFFA040;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;
    private static final int BUTTON = 20;

    public EventHorizonScreen(EventHorizonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EventHorizonMenu.WIDTH, EventHorizonMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + EventHorizonMenu.PRIORITY_TOP;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> press(minecraft.hasShiftDown() ? EventHorizonMenu.LOWER_MORE : EventHorizonMenu.LOWER))
                .bounds(leftPos + imageWidth / 2 - 40, y, BUTTON, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> press(minecraft.hasShiftDown() ? EventHorizonMenu.HIGHER_MORE : EventHorizonMenu.HIGHER))
                .bounds(leftPos + imageWidth / 2 + 20, y, BUTTON, 14).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, PANEL_BORDER);
        graphics.centeredText(font, title, leftPos + imageWidth / 2, topPos + 7, TEXT);
        graphics.text(font, playerInventoryTitle, leftPos + 8, topPos + EventHorizonMenu.INVENTORY_TOP - 11, TEXT_DIM);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, SLOT_COLOR);
            if (slot.index < EventHorizonBlockEntity.FILTER_SLOTS) graphics.outline(x - 1, y - 1, 18, 18, FILTER_COLOR);
        }
        // The priority, between its buttons
        String priority = String.valueOf(menu.priority());
        int priorityY = topPos + EventHorizonMenu.PRIORITY_TOP + 3;
        graphics.centeredText(font, Component.literal(priority), leftPos + imageWidth / 2, priorityY, TEXT);
        super.extractRenderState(graphics, mouseX, mouseY, a);

        if (menu.getCarried().isEmpty()) {
            if (mouseY >= priorityY - 3 && mouseY < priorityY + 11 && mouseX >= leftPos + imageWidth / 2 - 20 && mouseX < leftPos + imageWidth / 2 + 20) {
                graphics.setTooltipForNextFrame(font, List.of(Component.translatable("screen.simpleores.horizon.priority", menu.priority()),
                        Component.translatable("screen.simpleores.horizon.priority_info").withStyle(net.minecraft.ChatFormatting.GRAY)),
                        Optional.empty(), mouseX, mouseY);
            }
            Slot over = hoveredSlot;
            if (over != null && over.index < EventHorizonBlockEntity.FILTER_SLOTS && !over.hasItem()) {
                graphics.setTooltipForNextFrame(font, List.of(Component.translatable("screen.simpleores.horizon.filter"),
                        Component.translatable("screen.simpleores.horizon.filter_info").withStyle(net.minecraft.ChatFormatting.GRAY)),
                        Optional.empty(), mouseX, mouseY);
            }
        }
    }

    /// The title and "Inventory" are drawn in extractRenderState.
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }
}
