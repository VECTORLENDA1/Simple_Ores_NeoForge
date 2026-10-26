package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.CellTier;
import com.exoticmoss.simpleores.starstorage.StarNetwork;
import com.exoticmoss.simpleores.starstorage.TerminalAction;
import com.exoticmoss.simpleores.starstorage.TerminalContents;
import com.exoticmoss.simpleores.starstorage.TerminalCrafting;
import com.exoticmoss.simpleores.starstorage.TerminalGrid;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/// The screen of the Stellar Terminal.
///  - Top left: crafting grid (3x3). Click a grid slot to pick it, then click an item of the network or of the
///    inventory to put it there (right-click empties a slot). The grid only shows the items: when crafting, the
///    ingredients are taken from the network (or from the inventory). Result: left-click crafts one,
///    right-click crafts a stack.
///  - Top right: how full each cell of the network is (mouse wheel to see more).
///  - Search box and sort button (most first, least first, A-Z, Z-A).
///  - Network: left-click takes a stack, right-click takes one. Mouse wheel scrolls.
///  - Inventory (bottom): clicking an item stores it. "Store all" stores the whole inventory (not the hotbar).
/// The screen only asks the server (TerminalAction, TerminalGrid); the server changes the network and sends
/// back the new contents (TerminalContents). The game is not paused, so the server keeps answering.
public class TerminalScreen extends Screen {
    /// Network grid: columns, and the size of one cell (pixels).
    private static final int COLUMNS = 9;
    private static final int CELL_W = 26;
    private static final int CELL_H = 28;
    /// Inventory and crafting slots (like the vanilla ones).
    private static final int SLOT = 18;
    /// Rows of the cells panel, and the top part (crafting + cells) and the search line.
    private static final int CELL_ROWS = 5;
    private static final int CRAFT_TOP = 20;
    private static final int SEARCH_TOP = 80;
    private static final int GRID_TOP = 100;
    /// The screen asks for the contents again every this many ticks (other players may change them).
    private static final int REFRESH_TICKS = 20;
    /// Colors (0xAARRGGBB).
    private static final int PANEL = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF8060C0;
    private static final int CELL_COLOR = 0xFF2A2A38;
    private static final int CELL_HOVER = 0xFF5A3A80;
    private static final int SLOT_COLOR = 0xFF20202C;
    private static final int SLOT_PICKED = 0xFF2F8F72;
    private static final int RESULT_COLOR = 0xFF34344A;
    private static final int BAR_BACK = 0xFF15151E;
    private static final int SCROLL_BAR = 0xFF8060C0;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;

    /// How the items are sorted.
    private static final String[] SORT_KEYS = {"most", "least", "a_z", "z_a"};
    private static int sort = 0;
    /// The crafting grid (kept while the game is open, also when the screen is closed).
    private static final ItemStack[] GRID = new ItemStack[TerminalCrafting.SIZE];

    static {
        Arrays.fill(GRID, ItemStack.EMPTY);
    }

    private final BlockPos pos;
    private TerminalContents contents;
    /// The items shown (after the search and the sorting).
    private List<TerminalContents.Item> shown = List.of();
    private String search = "";
    private int scroll;
    private int cellScroll;
    private int ticks;
    /// The crafting slot picked (waiting for an item), or -1.
    private int pickedSlot = -1;
    private Button sortButton;

    private int left, top, panelWidth, panelHeight, rows, gridTop, inventoryTop;

    public TerminalScreen(TerminalContents contents) {
        super(Component.translatable("screen.simpleores.terminal.title"));
        this.pos = contents.pos();
        this.contents = contents;
        filter();
        sendGrid();
    }

    public BlockPos pos() {
        return pos;
    }

    /// New contents from the server.
    public void update(TerminalContents contents) {
        this.contents = contents;
        filter();
    }

    /// Search and sort.
    private void filter() {
        String text = search.trim().toLowerCase(Locale.ROOT);
        List<TerminalContents.Item> list = new ArrayList<>();
        for (TerminalContents.Item item : contents.items()) {
            if (text.isEmpty() || name(item).toLowerCase(Locale.ROOT).contains(text)) list.add(item);
        }
        Comparator<TerminalContents.Item> byName = Comparator.comparing(item -> name(item).toLowerCase(Locale.ROOT));
        switch (sort) {
            case 1 -> list.sort(Comparator.comparingLong(TerminalContents.Item::count).thenComparing(byName));
            case 2 -> list.sort(byName);
            case 3 -> list.sort(byName.reversed());
            default -> list.sort(Comparator.comparingLong(TerminalContents.Item::count).reversed().thenComparing(byName));
        }
        shown = list;
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private static String name(TerminalContents.Item item) {
        return item.item().create().getHoverName().getString();
    }

    private int maxScroll() {
        int totalRows = (shown.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, totalRows - Math.max(1, rows));
    }

    private void send(int action, Optional<ItemStackTemplate> item, int slot) {
        ClientPacketDistributor.sendToServer(new TerminalAction(pos, action, item, slot));
    }

    private void sendGrid() {
        ClientPacketDistributor.sendToServer(new TerminalGrid(pos, List.of(GRID)));
    }

    // ---------------------------------------------------------------- layout and buttons

    @Override
    protected void init() {
        // Smaller windows show fewer rows of the network
        rows = Mth.clamp((height - 16 - (GRID_TOP + 30 + 3 * SLOT + 4 + SLOT + 10)) / CELL_H, 2, 5);
        panelWidth = COLUMNS * CELL_W + 20;
        panelHeight = GRID_TOP + rows * CELL_H + 30 + 3 * SLOT + 4 + SLOT + 10;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        gridTop = top + GRID_TOP;
        inventoryTop = gridTop + rows * CELL_H + 30;
        scroll = Mth.clamp(scroll, 0, maxScroll());

        EditBox searchBox = new EditBox(font, left + 10, top + SEARCH_TOP, 150, 14, Component.translatable("screen.simpleores.terminal.search"));
        searchBox.setHint(Component.translatable("screen.simpleores.terminal.search"));
        searchBox.setValue(search);
        searchBox.setResponder(value -> {
            search = value;
            scroll = 0;
            filter();
        });
        addRenderableWidget(searchBox);

        sortButton = addRenderableWidget(Button.builder(sortLabel(), b -> {
            sort = (sort + 1) % SORT_KEYS.length;
            b.setMessage(sortLabel());
            filter();
        }).bounds(left + panelWidth - 10 - 80, top + SEARCH_TOP - 2, 80, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.simpleores.terminal.clear"), b -> {
            Arrays.fill(GRID, ItemStack.EMPTY);
            pickedSlot = -1;
            sendGrid();
        }).bounds(left + 70, top + CRAFT_TOP + 40, 40, 14).build());

        int inventoryRight = left + 10 + COLUMNS * SLOT;
        addRenderableWidget(Button.builder(Component.translatable("screen.simpleores.terminal.store_all"),
                        b -> send(TerminalAction.INSERT_ALL, Optional.empty(), 0))
                .bounds(inventoryRight + 8, inventoryTop, left + panelWidth - 10 - inventoryRight - 8, 20).build());
    }

    private static Component sortLabel() {
        return Component.translatable("screen.simpleores.terminal.sort." + SORT_KEYS[sort]);
    }

    @Override
    public void tick() {
        // Every second: ask for the contents again (other players or machines may have changed them)
        if (++ticks % REFRESH_TICKS == 0) send(TerminalAction.REFRESH, Optional.empty(), 0);
        // Too far from the terminal: close
        if (minecraft.player == null || minecraft.player.position().distanceToSqr(Vec3.atCenterOf(pos))
                > TerminalAction.RANGE * TerminalAction.RANGE) {
            onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0) return true;
        int step = scrollY > 0 ? -1 : 1;
        if (x >= cellsX() && y >= top + CRAFT_TOP && y < top + CRAFT_TOP + 9 + CELL_ROWS * 9) {
            cellScroll = Mth.clamp(cellScroll + step, 0, Math.max(0, contents.cells().size() - CELL_ROWS));
        } else {
            scroll = Mth.clamp(scroll + step, 0, maxScroll());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        boolean right = event.button() == 1;

        // Crafting grid: pick a slot (left) or empty it (right)
        int craftSlot = craftSlotAt(event.x(), event.y());
        if (craftSlot >= 0) {
            if (right) {
                GRID[craftSlot] = ItemStack.EMPTY;
                if (pickedSlot == craftSlot) pickedSlot = -1;
                sendGrid();
            } else {
                pickedSlot = pickedSlot == craftSlot ? -1 : craftSlot;
            }
            return true;
        }
        // Result: craft
        if (isOverResult(event.x(), event.y())) {
            if (!contents.craftResult().isEmpty()) {
                send(right ? TerminalAction.CRAFT_STACK : TerminalAction.CRAFT_ONE, Optional.empty(), 0);
            }
            return true;
        }

        // Network grid: put in the crafting slot picked, or take
        int index = cellAt(event.x(), event.y());
        if (index >= 0) {
            if (pickedSlot >= 0) {
                putInGrid(shown.get(index).item().create());
            } else {
                send(right ? TerminalAction.TAKE_ONE : TerminalAction.TAKE_STACK, Optional.of(shown.get(index).item()), 0);
            }
            return true;
        }
        // Inventory: put in the crafting slot picked, or store
        int slot = slotAt(event.x(), event.y());
        if (slot >= 0 && minecraft.player != null) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.isEmpty()) return true;
            if (pickedSlot >= 0) putInGrid(stack);
            else send(TerminalAction.INSERT_SLOT, Optional.empty(), slot);
            return true;
        }
        return false;
    }

    /// Puts (a picture of) the item in the crafting slot picked, then picks the next empty slot.
    private void putInGrid(ItemStack stack) {
        GRID[pickedSlot] = stack.copyWithCount(1);
        int next = -1;
        for (int i = 1; i < TerminalCrafting.SIZE && next < 0; i++) {
            int candidate = (pickedSlot + i) % TerminalCrafting.SIZE;
            if (GRID[candidate].isEmpty()) next = candidate;
        }
        pickedSlot = next;
        sendGrid();
    }

    // ---------------------------------------------------------------- where things are

    private int craftX(int slot) {
        return left + 10 + (slot % 3) * SLOT;
    }

    private int craftY(int slot) {
        return top + CRAFT_TOP + (slot / 3) * SLOT;
    }

    private int craftSlotAt(double x, double y) {
        for (int slot = 0; slot < TerminalCrafting.SIZE; slot++) {
            if (x >= craftX(slot) && x < craftX(slot) + SLOT && y >= craftY(slot) && y < craftY(slot) + SLOT) return slot;
        }
        return -1;
    }

    private int resultX() {
        return left + 86;
    }

    private int resultY() {
        return top + CRAFT_TOP + 14;
    }

    private boolean isOverResult(double x, double y) {
        return x >= resultX() - 2 && x < resultX() + SLOT + 2 && y >= resultY() - 2 && y < resultY() + SLOT + 2;
    }

    private int cellsX() {
        return left + 124;
    }

    /// The item (index in "shown") under the mouse, or -1.
    private int cellAt(double x, double y) {
        if (x < left + 10 || y < gridTop) return -1;
        int column = (int) ((x - left - 10) / CELL_W);
        int row = (int) ((y - gridTop) / CELL_H);
        if (column >= COLUMNS || row >= rows) return -1;
        int index = (scroll + row) * COLUMNS + column;
        return index < shown.size() ? index : -1;
    }

    /// The inventory slot under the mouse (0-8 hotbar, 9-35 the rest), or -1.
    private int slotAt(double x, double y) {
        if (x < left + 10 || x >= left + 10 + COLUMNS * SLOT || y < inventoryTop) return -1;
        int column = (int) ((x - left - 10) / SLOT);
        double dy = y - inventoryTop;
        if (dy < 3 * SLOT) return TerminalAction.HOTBAR_SIZE + (int) (dy / SLOT) * COLUMNS + column;
        dy -= 3 * SLOT + 4;
        if (dy >= 0 && dy < SLOT) return column;
        return -1;
    }

    private int slotX(int slot) {
        return left + 10 + (slot % COLUMNS) * SLOT;
    }

    private int slotY(int slot) {
        return slot < TerminalAction.HOTBAR_SIZE ? inventoryTop + 3 * SLOT + 4 : inventoryTop + (slot / COLUMNS - 1) * SLOT;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        scroll = Mth.clamp(scroll, 0, maxScroll());

        // Panel
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.outline(left, top, panelWidth, panelHeight, PANEL_BORDER);
        graphics.centeredText(font, title, left + panelWidth / 2, top + 7, TEXT);

        drawCrafting(graphics, mouseX, mouseY);
        drawCells(graphics);

        // Network grid
        int hovered = cellAt(mouseX, mouseY);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int x = left + 10 + column * CELL_W;
                int y = gridTop + row * CELL_H;
                int index = (scroll + row) * COLUMNS + column;
                graphics.fill(x + 1, y + 1, x + CELL_W - 1, y + CELL_H - 1, index == hovered ? CELL_HOVER : CELL_COLOR);
                if (index >= shown.size()) continue;
                TerminalContents.Item item = shown.get(index);
                graphics.item(item.item().create(), x + (CELL_W - 16) / 2, y + 3);
                graphics.centeredText(font, Component.literal(StarNetwork.format(item.count())), x + CELL_W / 2, y + CELL_H - 10, TEXT);
            }
        }
        // Scroll bar (only when there are more rows than fit)
        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int barTop = gridTop + 1, barHeight = rows * CELL_H - 2;
            int thumb = Math.max(8, barHeight * rows / (rows + maxScroll));
            int thumbY = barTop + (barHeight - thumb) * scroll / maxScroll;
            graphics.fill(left + panelWidth - 7, thumbY, left + panelWidth - 4, thumbY + thumb, SCROLL_BAR);
        }

        // Used / capacity
        graphics.centeredText(font, Component.translatable("screen.simpleores.terminal.usage",
                        StarNetwork.format(contents.used()), StarNetwork.format(contents.capacity()), shown.size()),
                left + panelWidth / 2, gridTop + rows * CELL_H + 6, TEXT_DIM);

        // Player inventory
        graphics.text(font, Component.translatable("screen.simpleores.terminal.inventory"), left + 10, inventoryTop - 11, TEXT_DIM);
        int hoveredSlot = slotAt(mouseX, mouseY);
        if (minecraft.player != null) {
            for (int slot = 0; slot < TerminalAction.INVENTORY_SIZE; slot++) {
                int x = slotX(slot), y = slotY(slot);
                graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, slot == hoveredSlot ? CELL_HOVER : SLOT_COLOR);
                ItemStack stack = minecraft.player.getInventory().getItem(slot);
                if (stack.isEmpty()) continue;
                graphics.item(stack, x + 1, y + 1);
                if (stack.getCount() > 1) {
                    String count = String.valueOf(stack.getCount());
                    graphics.text(font, Component.literal(count), x + SLOT - 1 - font.width(count), y + SLOT - 9, TEXT);
                }
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
        drawTooltips(graphics, mouseX, mouseY, hovered, hoveredSlot);
    }

    /// The crafting grid, the arrow and the result.
    private void drawCrafting(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int hoveredSlot = craftSlotAt(mouseX, mouseY);
        for (int slot = 0; slot < TerminalCrafting.SIZE; slot++) {
            int x = craftX(slot), y = craftY(slot);
            int color = slot == pickedSlot ? SLOT_PICKED : slot == hoveredSlot ? CELL_HOVER : SLOT_COLOR;
            graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, color);
            if (!GRID[slot].isEmpty()) graphics.item(GRID[slot], x + 1, y + 1);
        }
        // Arrow
        int ax = left + 68, ay = top + CRAFT_TOP + 22;
        graphics.fill(ax, ay, ax + 12, ay + 2, TEXT_DIM);
        for (int i = 0; i < 4; i++) graphics.fill(ax + 8 + i, ay - 3 + i, ax + 9 + i, ay + 5 - i, TEXT_DIM);
        // Result
        int rx = resultX(), ry = resultY();
        graphics.fill(rx - 2, ry - 2, rx + SLOT + 2, ry + SLOT + 2, isOverResult(mouseX, mouseY) ? CELL_HOVER : RESULT_COLOR);
        ItemStack result = contents.craftResult();
        if (!result.isEmpty()) {
            graphics.item(result, rx + 1, ry + 1);
            if (result.getCount() > 1) {
                String count = String.valueOf(result.getCount());
                graphics.text(font, Component.literal(count), rx + SLOT - 1 - font.width(count), ry + SLOT - 9, TEXT);
            }
        }
    }

    /// How full each cell is: a small square with the color of the cell, a bar and the %.
    private void drawCells(GuiGraphicsExtractor graphics) {
        int x = cellsX(), y = top + CRAFT_TOP;
        int width = left + panelWidth - 10 - x;
        List<TerminalContents.Cell> cells = contents.cells();
        cellScroll = Mth.clamp(cellScroll, 0, Math.max(0, cells.size() - CELL_ROWS));
        graphics.text(font, Component.translatable("screen.simpleores.terminal.cells", cells.size()), x, y, TEXT_DIM);
        if (cells.isEmpty()) {
            graphics.text(font, Component.translatable("screen.simpleores.terminal.no_cells"), x, y + 12, TEXT_DIM);
            return;
        }
        for (int row = 0; row < CELL_ROWS && cellScroll + row < cells.size(); row++) {
            TerminalContents.Cell cell = cells.get(cellScroll + row);
            CellTier tier = CellTier.values()[Mth.clamp(cell.tier(), 0, CellTier.values().length - 1)];
            int ry = y + 11 + row * 9;
            double full = tier.capacity <= 0 ? 0 : Math.min(1, cell.used() / (double) tier.capacity);
            String percent = full > 0 && full < 0.01 ? "<1%" : Math.round(full * 100) + "%";
            int textWidth = font.width("100%");
            int barLeft = x + 9, barRight = x + width - textWidth - 4;
            graphics.fill(x, ry + 1, x + 6, ry + 7, 0xFF000000 | tier.glowColor);
            graphics.fill(barLeft, ry + 2, barRight, ry + 6, BAR_BACK);
            int fill = barLeft + (int) Math.round((barRight - barLeft) * full);
            int barColor = full >= 0.9 ? 0xFFD04040 : full >= 0.7 ? 0xFFD0A040 : 0xFF40B070;
            if (fill > barLeft) graphics.fill(barLeft, ry + 2, fill, ry + 6, barColor);
            graphics.text(font, Component.literal(percent), x + width - font.width(percent), ry, TEXT);
        }
        if (cells.size() > CELL_ROWS) {
            String more = (cellScroll + 1) + "-" + Math.min(cells.size(), cellScroll + CELL_ROWS) + "/" + cells.size();
            graphics.text(font, Component.literal(more), x + width - font.width(more), y, TEXT_DIM);
        }
    }

    private void drawTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int hovered, int hoveredSlot) {
        int craftSlot = craftSlotAt(mouseX, mouseY);
        if (hovered >= 0) {
            TerminalContents.Item item = shown.get(hovered);
            graphics.setTooltipForNextFrame(font, List.of(item.item().create().getHoverName(),
                            Component.translatable("screen.simpleores.terminal.stored", String.format(Locale.ROOT, "%,d", item.count())),
                            Component.translatable(pickedSlot >= 0 ? "screen.simpleores.terminal.put_hint" : "screen.simpleores.terminal.take_hint")),
                    Optional.empty(), mouseX, mouseY);
        } else if (hoveredSlot >= 0 && minecraft.player != null && !minecraft.player.getInventory().getItem(hoveredSlot).isEmpty()) {
            graphics.setTooltipForNextFrame(font, List.of(minecraft.player.getInventory().getItem(hoveredSlot).getHoverName(),
                    Component.translatable(pickedSlot >= 0 ? "screen.simpleores.terminal.put_hint" : "screen.simpleores.terminal.store_hint")),
                    Optional.empty(), mouseX, mouseY);
        } else if (craftSlot >= 0) {
            List<Component> lines = new ArrayList<>();
            if (!GRID[craftSlot].isEmpty()) lines.add(GRID[craftSlot].getHoverName());
            lines.add(Component.translatable("screen.simpleores.terminal.grid_hint"));
            graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        } else if (isOverResult(mouseX, mouseY) && !contents.craftResult().isEmpty()) {
            graphics.setTooltipForNextFrame(font, List.of(contents.craftResult().getHoverName(),
                    Component.translatable("screen.simpleores.terminal.craft_hint")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
