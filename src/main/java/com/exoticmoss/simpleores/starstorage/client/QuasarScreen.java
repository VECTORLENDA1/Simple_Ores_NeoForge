package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.starstorage.Pattern;
import com.exoticmoss.simpleores.starstorage.QuasarMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// The screen of the Quasar (see QuasarMenu for where everything is).
///  - Two tabs at the top: crafting table recipes and Gravity Core recipes (level 2, later).
///  - The recipe grid: put items with the mouse (they stay in your hand, only a picture goes to the grid),
///    or use JEI's "+" button. "Save" keeps the recipe in the Quasar.
///  - The saved recipes: right-click one to remove it. Mouse wheel scrolls.
public class QuasarScreen extends AbstractContainerScreen<QuasarMenu> {
    private static final int COLUMNS = 9;
    private static final int PATTERN_ROWS = 2;
    private static final int TAB = 22;
    /// Colors (0xAARRGGBB).
    private static final int PANEL = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF8060C0;
    private static final int SLOT_COLOR = 0xFF20202C;
    private static final int SLOT_HOVER = 0xFF5A3A80;
    private static final int RESULT_COLOR = 0xFF34344A;
    private static final int TAB_ON = 0xFF2A2A3E;
    private static final int TAB_OFF = 0xE0101018;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;

    /// 0 = crafting table, 1 = Gravity Core.
    private int tab;
    private int scroll;
    private Button saveButton;
    private Button clearButton;

    public QuasarScreen(QuasarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, QuasarMenu.WIDTH, QuasarMenu.HEIGHT);
    }

    private List<Pattern> patterns() {
        return QuasarClient.patterns(menu.pos);
    }

    private void button(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void init() {
        super.init();
        saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.simpleores.quasar.save"), b -> button(QuasarMenu.SAVE))
                .bounds(leftPos + QuasarMenu.RESULT_X - 12, topPos + QuasarMenu.RESULT_Y + 24, 42, 16).build());
        // Small X at the top right of the grid: empties it (the X is drawn in extractRenderState)
        clearButton = addRenderableWidget(Button.builder(Component.empty(), b -> button(QuasarMenu.CLEAR))
                .bounds(leftPos + QuasarMenu.GRID_X + 54 - 11, topPos + QuasarMenu.GRID_Y - 12, 11, 11).build());
    }

    // ---------------------------------------------------------------- mouse

    private int tabX(int which) {
        return leftPos + 4 + which * (TAB + 2);
    }

    private int tabY() {
        return topPos - TAB + 2;
    }

    private int patternX(int index) {
        return leftPos + 8 + (index % COLUMNS) * 18;
    }

    private int patternY(int index) {
        return topPos + QuasarMenu.PATTERNS_Y + (index / COLUMNS) * 18;
    }

    /// The saved recipe under the mouse (index in the list), or -1.
    private int patternAt(double x, double y) {
        for (int i = 0; i < COLUMNS * PATTERN_ROWS; i++) {
            int index = scroll * COLUMNS + i;
            if (index >= patterns().size()) break;
            if (x >= patternX(i) && x < patternX(i) + 18 && y >= patternY(i) && y < patternY(i) + 18) return index;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int which = 0; which < 2; which++) {
            if (event.x() >= tabX(which) && event.x() < tabX(which) + TAB && event.y() >= tabY() && event.y() < tabY() + TAB) {
                tab = which;
                return true;
            }
        }
        int index = patternAt(event.x(), event.y());
        if (index >= 0) {
            if (event.button() == 1) button(QuasarMenu.REMOVE + index);
            return true;
        }
        if (tab == 1 && isOverGrid(event.x(), event.y())) return true;
        boolean handled = super.mouseClicked(event, doubleClick);
        setFocused(null);
        return handled;
    }

    private boolean isOverGrid(double x, double y) {
        return x >= leftPos + QuasarMenu.GRID_X && x < leftPos + QuasarMenu.RESULT_X + 18 && y >= topPos + QuasarMenu.GRID_Y && y < topPos + QuasarMenu.GRID_Y + 54;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int rows = (patterns().size() + COLUMNS - 1) / COLUMNS;
        if (scrollY != 0) scroll = Mth.clamp(scroll + (scrollY > 0 ? -1 : 1), 0, Math.max(0, rows - PATTERN_ROWS));
        return true;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        saveButton.visible = tab == 0;
        clearButton.visible = tab == 0;

        // Tabs (above the panel)
        ItemStack[] icons = {new ItemStack(Items.CRAFTING_TABLE), new ItemStack(ModBlocks.GRAVITY_CORE_SUN.get())};
        for (int which = 0; which < 2; which++) {
            int x = tabX(which), y = tabY();
            graphics.fill(x, y, x + TAB, y + TAB, which == tab ? TAB_ON : TAB_OFF);
            graphics.outline(x, y, TAB, TAB, PANEL_BORDER);
            graphics.item(icons[which], x + 3, y + 3);
        }

        // Panel
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, PANEL_BORDER);
        graphics.centeredText(font, title, leftPos + imageWidth / 2, topPos + 7, TEXT);

        if (tab == 0) {
            // Recipe grid, arrow and result
            for (int i = 0; i <= QuasarMenu.GRID_SLOTS; i++) {
                Slot slot = menu.slots.get(i);
                int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
                if (i == QuasarMenu.RESULT_SLOT) graphics.fill(x - 2, y - 2, x + 20, y + 20, RESULT_COLOR);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_COLOR);
            }
            int ax = leftPos + QuasarMenu.GRID_X + 58, ay = topPos + QuasarMenu.RESULT_Y + 8;
            graphics.fill(ax, ay, ax + 14, ay + 2, TEXT_DIM);
            for (int i = 0; i < 4; i++) graphics.fill(ax + 10 + i, ay - 3 + i, ax + 11 + i, ay + 5 - i, TEXT_DIM);
        } else {
            graphics.centeredText(font, Component.translatable("screen.simpleores.quasar.core_soon"), leftPos + imageWidth / 2, topPos + 50, TEXT_DIM);
        }

        // Saved recipes
        graphics.text(font, Component.translatable("screen.simpleores.quasar.recipes", patterns().size()), leftPos + 8, topPos + QuasarMenu.PATTERNS_Y - 11, TEXT_DIM);
        int hovered = patternAt(mouseX, mouseY);
        for (int i = 0; i < COLUMNS * PATTERN_ROWS; i++) {
            int index = scroll * COLUMNS + i;
            int x = patternX(i), y = patternY(i);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, index == hovered ? SLOT_HOVER : SLOT_COLOR);
            if (index < patterns().size()) graphics.item(patterns().get(index).outputStack(), x + 1, y + 1);
        }

        // Inventory
        graphics.text(font, playerInventoryTitle, leftPos + 8, topPos + QuasarMenu.INVENTORY_Y - 11, TEXT_DIM);
        for (int i = QuasarMenu.INVENTORY_START; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, SLOT_COLOR);
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);

        if (tab == 0 && clearButton != null) {
            int cx = clearButton.getX() + 3, cy = clearButton.getY() + 3;
            for (int i = 0; i < 5; i++) {
                graphics.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, TEXT);
                graphics.fill(cx + 4 - i, cy + i, cx + 5 - i, cy + i + 1, TEXT);
            }
        }
        if (hovered >= 0) {
            Pattern pattern = patterns().get(hovered);
            List<Component> lines = new ArrayList<>();
            lines.add(pattern.outputStack().getHoverName().copy().append(" x" + pattern.count()));
            for (List<ItemStackTemplate> slot : pattern.inputs()) {
                if (!slot.isEmpty()) lines.add(Component.literal(" - ").append(slot.get(0).create().getHoverName()));
            }
            graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    /// In the Gravity Core tab the grid slots are hidden.
    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (tab == 1 && slot.index <= QuasarMenu.RESULT_SLOT) return;
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
