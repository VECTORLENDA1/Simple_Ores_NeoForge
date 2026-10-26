package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.CellTier;
import com.exoticmoss.simpleores.starstorage.CraftRequest;
import com.exoticmoss.simpleores.starstorage.StarNetwork;
import com.exoticmoss.simpleores.starstorage.TerminalAction;
import com.exoticmoss.simpleores.starstorage.TerminalContents;
import com.exoticmoss.simpleores.starstorage.TerminalLayout;
import com.exoticmoss.simpleores.starstorage.TerminalMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/// The screen of the Stellar Terminal (see TerminalLayout for where everything is).
///  - Top: how full each cell of the network is (mouse wheel to see more), the search box (right-click clears it)
///    and the sort button (most first, least first, A-Z, Z-A).
///  - Middle: the items of the network. Left-click takes a stack, right-click takes one (into the mouse cursor);
///    Shift + click takes a stack into the inventory. Clicking with an item in the mouse cursor stores it
///    (right-click: only one). Mouse wheel scrolls.
///  - Bottom: the inventory and a normal crafting grid. Shift + click on an item of the inventory stores it.
///  - Items the Quasars can craft: shown with "Craft" when the network has none. Clicking it (or Ctrl + click
///    on any craftable item) opens a small panel to choose how many to craft.
/// The screen only asks the server (TerminalAction); the server changes the network and sends back the new
/// contents (TerminalContents). The game is not paused, so the server keeps answering.
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    /// Rows of the cells panel.
    private static final int CELL_ROWS = 5;
    /// The screen asks for the contents again every this many ticks (other players may change them).
    private static final int REFRESH_TICKS = 20;
    /// Colors (0xAARRGGBB).
    private static final int PANEL = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF8060C0;
    private static final int CELL_COLOR = 0xFF2A2A38;
    private static final int CELL_HOVER = 0xFF5A3A80;
    private static final int SLOT_COLOR = 0xFF20202C;
    private static final int RESULT_COLOR = 0xFF34344A;
    private static final int BAR_BACK = 0xFF15151E;
    private static final int SCROLL_BAR = 0xFF8060C0;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;

    /// How the items are sorted (kept while the game is open).
    private static final String[] SORT_KEYS = {"most", "least", "a_z", "z_a"};
    private static int sort = 0;

    private final TerminalLayout layout;
    private TerminalContents contents;
    /// The items shown (after the search and the sorting).
    private List<TerminalContents.Item> shown = List.of();
    private String search = "";
    private int scroll;
    private int cellScroll;
    private int ticks;
    private EditBox searchBox;
    private Button sortButton;
    /// Craft panel: the item (null = closed), the amount box, its buttons and its message.
    private ItemStackTemplate craftItem;
    private EditBox amountBox;
    private Button startButton;
    private Button cancelButton;
    private final List<Component> craftMessage = new ArrayList<>();
    private Button clearButton;

    public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TerminalLayout.WIDTH, menu.layout.height());
        this.layout = menu.layout;
        this.contents = TerminalClient.last(menu.pos);
        filter();
    }

    /// New contents from the server.
    public void update(TerminalContents contents) {
        this.contents = contents;
        filter();
    }

    /// Search and sort.
    private void filter() {
        String text = search.trim().toLowerCase(Locale.ROOT);
        List<TerminalContents.Item> all = new ArrayList<>(contents.items());
        // The items that can be crafted and that the network doesn't have: shown with "Craft"
        for (ItemStackTemplate craftable : contents.craftables()) {
            ItemStack stack = craftable.create();
            if (all.stream().noneMatch(item -> ItemStack.isSameItemSameComponents(item.item().create(), stack))) {
                all.add(new TerminalContents.Item(craftable, 0));
            }
        }
        List<TerminalContents.Item> list = new ArrayList<>();
        for (TerminalContents.Item item : all) {
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
        int totalRows = (shown.size() + TerminalLayout.COLUMNS - 1) / TerminalLayout.COLUMNS;
        return Math.max(0, totalRows - layout.rows());
    }

    private void send(int action, Optional<ItemStackTemplate> item) {
        ClientPacketDistributor.sendToServer(new TerminalAction(menu.pos, action, item));
    }

    // ---------------------------------------------------------------- layout and buttons

    @Override
    protected void init() {
        super.init();
        int right = leftPos + TerminalLayout.WIDTH - 10;
        searchBox = new EditBox(font, right - 104, topPos + TerminalLayout.TOP + 2, 104, 14, Component.translatable("screen.simpleores.terminal.search"));
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
        }).bounds(right - 104, topPos + TerminalLayout.TOP + 22, 104, 18).build());

        // Small X at the top right of the crafting grid: puts the items of the grid back in the network
        int gridRight = leftPos + TerminalLayout.craftLeft() + 3 * TerminalLayout.SLOT;
        // (the X is drawn in extractRenderState, so it is exactly in the middle of the button)
        clearButton = addRenderableWidget(Button.builder(Component.empty(), b -> send(TerminalAction.CLEAR_GRID, Optional.empty()))
                .bounds(gridRight - 11, topPos + layout.inventoryTop() - 12, 11, 11).build());
    }

    private boolean isCraftable(ItemStackTemplate item) {
        ItemStack stack = item.create();
        return contents.craftables().stream().anyMatch(craftable -> ItemStack.isSameItemSameComponents(craftable.create(), stack));
    }

    // ---------------------------------------------------------------- craft panel

    private static final int CRAFT_W = 180;
    private static final int CRAFT_H = 96;

    private int craftLeft() {
        return leftPos + (imageWidth - CRAFT_W) / 2;
    }

    private int craftTop() {
        return topPos + 18;
    }

    private void openCraft(ItemStackTemplate item) {
        closeCraft();
        craftItem = item;
        craftMessage.clear();
        int x = craftLeft(), y = craftTop();
        amountBox = addRenderableWidget(new EditBox(font, x + 10, y + 34, 70, 16, Component.empty()));
        amountBox.setFilter(value -> value.matches("\\d{0,6}"));
        amountBox.setValue("1");
        startButton = addRenderableWidget(Button.builder(Component.translatable("screen.simpleores.terminal.craft.start"), b -> startCraft())
                .bounds(x + 86, y + 33, 40, 18).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("screen.simpleores.terminal.craft.cancel"), b -> closeCraft())
                .bounds(x + 130, y + 33, 40, 18).build());
        setFocused(amountBox);
        amountBox.setFocused(true);
        searchBox.visible = false;
        sortButton.visible = false;
    }

    private void closeCraft() {
        if (amountBox != null) removeWidget(amountBox);
        if (startButton != null) removeWidget(startButton);
        if (cancelButton != null) removeWidget(cancelButton);
        amountBox = null;
        startButton = null;
        cancelButton = null;
        craftItem = null;
        if (searchBox != null) searchBox.visible = true;
        if (sortButton != null) sortButton.visible = true;
        setFocused(null);
    }

    private void startCraft() {
        if (craftItem == null) return;
        int amount;
        try {
            amount = Integer.parseInt(amountBox.getValue());
        } catch (NumberFormatException e) {
            amount = 0;
        }
        if (amount <= 0) return;
        craftMessage.clear();
        craftMessage.add(Component.translatable("screen.simpleores.terminal.craft.working"));
        ClientPacketDistributor.sendToServer(new CraftRequest(menu.pos, craftItem, amount));
    }

    /// The server answered the craft request.
    public void onCraftReply(CraftRequest.Reply reply) {
        if (craftItem == null) return;
        if (reply.ok()) {
            closeCraft();
            return;
        }
        craftMessage.clear();
        if (!reply.error().isEmpty()) {
            craftMessage.add(Component.translatable(reply.error()).withColor(0xFFE05050));
            return;
        }
        craftMessage.add(Component.translatable("screen.simpleores.terminal.craft.missing").withColor(0xFFE05050));
        for (int i = 0; i < Math.min(3, reply.missing().size()); i++) {
            CraftRequest.Missing missing = reply.missing().get(i);
            craftMessage.add(Component.literal(StarNetwork.format(missing.count()) + " x ").append(missing.item().getHoverName()));
        }
        if (reply.missing().size() > 3) craftMessage.add(Component.literal("..."));
    }

    private void drawCraftPanel(GuiGraphicsExtractor graphics) {
        int x = craftLeft(), y = craftTop();
        graphics.fill(x, y, x + CRAFT_W, y + CRAFT_H, 0xF0161622);
        graphics.outline(x, y, CRAFT_W, CRAFT_H, PANEL_BORDER);
        ItemStack stack = craftItem.create();
        graphics.item(stack, x + 10, y + 9);
        graphics.text(font, Component.translatable("screen.simpleores.terminal.craft.title", stack.getHoverName()), x + 32, y + 13, TEXT);
        for (int i = 0; i < craftMessage.size(); i++) {
            graphics.text(font, craftMessage.get(i), x + 10, y + 56 + i * 10, TEXT_DIM);
        }
    }

    private static Component sortLabel() {
        return Component.translatable("screen.simpleores.terminal.sort." + SORT_KEYS[sort]);
    }

    @Override
    protected void containerTick() {
        // Every second: ask for the contents again (other players or machines may have changed them)
        if (++ticks % REFRESH_TICKS == 1) send(TerminalAction.REFRESH, Optional.empty());
    }

    // ---------------------------------------------------------------- mouse and keyboard

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0) return true;
        int step = scrollY > 0 ? -1 : 1;
        if (isOverCells(x, y)) {
            cellScroll = Mth.clamp(cellScroll + step, 0, Math.max(0, contents.cells().size() - CELL_ROWS));
        } else {
            scroll = Mth.clamp(scroll + step, 0, maxScroll());
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Right-click on the search box: clears it
        if (searchBox != null && event.button() == 1 && searchBox.isMouseOver(event.x(), event.y())) {
            searchBox.setValue("");
            return true;
        }
        boolean handled = handleClick(event, doubleClick);
        if (craftItem != null) {
            if (amountBox != null) {
                setFocused(amountBox);
                amountBox.setFocused(true);
            }
            return handled;
        }
        // Nothing stays selected (white outline) after a click: only the search box, while clicking on it
        if (searchBox == null || !searchBox.isMouseOver(event.x(), event.y())) {
            if (searchBox != null) searchBox.setFocused(false);
            setFocused(null);
        }
        return handled;
    }

    private boolean handleClick(MouseButtonEvent event, boolean doubleClick) {
        if (craftItem != null) {
            // The craft panel is open: only its box and buttons work
            int x = craftLeft(), y = craftTop();
            if (event.x() >= x && event.x() < x + CRAFT_W && event.y() >= y && event.y() < y + CRAFT_H) {
                return super.mouseClicked(event, doubleClick);
            }
            return true;
        }
        if (isOverNetwork(event.x(), event.y())) {
            boolean right = event.button() == 1;
            if (!menu.getCarried().isEmpty()) {
                // An item in the mouse cursor: store it
                send(right ? TerminalAction.STORE_CARRIED_ONE : TerminalAction.STORE_CARRIED, Optional.empty());
            } else {
                int index = cellAt(event.x(), event.y());
                if (index >= 0 && isCraftable(shown.get(index).item())
                        && (shown.get(index).count() == 0 || event.hasControlDown())) {
                    openCraft(shown.get(index).item());
                } else if (index >= 0) {
                    int action = event.hasShiftDown() ? TerminalAction.TAKE_TO_INVENTORY : right ? TerminalAction.TAKE_ONE : TerminalAction.TAKE_STACK;
                    send(action, Optional.of(shown.get(index).item()));
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /// While writing in the search box, the keys write (the inventory key doesn't close the screen).
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (craftItem != null) {
            // Craft panel: Esc closes it, Enter starts, the rest writes the amount
            if (event.isEscape()) closeCraft();
            else if (event.isConfirmation()) startCraft();
            else if (amountBox != null) amountBox.keyPressed(event);
            return true;
        }
        if (searchBox != null && searchBox.isFocused() && !event.isEscape()) {
            // Enter: stop writing in the search box
            if (event.isConfirmation()) {
                searchBox.setFocused(false);
                setFocused(null);
                return true;
            }
            searchBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    private int networkLeft() {
        return leftPos + 10;
    }

    private int networkTop() {
        return topPos + TerminalLayout.NETWORK_TOP;
    }

    private boolean isOverNetwork(double x, double y) {
        return x >= networkLeft() && x < networkLeft() + TerminalLayout.COLUMNS * TerminalLayout.CELL_W
                && y >= networkTop() && y < networkTop() + layout.rows() * TerminalLayout.CELL_H;
    }

    /// The item (index in "shown") under the mouse, or -1.
    private int cellAt(double x, double y) {
        if (!isOverNetwork(x, y)) return -1;
        int column = (int) ((x - networkLeft()) / TerminalLayout.CELL_W);
        int row = (int) ((y - networkTop()) / TerminalLayout.CELL_H);
        int index = (scroll + row) * TerminalLayout.COLUMNS + column;
        return index < shown.size() ? index : -1;
    }

    private int cellsLeft() {
        return leftPos + 10;
    }

    private int cellsWidth() {
        return 160;
    }

    private boolean isOverCells(double x, double y) {
        return x >= cellsLeft() && x < cellsLeft() + cellsWidth() && y >= topPos + TerminalLayout.TOP && y < networkTop();
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        scroll = Mth.clamp(scroll, 0, maxScroll());

        // Panel
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, PANEL_BORDER);
        graphics.centeredText(font, title, leftPos + imageWidth / 2, topPos + 7, TEXT);

        if (craftItem == null) drawCells(graphics);
        int hovered = drawNetwork(graphics, mouseX, mouseY);
        if (craftItem != null) {
            hovered = -1;
            drawCraftPanel(graphics);
        }

        // Used / capacity
        graphics.centeredText(font, Component.translatable("screen.simpleores.terminal.usage",
                        StarNetwork.format(contents.used()), StarNetwork.format(contents.capacity()), shown.size()),
                leftPos + imageWidth / 2, networkTop() + layout.rows() * TerminalLayout.CELL_H + 6, TEXT_DIM);

        // Inventory and crafting: the frames of the slots (the items are drawn by the screen itself)
        int top = topPos + layout.inventoryTop();
        graphics.text(font, Component.translatable("screen.simpleores.terminal.inventory"), leftPos + TerminalLayout.inventoryLeft(), top - 11, TEXT_DIM);
        graphics.text(font, Component.translatable("screen.simpleores.terminal.crafting"), leftPos + TerminalLayout.craftLeft(), top - 11, TEXT_DIM);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
            if (slot.index == TerminalMenu.RESULT_SLOT) {
                graphics.fill(x - 2, y - 2, x + TerminalLayout.SLOT + 2, y + TerminalLayout.SLOT + 2, RESULT_COLOR);
            }
            graphics.fill(x + 1, y + 1, x + TerminalLayout.SLOT - 1, y + TerminalLayout.SLOT - 1, SLOT_COLOR);
        }
        // Arrow between the crafting grid and the result
        int ax = leftPos + TerminalLayout.craftLeft() + 3 * TerminalLayout.SLOT + 4, ay = topPos + layout.resultY() + 8;
        graphics.fill(ax, ay, ax + 12, ay + 2, TEXT_DIM);
        for (int i = 0; i < 4; i++) graphics.fill(ax + 8 + i, ay - 3 + i, ax + 9 + i, ay + 5 - i, TEXT_DIM);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        // The X of the button that empties the crafting grid (5x5 pixels, in the middle of the button)
        if (clearButton != null) {
            int cx = clearButton.getX() + 3, cy = clearButton.getY() + 3;
            for (int i = 0; i < 5; i++) {
                graphics.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, TEXT);
                graphics.fill(cx + 4 - i, cy + i, cx + 5 - i, cy + i + 1, TEXT);
            }
        }

        // Tooltip of the items of the network
        if (hovered >= 0 && menu.getCarried().isEmpty()) {
            TerminalContents.Item item = shown.get(hovered);
            List<Component> lines = new ArrayList<>();
            lines.add(item.item().create().getHoverName());
            lines.add(Component.translatable("screen.simpleores.terminal.stored", String.format(Locale.ROOT, "%,d", item.count())));
            if (item.count() > 0 && isCraftable(item.item())) lines.add(Component.translatable("screen.simpleores.terminal.craft.ctrl").withColor(0xFF7FE0C0));
            graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    /// The title and "Inventory" are drawn above (in extractRenderState).
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    /// The items of the network. Returns the one under the mouse (index in "shown"), or -1.
    private int drawNetwork(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int hovered = cellAt(mouseX, mouseY);
        for (int row = 0; row < layout.rows(); row++) {
            for (int column = 0; column < TerminalLayout.COLUMNS; column++) {
                int x = networkLeft() + column * TerminalLayout.CELL_W;
                int y = networkTop() + row * TerminalLayout.CELL_H;
                int index = (scroll + row) * TerminalLayout.COLUMNS + column;
                graphics.fill(x + 1, y + 1, x + TerminalLayout.CELL_W - 1, y + TerminalLayout.CELL_H - 1, index == hovered ? CELL_HOVER : CELL_COLOR);
                if (index >= shown.size()) continue;
                TerminalContents.Item item = shown.get(index);
                if (craftItem != null) continue;
                graphics.item(item.item().create(), x + (TerminalLayout.CELL_W - 16) / 2, y + 3);
                Component count = item.count() == 0 ? Component.translatable("screen.simpleores.terminal.craft") : Component.literal(StarNetwork.format(item.count()));
                graphics.centeredText(font, count, x + TerminalLayout.CELL_W / 2, y + TerminalLayout.CELL_H - 10, item.count() == 0 ? 0xFF7FE0C0 : TEXT);
            }
        }
        // Scroll bar (only when there are more rows than fit)
        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int barTop = networkTop() + 1, barHeight = layout.rows() * TerminalLayout.CELL_H - 2;
            int thumb = Math.max(8, barHeight * layout.rows() / (layout.rows() + maxScroll));
            int thumbY = barTop + (barHeight - thumb) * scroll / maxScroll;
            graphics.fill(leftPos + imageWidth - 7, thumbY, leftPos + imageWidth - 4, thumbY + thumb, SCROLL_BAR);
        }
        return hovered;
    }

    /// How full each cell is: a small square with the color of the cell, a bar and the %.
    private void drawCells(GuiGraphicsExtractor graphics) {
        int x = cellsLeft(), y = topPos + TerminalLayout.TOP;
        int width = cellsWidth();
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
            int barLeft = x + 9, barRight = x + width - font.width("100%") - 4;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
