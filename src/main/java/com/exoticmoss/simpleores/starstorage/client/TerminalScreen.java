package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.StarNetwork;
import com.exoticmoss.simpleores.starstorage.TerminalAction;
import com.exoticmoss.simpleores.starstorage.TerminalContents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/// The screen of the Stellar Terminal: every item of the Magnetar network, and the player's inventory below.
///  - Network (top): left-click takes a stack, right-click takes one. Mouse wheel scrolls. The box searches by name.
///  - Inventory (bottom): clicking an item puts it in the network. "Store all" puts the whole inventory
///    (not the hotbar) in the network.
/// The screen only asks the server (TerminalAction); the server changes the network and sends back the
/// new contents (TerminalContents). The game is not paused, so the server keeps answering.
public class TerminalScreen extends Screen {
    /// Network grid: columns, and the size of one cell (pixels).
    private static final int COLUMNS = 9;
    private static final int CELL_W = 26;
    private static final int CELL_H = 28;
    /// Inventory slots (like the vanilla ones).
    private static final int SLOT = 18;
    /// The screen asks for the contents again every this many ticks (other players may change them).
    private static final int REFRESH_TICKS = 20;
    /// Colors (0xAARRGGBB).
    private static final int PANEL = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF8060C0;
    private static final int CELL_COLOR = 0xFF2A2A38;
    private static final int CELL_HOVER = 0xFF5A3A80;
    private static final int SLOT_COLOR = 0xFF20202C;
    private static final int SCROLL_BAR = 0xFF8060C0;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;

    private final BlockPos pos;
    private TerminalContents contents;
    /// The items shown (after the search), most first.
    private List<TerminalContents.Item> shown = List.of();
    private String search = "";
    private int scroll;
    private int ticks;

    private int left, top, panelWidth, panelHeight, rows, gridTop, inventoryTop;

    public TerminalScreen(TerminalContents contents) {
        super(Component.translatable("screen.simpleores.terminal.title"));
        this.pos = contents.pos();
        this.contents = contents;
        filter();
    }

    public BlockPos pos() {
        return pos;
    }

    /// New contents from the server.
    public void update(TerminalContents contents) {
        this.contents = contents;
        filter();
    }

    private void filter() {
        String text = search.trim().toLowerCase(Locale.ROOT);
        List<TerminalContents.Item> list = new ArrayList<>();
        for (TerminalContents.Item item : contents.items()) {
            if (text.isEmpty() || item.item().create().getHoverName().getString().toLowerCase(Locale.ROOT).contains(text)) {
                list.add(item);
            }
        }
        shown = list;
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        int totalRows = (shown.size() + COLUMNS - 1) / COLUMNS;
        return Math.max(0, totalRows - Math.max(1, rows));
    }

    private static void send(int action, Optional<net.minecraft.world.item.ItemStackTemplate> item, int slot, BlockPos pos) {
        ClientPacketDistributor.sendToServer(new TerminalAction(pos, action, item, slot));
    }

    // ---------------------------------------------------------------- layout and buttons

    @Override
    protected void init() {
        // Smaller windows show fewer rows of the network
        rows = height >= 300 ? 5 : height >= 270 ? 4 : 3;
        panelWidth = COLUMNS * CELL_W + 20;
        panelHeight = 40 + rows * CELL_H + 18 + 12 + 3 * SLOT + 4 + SLOT + 10;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        gridTop = top + 40;
        inventoryTop = gridTop + rows * CELL_H + 30;
        scroll = Mth.clamp(scroll, 0, maxScroll());

        EditBox searchBox = new EditBox(font, left + panelWidth - 10 - 110, top + 20, 110, 14, Component.translatable("screen.simpleores.terminal.search"));
        searchBox.setHint(Component.translatable("screen.simpleores.terminal.search"));
        searchBox.setValue(search);
        searchBox.setResponder(value -> {
            search = value;
            scroll = 0;
            filter();
        });
        addRenderableWidget(searchBox);

        int inventoryRight = left + 10 + COLUMNS * SLOT;
        addRenderableWidget(Button.builder(Component.translatable("screen.simpleores.terminal.store_all"),
                        b -> send(TerminalAction.INSERT_ALL, Optional.empty(), 0, pos))
                .bounds(inventoryRight + 8, inventoryTop, left + panelWidth - 10 - inventoryRight - 8, 20).build());
    }

    @Override
    public void tick() {
        // Every second: ask for the contents again (other players or machines may have changed them)
        if (++ticks % REFRESH_TICKS == 0) send(TerminalAction.REFRESH, Optional.empty(), 0, pos);
        // Too far from the terminal: close
        if (minecraft.player == null || minecraft.player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))
                > TerminalAction.RANGE * TerminalAction.RANGE) {
            onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) scroll = Mth.clamp(scroll + (scrollY > 0 ? -1 : 1), 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;

        // Network grid: take
        int index = cellAt(event.x(), event.y());
        if (index >= 0) {
            int action = event.button() == 1 ? TerminalAction.TAKE_ONE : TerminalAction.TAKE_STACK;
            send(action, Optional.of(shown.get(index).item()), 0, pos);
            return true;
        }
        // Inventory: store
        int slot = slotAt(event.x(), event.y());
        if (slot >= 0 && minecraft.player != null && !minecraft.player.getInventory().getItem(slot).isEmpty()) {
            send(TerminalAction.INSERT_SLOT, Optional.empty(), slot, pos);
            return true;
        }
        return false;
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
        graphics.text(font, Component.translatable("screen.simpleores.terminal.cells", contents.cells()), left + 10, top + 23, TEXT_DIM);

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

        // Tooltips
        if (hovered >= 0) {
            TerminalContents.Item item = shown.get(hovered);
            ItemStack stack = item.item().create();
            graphics.setTooltipForNextFrame(font, List.of(stack.getHoverName(),
                            Component.translatable("screen.simpleores.terminal.stored", String.format(Locale.ROOT, "%,d", item.count())),
                            Component.translatable("screen.simpleores.terminal.take_hint")),
                    Optional.empty(), mouseX, mouseY);
        } else if (hoveredSlot >= 0 && minecraft.player != null && !minecraft.player.getInventory().getItem(hoveredSlot).isEmpty()) {
            graphics.setTooltipForNextFrame(font, List.of(minecraft.player.getInventory().getItem(hoveredSlot).getHoverName(),
                    Component.translatable("screen.simpleores.terminal.store_hint")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
