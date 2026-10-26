package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.starstorage.Pattern;
import com.exoticmoss.simpleores.starstorage.QuasarMenu;
import com.exoticmoss.simpleores.starstorage.TerminalLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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

/// The screen of the Quasar: like the Stellar Terminal (the items of the network, the search, the sort and
/// the inventory), but it doesn't craft: the grid at the bottom right is the recipe grid.
///  - Two tabs above the panel: crafting table recipes and Gravity Core recipes.
///  - Recipe grid: click a slot with an item in the mouse cursor (only a picture of it goes there), drag an item
///    from the JEI list onto a slot, or use JEI's "+" button. Right-click (or an empty cursor) empties a slot.
///    "Save" keeps the recipe; the small X empties the grid.
///  - Top left: the recipes kept in the Quasar. Right-click one to remove it. Mouse wheel scrolls.
public class QuasarScreen extends TerminalScreen<QuasarMenu> {
    private static final int PATTERN_COLUMNS = 8;
    private static final int PATTERN_ROWS = 2;
    private static final int TAB = 22;
    private static final int TAB_ON = 0xFF2A2A3E;
    private static final int TAB_OFF = 0xE0101018;

    /// 0 = crafting table, 1 = Gravity Core.
    private int tab;
    private int patternScroll;
    private Button saveButton;

    public QuasarScreen(QuasarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private List<Pattern> patterns() {
        return QuasarClient.patterns(menu.pos);
    }

    private void button(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected boolean crafting() {
        return false;
    }

    @Override
    protected void clearGrid() {
        button(QuasarMenu.CLEAR);
    }

    @Override
    protected Component gridLabel() {
        return Component.translatable("screen.simpleores.quasar.recipe");
    }

    @Override
    protected void addMoreWidgets() {
        saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.simpleores.quasar.save"), b -> button(QuasarMenu.SAVE))
                .bounds(leftPos + menu.layout.resultX() - 10, topPos + menu.layout.resultY() + 24, 40, 14).build());
    }

    // ---------------------------------------------------------------- tabs and saved recipes

    private int tabX(int which) {
        return leftPos + 4 + which * (TAB + 2);
    }

    private int tabY() {
        return topPos - TAB + 2;
    }

    private int patternX(int i) {
        return cellsLeft() + (i % PATTERN_COLUMNS) * 18;
    }

    private int patternY(int i) {
        return topPos + TerminalLayout.TOP + 11 + (i / PATTERN_COLUMNS) * 18;
    }

    /// The saved recipe under the mouse (index in the list), or -1.
    private int patternAt(double x, double y) {
        for (int i = 0; i < PATTERN_COLUMNS * PATTERN_ROWS; i++) {
            int index = patternScroll * PATTERN_COLUMNS + i;
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
        // Gravity Core tab: the recipe grid is hidden
        if (tab == 1 && isOverRecipeGrid(event.x(), event.y())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isOverRecipeGrid(double x, double y) {
        int top = topPos + menu.layout.inventoryTop();
        return x >= leftPos + TerminalLayout.craftLeft() && x < leftPos + menu.layout.resultX() + 20 && y >= top - 12 && y < top + 3 * 18;
    }

    @Override
    protected boolean clickTopLeft(MouseButtonEvent event) {
        int index = patternAt(event.x(), event.y());
        if (index < 0) return false;
        if (event.button() == 1) button(QuasarMenu.REMOVE + index);
        return true;
    }

    @Override
    protected boolean scrollTopLeft(double x, double y, int step) {
        if (!isOverCells(x, y)) return false;
        int rows = (patterns().size() + PATTERN_COLUMNS - 1) / PATTERN_COLUMNS;
        patternScroll = Mth.clamp(patternScroll + step, 0, Math.max(0, rows - PATTERN_ROWS));
        return true;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void drawTopLeft(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.translatable("screen.simpleores.quasar.recipes", patterns().size()), cellsLeft(), topPos + TerminalLayout.TOP, TEXT_DIM);
        int hovered = patternAt(mouseX, mouseY);
        for (int i = 0; i < PATTERN_COLUMNS * PATTERN_ROWS; i++) {
            int index = patternScroll * PATTERN_COLUMNS + i;
            int x = patternX(i), y = patternY(i);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, index == hovered ? CELL_HOVER : SLOT_COLOR);
            if (index < patterns().size()) graphics.item(patterns().get(index).outputStack(), x + 1, y + 1);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (saveButton != null) saveButton.visible = tab == 0;
        // Tabs (above the panel)
        ItemStack[] icons = {new ItemStack(Items.CRAFTING_TABLE), new ItemStack(ModBlocks.GRAVITY_CORE_SUN.get())};
        for (int which = 0; which < 2; which++) {
            int x = tabX(which), y = tabY();
            graphics.fill(x, y, x + TAB, y + TAB, which == tab ? TAB_ON : TAB_OFF);
            graphics.outline(x, y, TAB, TAB, PANEL_BORDER);
            graphics.item(icons[which], x + 3, y + 3);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void drawMore(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (tab == 1) {
            int top = topPos + menu.layout.inventoryTop();
            graphics.fill(leftPos + TerminalLayout.craftLeft() - 2, top - 13, leftPos + imageWidth - 4, top + 3 * 18 + 2, 0xFF101018);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("screen.simpleores.quasar.core_hint"), imageWidth - TerminalLayout.craftLeft() - 8);
            for (int i = 0; i < lines.size(); i++) graphics.text(font, lines.get(i), leftPos + TerminalLayout.craftLeft(), top + 4 + i * 10, TEXT_DIM, false);
        }
        int hovered = patternAt(mouseX, mouseY);
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

    /// In the Gravity Core tab the recipe grid is hidden.
    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        if (tab == 1 && slot.index <= QuasarMenu.RESULT_SLOT) return;
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    /// The left and top of the panel (for the JEI drag and drop).
    public int left() {
        return leftPos;
    }

    public int top() {
        return topPos;
    }

    public boolean recipeTab() {
        return tab == 0;
    }
}
