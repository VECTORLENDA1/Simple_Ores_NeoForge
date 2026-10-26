package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.starstorage.Pattern;
import com.exoticmoss.simpleores.starstorage.QuasarJobs;
import com.exoticmoss.simpleores.starstorage.QuasarMenu;
import com.exoticmoss.simpleores.starstorage.StarNetwork;
import com.exoticmoss.simpleores.starstorage.TerminalLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// The screen of the Quasar: like the Stellar Terminal (the items of the network, the search, the sort, the
/// inventory, and crafting the items it has recipes for), and four tabs above the panel:
///  - crafting table / Gravity Core / furnace: the recipe at the bottom right. Put items in its slots with the
///    mouse (only a picture goes there), drag them from the JEI list, or use JEI's "+". "Save" keeps the recipe.
///    The Gravity Core tab starts with one slot; one more appears for each item (up to 20, the Black Hole).
///  - auto-crafts: the middle shows the auto-crafts being made (what, how many, the step and what it waits for),
///    with an X to cancel each one.
///  - Top left: the recipes kept in the Quasar. Right-click one to remove it. Mouse wheel scrolls.
public class QuasarScreen extends TerminalScreen<QuasarMenu> {
    private static final int PATTERN_COLUMNS = 8;
    private static final int PATTERN_ROWS = 2;
    private static final int TAB = 22;
    private static final int TABS = 4;
    private static final int TAB_ON = 0xFF2A2A3E;
    private static final int TAB_OFF = 0xE0101018;
    private static final int JOB_H = 26;

    private int patternScroll;
    private int jobScroll;
    private Button saveButton;

    public QuasarScreen(QuasarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private List<Pattern> patterns() {
        return QuasarClient.patterns(menu.pos);
    }

    private List<QuasarJobs.View> jobs() {
        return QuasarClient.jobs(menu.pos);
    }

    private void button(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /// Shows a tab (and tells the server, which works out the result of that tab).
    public void setTab(int tab) {
        menu.tab = tab;
        button(QuasarMenu.TAB + tab);
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

    @Override
    protected boolean showNetwork() {
        return menu.tab != QuasarMenu.TAB_JOBS;
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
        for (int which = 0; which < TABS; which++) {
            if (event.x() >= tabX(which) && event.x() < tabX(which) + TAB && event.y() >= tabY() && event.y() < tabY() + TAB) {
                setTab(which);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
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
        if (!isOverCells(x, y)) {
            if (menu.tab == QuasarMenu.TAB_JOBS && y >= networkTopY()) {
                jobScroll = Mth.clamp(jobScroll + step, 0, Math.max(0, jobs().size() - jobRows()));
                return true;
            }
            return false;
        }
        int rows = (patterns().size() + PATTERN_COLUMNS - 1) / PATTERN_COLUMNS;
        patternScroll = Mth.clamp(patternScroll + step, 0, Math.max(0, rows - PATTERN_ROWS));
        return true;
    }

    // ---------------------------------------------------------------- auto-crafts tab

    private int jobRows() {
        return Math.max(1, menu.layout.rows() * TerminalLayout.CELL_H / JOB_H);
    }

    /// The X of the job in row "row": {x, y, size}.
    private int[] cancelBox(int row) {
        int y = networkTopY() + row * JOB_H;
        return new int[]{leftPos + imageWidth - 10 - 14, y + 6, 12};
    }

    @Override
    protected boolean clickMiddle(MouseButtonEvent event) {
        List<QuasarJobs.View> jobs = jobs();
        for (int row = 0; row < jobRows() && jobScroll + row < jobs.size(); row++) {
            int[] box = cancelBox(row);
            if (event.x() >= box[0] && event.x() < box[0] + box[2] && event.y() >= box[1] && event.y() < box[1] + box[2]) {
                button(QuasarMenu.CANCEL_JOB + jobs.get(jobScroll + row).id());
                return true;
            }
        }
        return true;
    }

    @Override
    protected void drawMiddle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<QuasarJobs.View> jobs = jobs();
        int left = leftPos + 10, width = imageWidth - 20;
        if (jobs.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.simpleores.quasar.no_jobs"), leftPos + imageWidth / 2, networkTopY() + 10, TEXT_DIM);
            return;
        }
        for (int row = 0; row < jobRows() && jobScroll + row < jobs.size(); row++) {
            QuasarJobs.View job = jobs.get(jobScroll + row);
            int y = networkTopY() + row * JOB_H;
            graphics.fill(left, y + 1, left + width, y + JOB_H - 1, 0xFF1C1C28);
            graphics.item(job.output(), left + 4, y + 5);
            graphics.text(font, Component.literal(StarNetwork.format(job.amount()) + " x ").append(job.output().getHoverName()), left + 24, y + 4, TEXT);
            graphics.text(font, Component.translatable(job.status()).append("  " + (job.step() + 1) + "/" + Math.max(1, job.steps())), left + 24, y + 14, TEXT_DIM);
            // Progress bar
            int barLeft = left + width - 80, barRight = left + width - 20;
            graphics.fill(barLeft, y + 10, barRight, y + 14, 0xFF15151E);
            int fill = barLeft + (barRight - barLeft) * job.step() / Math.max(1, job.steps());
            if (fill > barLeft) graphics.fill(barLeft, y + 10, fill, y + 14, 0xFF40B070);
            // Cancel X
            int[] box = cancelBox(row);
            graphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[2], 0xFFC02030);
            for (int i = 0; i < 6; i++) {
                graphics.fill(box[0] + 3 + i, box[1] + 3 + i, box[0] + 4 + i, box[1] + 4 + i, TEXT);
                graphics.fill(box[0] + 8 - i, box[1] + 3 + i, box[0] + 9 - i, box[1] + 4 + i, TEXT);
            }
        }
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
        if (saveButton != null) saveButton.visible = menu.tab != QuasarMenu.TAB_JOBS;
        // Tabs (above the panel)
        ItemStack[] icons = {new ItemStack(Items.CRAFTING_TABLE), new ItemStack(ModBlocks.GRAVITY_CORE_SUN.get()),
                new ItemStack(Items.FURNACE), new ItemStack(Items.CLOCK)};
        for (int which = 0; which < TABS; which++) {
            int x = tabX(which), y = tabY();
            graphics.fill(x, y, x + TAB, y + TAB, which == menu.tab ? TAB_ON : TAB_OFF);
            graphics.outline(x, y, TAB, TAB, PANEL_BORDER);
            graphics.item(icons[which], x + 3, y + 3);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void drawMore(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int hovered = patternAt(mouseX, mouseY);
        if (hovered >= 0) {
            Pattern pattern = patterns().get(hovered);
            List<Component> lines = new ArrayList<>();
            lines.add(pattern.outputStack().getHoverName().copy().append(" x" + pattern.count()));
            for (int i = 0; i < pattern.inputs().size(); i++) {
                List<ItemStackTemplate> slot = pattern.inputs().get(i);
                if (!slot.isEmpty()) {
                    String amount = pattern.amount(i) > 1 ? pattern.amount(i) + " x " : "";
                    lines.add(Component.literal(" - " + amount).append(slot.get(0).create().getHoverName()));
                }
            }
            graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        }
        // Tab names
        String[] keys = {"crafting", "core", "furnace", "jobs"};
        for (int which = 0; which < TABS; which++) {
            if (mouseX >= tabX(which) && mouseX < tabX(which) + TAB && mouseY >= tabY() && mouseY < tabY() + TAB) {
                graphics.setTooltipForNextFrame(font, List.of(Component.translatable("screen.simpleores.quasar.tab." + keys[which])),
                        Optional.empty(), mouseX, mouseY);
            }
        }
    }

    /// The left and top of the panel (for the JEI drag and drop).
    public int left() {
        return leftPos;
    }

    public int top() {
        return topPos;
    }
}
