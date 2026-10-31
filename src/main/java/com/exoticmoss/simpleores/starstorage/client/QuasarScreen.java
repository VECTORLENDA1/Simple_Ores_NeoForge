package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.starstorage.quasar.Pattern;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarJobs;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarMenu;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalLayout;
import com.exoticmoss.simpleores.util.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import com.exoticmoss.simpleores.util.gui.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.exoticmoss.simpleores.util.ItemStackTemplate;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/// The screen of the Quasar: like the Stellar Terminal (the items of the network, the search, the sort, the
/// inventory, and crafting the items it has recipes for), and four tabs above the panel:
///  - crafting table / Gravity Core / furnace: the recipe at the bottom right. Put items in its slots with the
///    mouse (only a picture goes there), drag them from the JEI list, or use JEI's "+". "Save" keeps the recipe.
///    The Gravity Core tab starts with one slot; one more appears for each item (up to 20, the Black Hole).
///    3x3 of them are shown: the mouse wheel over them moves them up and down. Their slots keep amounts.
///  - auto-crafts: the middle shows the auto-crafts being made (what, how many, how far it is and what it does),
///    with an X to cancel each one.
///  - Top left: the recipes kept in the Quasar. Left-click one shows it in JEI, right-click removes it.
///    Mouse wheel scrolls.
public class QuasarScreen extends TerminalScreen<QuasarMenu> {
    private static final int PATTERN_COLUMNS = 8;
    private static final int PATTERN_ROWS = 2;
    private static final int TAB = 22;
    private static final int TABS = 4;
    /// The color of the panel (the tab shown looks like a part of it).
    private static final int TAB_ON = 0xE0101018;
    private static final int TAB_OFF = 0xF0060609;
    private static final int JOB_H = 26;

    /// JEI (set by its plugin when JEI is installed): shows the recipes of an item.
    public static Consumer<ItemStack> showRecipes = stack -> {
    };
    /// JEI: runs every tick while the screen is open (see QuasarGhostHandler).
    public static Runnable jeiTick = () -> {
    };

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

    /// The auto-crafts whose sub-crafts are shown (the small arrow on the left).
    private static final java.util.Set<Integer> expanded = new java.util.HashSet<>();

    /// A row of the jobs tab: an auto-craft (sub = null) or one of its sub-crafts.
    private record Row(QuasarJobs.View job, QuasarJobs.Sub sub) {
    }

    /// The rows of the jobs tab: each auto-craft, and under it its sub-crafts if they are shown.
    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (QuasarJobs.View job : jobs()) {
            rows.add(new Row(job, null));
            if (expanded.contains(job.id())) {
                for (QuasarJobs.Sub sub : job.subs()) rows.add(new Row(job, sub));
            }
        }
        return rows;
    }

    private void button(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /// Shows a tab (and tells the server, which works out the result of that tab).
    public void setTab(int tab) {
        menu.tab = tab;
        button(QuasarMenu.TAB + tab);
    }

    /// The kind of furnace of the furnace tab (Pattern.FURNACE, BLAST_FURNACE, SMOKER).
    public void setCooking(int kind) {
        menu.cooking = kind;
        button(QuasarMenu.COOKING + kind);
    }

    /// The left and top of the button of a kind of furnace (in the furnace tab, under its slot).
    private int cookingX(int kind) {
        return leftPos + TerminalLayout.craftLeft() + kind * TerminalLayout.SLOT;
    }

    private int cookingY() {
        return topPos + menu.layout.inventoryTop() + 2 * TerminalLayout.SLOT + 2;
    }

    /// The kind of furnace under the mouse (-1 = none).
    private int cookingAt(double x, double y) {
        if (menu.tab != QuasarMenu.TAB_FURNACE) return -1;
        for (int kind = Pattern.FURNACE; kind <= Pattern.SMOKER; kind++) {
            if (x >= cookingX(kind) && x < cookingX(kind) + TerminalLayout.SLOT && y >= cookingY() && y < cookingY() + TerminalLayout.SLOT) return kind;
        }
        return -1;
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

    @Override
    protected int tabsReserved() {
        return 4 + TABS * (TAB + 2) + 4;
    }

    private int tabY() {
        return topPos - TAB + 2;
    }

    @Override
    protected int[] topGap() {
        return new int[]{tabX(menu.tab) + 1, tabX(menu.tab) + TAB - 1};
    }

    @Override
    protected boolean showGrid() {
        return menu.tab != QuasarMenu.TAB_JOBS;
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
        int kind = cookingAt(event.x(), event.y());
        if (kind >= 0) {
            setCooking(kind);
            return true;
        }
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
        else if (event.button() == 0) showRecipes.accept(patterns().get(index).outputStack());
        return true;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        jeiTick.run();
    }

    /// True if the mouse is over the core slots (or the bar next to them).
    private boolean isOverCores(double x, double y) {
        int left = leftPos + TerminalLayout.craftLeft(), top = topPos + menu.layout.inventoryTop();
        return menu.tab == QuasarMenu.TAB_CORE && x >= left && x < left + 3 * TerminalLayout.SLOT + 6
                && y >= top && y < top + 3 * TerminalLayout.SLOT;
    }

    @Override
    protected boolean scrollTopLeft(double x, double y, int step) {
        // On a slot of the grid: picks one of its items
        if (menu.tab == QuasarMenu.TAB_CRAFTING) {
            for (int i = 0; i < QuasarMenu.GRID_SLOTS; i++) {
                net.minecraft.world.inventory.Slot slot = menu.slots.get(i);
                int slotX = leftPos + slot.x, slotY = topPos + slot.y;
                if (slot.hasItem() && x >= slotX && x < slotX + 16 && y >= slotY && y < slotY + 16) {
                    button(QuasarMenu.PICK + i * 2 + (step > 0 ? 1 : 0));
                    return true;
                }
            }
        }
        if (isOverCores(x, y)) {
            int row = Mth.clamp(menu.coreScroll + step, 0, menu.maxCoreScroll());
            if (row != menu.coreScroll) {
                menu.coreScroll = row;
                button(QuasarMenu.CORE_SCROLL + row);
            }
            return true;
        }
        if (!isOverCells(x, y)) {
            if (menu.tab == QuasarMenu.TAB_JOBS && y >= networkTopY()) {
                jobScroll = Mth.clamp(jobScroll + step, 0, Math.max(0, rows().size() - jobRows()));
                return true;
            }
            return false;
        }
        int rows = (patterns().size() + PATTERN_COLUMNS - 1) / PATTERN_COLUMNS;
        patternScroll = Mth.clamp(patternScroll + step, 0, Math.max(0, rows - PATTERN_ROWS));
        return true;
    }

    // ---------------------------------------------------------------- auto-crafts tab

    /// JEI: also the saved recipes (top left) and the items of the auto-crafts.
    @Override
    public @Nullable JeiTarget jeiTargetAt(double mouseX, double mouseY) {
        JeiTarget target = super.jeiTargetAt(mouseX, mouseY);
        if (target != null) return target;
        int index = patternAt(mouseX, mouseY);
        if (index >= 0) {
            int i = index - patternScroll * PATTERN_COLUMNS;
            return JeiTarget.of(patterns().get(index).outputStack().copyWithCount(1), patternX(i) + 1, patternY(i) + 1);
        }
        if (menu.tab == QuasarMenu.TAB_JOBS) {
            List<Row> rows = rows();
            for (int row = 0; row < jobRows() && jobScroll + row < rows.size(); row++) {
                Row line = rows.get(jobScroll + row);
                int x = itemX(line), y = networkTopY() + row * JOB_H + 5;
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    return JeiTarget.of(itemOf(line).copyWithCount(1), x, y);
                }
            }
        }
        return null;
    }

    private int jobRows() {
        return Math.max(1, menu.layout.rows() * TerminalLayout.CELL_H / JOB_H);
    }

    /// The X of the job in row "row": {x, y, size}.
    private int[] cancelBox(int row) {
        int y = networkTopY() + row * JOB_H;
        return new int[]{leftPos + imageWidth - 10 - 14, y + 6, 12};
    }

    /// The X of the item of a row (the sub-crafts are a little to the right).
    private int itemX(Row row) {
        return leftPos + 10 + (row.sub() == null ? 9 : 18);
    }

    private static ItemStack itemOf(Row row) {
        return row.sub() == null ? row.job().output() : row.sub().output();
    }

    @Override
    protected boolean clickMiddle(MouseButtonEvent event) {
        List<Row> rows = rows();
        for (int row = 0; row < jobRows() && jobScroll + row < rows.size(); row++) {
            Row line = rows.get(jobScroll + row);
            int y = networkTopY() + row * JOB_H;
            if (event.y() < y || event.y() >= y + JOB_H) continue;
            if (line.sub() == null) {
                int[] box = cancelBox(row);
                // (a little bigger than the X, so it is easy to hit)
                if (event.x() >= box[0] - 3 && event.x() < box[0] + box[2] + 3 && event.y() >= box[1] - 3 && event.y() < box[1] + box[2] + 3) {
                    button(QuasarMenu.CANCEL_JOB + line.job().id());
                    return true;
                }
                // The arrow: shows or hides the sub-crafts
                if (!line.job().subs().isEmpty() && event.x() >= leftPos + 10 && event.x() < itemX(line)) {
                    if (!expanded.remove(line.job().id())) expanded.add(line.job().id());
                    jobScroll = Mth.clamp(jobScroll, 0, Math.max(0, rows().size() - jobRows()));
                    return true;
                }
            }
            int x = itemX(line);
            if (event.button() == 0 && event.x() >= x && event.x() < x + 16 && event.y() >= y + 5 && event.y() < y + 21) {
                showRecipes.accept(itemOf(line));
                return true;
            }
        }
        return true;
    }

    @Override
    protected void drawMiddle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Row> rows = rows();
        int left = leftPos + 10, width = imageWidth - 20;
        if (rows.isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.simpleores.quasar.no_jobs"), leftPos + imageWidth / 2, networkTopY() + 10, TEXT_DIM);
            return;
        }
        for (int row = 0; row < jobRows() && jobScroll + row < rows.size(); row++) {
            Row line = rows.get(jobScroll + row);
            QuasarJobs.Sub sub = line.sub();
            int y = networkTopY() + row * JOB_H;
            int itemX = itemX(line);
            graphics.fill(left, y + 1, left + width, y + JOB_H - 1, sub == null ? 0xFF1C1C28 : 0xFF15151F);
            long amount, made;
            Component status;
            int statusColor = TEXT_DIM;
            if (sub == null) {
                QuasarJobs.View job = line.job();
                amount = job.amount();
                made = job.made();
                String[] parts = job.status().split("\\|");
                status = Component.translatable(parts[0], (Object[]) java.util.Arrays.copyOfRange(parts, 1, parts.length));
                // The small arrow (right = hidden, down = shown)
                if (!job.subs().isEmpty()) drawArrow(graphics, left + 2, y + 9, expanded.contains(job.id()));
            } else {
                amount = sub.amount();
                made = sub.made();
                boolean finished = made >= amount;
                status = Component.translatable(finished ? "screen.simpleores.quasar.step.done"
                        : sub.current() ? "screen.simpleores.quasar.job.working" : "screen.simpleores.quasar.step.waiting");
                if (finished) statusColor = 0xFF7FE8A0;
                // A line on the left joins the sub-crafts to their auto-craft
                graphics.fill(left + 4, y, left + 5, y + JOB_H, 0xFF3A3A50);
            }
            // Left: the item, how many and what it does now
            graphics.item(itemOf(line), itemX, y + 5);
            int textLeft = itemX + 20;
            int barLeft = left + 24 + 104 + 6 + 9, barRight = left + width - 24;
            int textWidth = barLeft - 6 - textLeft;
            String name = StarNetwork.format(amount) + " x " + itemOf(line).getHoverName().getString();
            graphics.text(font, Component.literal(font.plainSubstrByWidth(name, textWidth)), textLeft, y + 4, TEXT);
            graphics.text(font, status, textLeft, y + 14, statusColor);
            // Middle: the bar (items made / items asked for)
            graphics.fill(barLeft - 1, y + 4, barRight + 1, y + 12, 0xFF0C0C12);
            graphics.fill(barLeft, y + 5, barRight, y + 11, 0xFF15151E);
            int fill = barLeft + (int) ((barRight - barLeft) * Math.min(made, amount) / Math.max(1, amount));
            if (fill > barLeft) {
                graphics.fill(barLeft, y + 5, fill, y + 11, 0xFF3FB86A);
                graphics.fill(barLeft, y + 5, fill, y + 6, 0xFF7FE8A0);
            }
            graphics.centeredText(font, Component.literal(Math.min(made, amount) + "/" + amount), (barLeft + barRight) / 2, y + 14, TEXT_DIM);
            if (sub != null) continue;
            // Cancel X
            int[] box = cancelBox(row);
            graphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[2], 0xFFC02030);
            for (int i = 0; i < 6; i++) {
                graphics.fill(box[0] + 3 + i, box[1] + 3 + i, box[0] + 4 + i, box[1] + 4 + i, TEXT);
                graphics.fill(box[0] + 8 - i, box[1] + 3 + i, box[0] + 9 - i, box[1] + 4 + i, TEXT);
            }
        }
    }

    /// A small triangle: pointing right (closed) or down (open).
    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y, boolean open) {
        for (int i = 0; i < 4; i++) {
            if (open) graphics.fill(x + i, y + i, x + 7 - i, y + i + 1, TEXT_DIM);
            else graphics.fill(x + i, y - 3 + i, x + i + 1, y + 4 - i, TEXT_DIM);
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
            if (which == menu.tab) {
                // The tab shown joins the panel: same color, no line between them (see topGap)
                graphics.fill(x, y, x + TAB, topPos, TAB_ON);
                graphics.fill(x, y, x + TAB, y + 1, PANEL_BORDER);
                graphics.fill(x, y, x + 1, topPos + 1, PANEL_BORDER);
                graphics.fill(x + TAB - 1, y, x + TAB, topPos + 1, PANEL_BORDER);
            } else {
                graphics.fill(x, y, x + TAB, topPos, TAB_OFF);
                graphics.outline(x, y, TAB, topPos - y, PANEL_BORDER);
            }
            graphics.item(icons[which], x + 3, y + 2);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void drawMore(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The bar next to the core slots (only when there are more than fit)
        int maxCoreScroll = menu.maxCoreScroll();
        if (menu.tab == QuasarMenu.TAB_CORE && maxCoreScroll > 0) {
            int barX = leftPos + TerminalLayout.craftLeft() + 3 * TerminalLayout.SLOT + 2, barTop = topPos + menu.layout.inventoryTop();
            int barHeight = 3 * TerminalLayout.SLOT;
            int rows = 3 + maxCoreScroll;
            int thumb = Math.max(6, barHeight * 3 / rows);
            int thumbY = barTop + (barHeight - thumb) * Math.min(menu.coreScroll, maxCoreScroll) / maxCoreScroll;
            graphics.fill(barX, barTop, barX + 3, barTop + barHeight, SLOT_COLOR);
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumb, PANEL_BORDER);
        }
        // Furnace tab: the kinds of furnace (the one chosen has a border)
        if (menu.tab == QuasarMenu.TAB_FURNACE) {
            for (int kind = Pattern.FURNACE; kind <= Pattern.SMOKER; kind++) {
                int x = cookingX(kind), y = cookingY();
                graphics.fill(x + 1, y + 1, x + TerminalLayout.SLOT - 1, y + TerminalLayout.SLOT - 1, SLOT_COLOR);
                if (kind == menu.cooking) graphics.outline(x, y, TerminalLayout.SLOT, TerminalLayout.SLOT, PANEL_BORDER);
                graphics.item(Pattern.cookingBlock(kind), x + 1, y + 1);
            }
            int over = cookingAt(mouseX, mouseY);
            if (over >= 0) {
                graphics.setTooltipForNextFrame(font, List.of(Pattern.cookingBlock(over).getHoverName()), Optional.empty(), mouseX, mouseY);
            }
        }
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
            if (!pattern.machines().isEmpty()) {
                lines.add(Component.translatable("screen.simpleores.quasar.machine", pattern.machines().get(0).create().getHoverName()));
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

    /// A slot of the grid (or the result) with the recipe of a machine: its tooltip also says how to change the amount.
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        if (hoveredSlot != null && (hoveredSlot.index < QuasarMenu.GRID_SLOTS || hoveredSlot.index == QuasarMenu.RESULT_SLOT) && menu.tab == QuasarMenu.TAB_CRAFTING && menu.isMachine()) {
            lines.add(Component.translatable("screen.simpleores.quasar.amount.more").withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.translatable("screen.simpleores.quasar.amount.less").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return lines;
    }

    /// The left and top of the panel (for the JEI drag and drop).
    public int left() {
        return leftPos;
    }

    public int top() {
        return topPos;
    }
}
