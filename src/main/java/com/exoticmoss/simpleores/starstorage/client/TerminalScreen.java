package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.cell.CellTier;
import com.exoticmoss.simpleores.starstorage.terminal.CraftRequest;
import com.exoticmoss.simpleores.starstorage.cell.FluidContents;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalAction;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalContents;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalLayout;
import com.exoticmoss.simpleores.starstorage.network.NetworkMenu;
import com.exoticmoss.simpleores.starstorage.quasar.Pattern;
import com.exoticmoss.simpleores.util.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.exoticmoss.simpleores.util.gui.KeyEvent;
import com.exoticmoss.simpleores.util.gui.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.exoticmoss.simpleores.util.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

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
public class TerminalScreen<M extends AbstractContainerMenu & NetworkMenu> extends AbstractContainerScreen<M> implements JeiTarget.Source {
    /// Rows of the cells panel.
    private static final int CELL_ROWS = 5;
    /// The screen asks for the contents again every this many ticks (other players may change them).
    private static final int REFRESH_TICKS = 20;
    /// Colors (0xAARRGGBB).
    private static final int PANEL = 0xE0101018;
    protected static final int PANEL_BORDER = 0xFF8060C0;
    private static final int CELL_COLOR = 0xFF2A2A38;
    protected static final int CELL_HOVER = 0xFF5A3A80;
    protected static final int SLOT_COLOR = 0xFF20202C;
    private static final int FLUID_COLOR = 0xFF3FA0FF;
    private static final int FLUID_TEXT = 0xFF8FD0FF;
    private static final int XP_COLOR = 0xFF80FF20;
    private static final int RESULT_COLOR = 0xFF34344A;
    private static final int BAR_BACK = 0xFF15151E;
    private static final int SCROLL_BAR = 0xFF8060C0;
    protected static final int TEXT = 0xFFFFFFFF;
    protected static final int TEXT_DIM = 0xFFA0A0B0;

    /// How the items are sorted (kept while the game is open).
    private static final String[] SORT_KEYS = {"most", "least", "a_z", "z_a"};
    private static int sort = 0;

    protected final TerminalLayout layout;
    protected TerminalContents contents;
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
    /// Craft panel: the recipe chosen (null = automatic: the first one that has everything).
    private Pattern craftChoice;
    private static final int OPTION = 20;
    private static final int AUTO_W = 30;
    private static final int MAX_OPTIONS = 7;
    private Button clearButton;

    public TerminalScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = TerminalLayout.WIDTH;
        this.imageHeight = menu.layout().height();
        this.layout = menu.layout();
        this.contents = TerminalClient.last(menu.networkPos());
        filter();
    }

    // ---------------------------------------------------------------- what the Quasar changes (see QuasarScreen)

    /// True: the items of the Quasars can be crafted here ("Craft"). The Quasar screen only makes recipes.
    protected boolean crafting() {
        return true;
    }

    /// The top left part: how full the cells are (the Quasar shows its recipes there).
    protected void drawTopLeft(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawCells(graphics);
    }

    /// Mouse wheel / click on the top left part. true = done.
    protected boolean scrollTopLeft(double x, double y, int step) {
        if (!isOverCells(x, y)) return false;
        cellScroll = Mth.clamp(cellScroll + step, 0, Math.max(0, contents.cells().size() - CELL_ROWS));
        return true;
    }

    protected boolean clickTopLeft(MouseButtonEvent event) {
        return false;
    }

    /// The small X of the grid: puts the items of the crafting grid back in the network.
    protected void clearGrid() {
        send(TerminalAction.CLEAR_GRID, Optional.empty());
    }

    /// The text above the grid.
    protected Component gridLabel() {
        return Component.translatable("screen.simpleores.terminal.crafting");
    }

    /// More widgets (the Quasar adds its "Save" button).
    protected void addMoreWidgets() {
    }

    /// True: the middle shows the items of the network (the Quasar shows its auto-crafts there in a tab).
    protected boolean showNetwork() {
        return true;
    }

    /// The middle part when it doesn't show the network. Returns nothing hovered.
    protected void drawMiddle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    protected boolean clickMiddle(MouseButtonEvent event) {
        return true;
    }

    protected int networkTopY() {
        return networkTop();
    }

    /// The crafting grid (or the recipe of the Quasar) is shown (not in the auto-crafts tab of the Quasar).
    protected boolean showGrid() {
        return true;
    }

    /// The part of the top border that is not drawn {from x, to x} (where a tab joins the panel), or null.
    /// The space at the left of the top of the panel used by the tabs of the screen (the tabs of the network are
    /// on the right).
    protected int tabsReserved() {
        return 0;
    }

    protected int @Nullable [] topGap() {
        return null;
    }

    /// Drawn after everything (the Quasar draws its tabs and tooltips).
    protected void drawMore(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
        // The liquids: shown as their buckets (with the mB)
        for (FluidContents.Entry fluid : contents.fluids()) {
            ItemStack bucket = StarFluids.bucket(fluid.fluid());
            if (bucket.isEmpty()) bucket = new ItemStack(Items.BUCKET);
            all.add(new TerminalContents.Item(ItemStackTemplate.fromNonEmptyStack(bucket), fluid.amount(), fluid.fluid()));
        }
        // The items that can be crafted and that the network doesn't have: shown with "Craft"
        for (ItemStackTemplate craftable : crafting() ? contents.craftables() : List.<ItemStackTemplate>of()) {
            ItemStack stack = craftable.create();
            if (all.stream().noneMatch(item -> item.fluid() == null && ItemStack.isSameItemSameComponents(item.item().create(), stack))) {
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
        if (item.fluid() != null) return StarFluids.name(item.fluid()).getString();
        return item.item().create().getHoverName().getString();
    }

    private int maxScroll() {
        int totalRows = (shown.size() + TerminalLayout.COLUMNS - 1) / TerminalLayout.COLUMNS;
        return Math.max(0, totalRows - layout.rows());
    }

    protected void send(int action, Optional<ItemStackTemplate> item) {
        PacketDistributor.sendToServer(new TerminalAction(menu.networkPos(), action, item));
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
        clearButton = addRenderableWidget(Button.builder(Component.empty(), b -> clearGrid())
                .bounds(gridRight - 11, topPos + layout.inventoryTop() - 12, 11, 11).build());
        addMoreWidgets();
    }

    private boolean isCraftable(ItemStackTemplate item) {
        if (!crafting()) return false;
        ItemStack stack = item.create();
        return contents.craftables().stream().anyMatch(craftable -> ItemStack.isSameItemSameComponents(craftable.create(), stack));
    }

    // ---------------------------------------------------------------- craft panel

    private static final int CRAFT_W = 200;
    /// Missing items: drawn as items, this many per row (with the amount under each one).
    private static final int MISSING_COLUMNS = 8;
    private static final int MISSING_CELL = 24;
    private List<CraftRequest.Missing> missingItems = List.of();

    /// How many rows of missing items fit above the inventory.
    private int missingRows() {
        int rows = (missingItems.size() + MISSING_COLUMNS - 1) / MISSING_COLUMNS;
        int fit = Math.max(1, (layout.inventoryTop() - 14 - 18 - 84) / (MISSING_CELL + 4));
        return Math.min(rows, fit);
    }

    /// Height of the craft panel: grows with the missing items.
    private int craftHeight() {
        int rows = missingRows();
        return 72 + optionsHeight() + (rows > 0 ? 12 + rows * (MISSING_CELL + 4) : 0);
    }

    /// The recipes for the item of the craft panel (more than one: the player chooses).
    private List<Pattern> craftOptions() {
        if (craftItem == null) return List.of();
        ItemStack stack = craftItem.create();
        List<Pattern> list = contents.recipes().stream().filter(recipe -> recipe.makes(stack)).toList();
        return list.size() > MAX_OPTIONS ? list.subList(0, MAX_OPTIONS) : list;
    }

    /// The row of recipes (only when there is more than one).
    private int optionsHeight() {
        return craftOptions().size() > 1 ? OPTION + 6 : 0;
    }

    /// The box of option "i" of the recipe row (-1 = "Auto"): {x, y, width}.
    private int[] optionBox(int i) {
        int x = craftLeft() + 10, y = craftTop() + 56;
        return i < 0 ? new int[]{x, y, AUTO_W} : new int[]{x + AUTO_W + 4 + i * (OPTION + 2), y, OPTION};
    }

    /// The ingredient shown for a recipe: the one with the most items.
    private static ItemStack mainInput(Pattern recipe) {
        ItemStack best = ItemStack.EMPTY;
        int bestAmount = 0;
        for (int i = 0; i < recipe.inputs().size(); i++) {
            if (recipe.inputs().get(i).isEmpty()) continue;
            int amount = recipe.amount(i);
            // the same item in many slots of the crafting grid counts together
            ItemStack item = recipe.inputs().get(i).get(0).create();
            for (int j = 0; j < recipe.inputs().size(); j++) {
                if (j != i && !recipe.inputs().get(j).isEmpty() && ItemStack.isSameItemSameComponents(recipe.inputs().get(j).get(0).create(), item)) {
                    amount += recipe.amount(j);
                }
            }
            if (amount > bestAmount) {
                best = item;
                bestAmount = amount;
            }
        }
        return best;
    }

    private int craftLeft() {
        return leftPos + (imageWidth - CRAFT_W) / 2;
    }

    private int craftTop() {
        return topPos + 18;
    }

    private void openCraft(ItemStackTemplate item) {
        closeCraft();
        craftItem = item;
        craftChoice = null;
        craftMessage.clear();
        missingItems = List.of();
        int x = craftLeft(), y = craftTop();
        amountBox = addRenderableWidget(new EditBox(font, x + 10, y + 34, 70, 16, Component.empty()));
        amountBox.setMaxLength(13);
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
        long amount;
        try {
            amount = Long.parseLong(amountBox.getValue().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            amount = 0;
        }
        if (amount <= 0) return;
        craftMessage.clear();
        missingItems = List.of();
        craftMessage.add(Component.translatable("screen.simpleores.terminal.craft.working"));
        PacketDistributor.sendToServer(new CraftRequest(menu.networkPos(), craftItem, amount, Optional.ofNullable(craftChoice)));
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
        missingItems = reply.missing();
    }

    private void drawCraftPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = craftLeft(), y = craftTop();
        graphics.fill(x, y, x + CRAFT_W, y + craftHeight(), 0xF0161622);
        graphics.outline(x, y, CRAFT_W, craftHeight(), PANEL_BORDER);
        ItemStack stack = craftItem.create();
        graphics.item(stack, x + 10, y + 9);
        graphics.text(font, Component.translatable("screen.simpleores.terminal.craft.title", stack.getHoverName()), x + 32, y + 13, TEXT);
        drawCraftOptions(graphics, mouseX, mouseY);
        for (int i = 0; i < craftMessage.size(); i++) {
            graphics.text(font, craftMessage.get(i), x + 10, y + 58 + optionsHeight() + i * 10, TEXT_DIM);
        }
        // The missing items, drawn as items with the amount under each one
        int shownMissing = Math.min(missingItems.size(), missingRows() * MISSING_COLUMNS);
        if (shownMissing < missingItems.size()) {
            graphics.text(font, Component.literal("+" + (missingItems.size() - shownMissing + 1)), x + CRAFT_W - 30, y + 58 + optionsHeight(), 0xFFFF9090);
            shownMissing--;
        }
        for (int i = 0; i < shownMissing; i++) {
            CraftRequest.Missing missing = missingItems.get(i);
            int mx = x + 4 + (i % MISSING_COLUMNS) * MISSING_CELL, my = y + 70 + optionsHeight() + (i / MISSING_COLUMNS) * (MISSING_CELL + 4);
            graphics.fill(mx + 1, my + 1, mx + MISSING_CELL - 1, my + MISSING_CELL + 3, 0xFF3A1E24);
            graphics.item(missing.item(), mx + (MISSING_CELL - 16) / 2, my + 2);
            graphics.centeredText(font, Component.literal(StarNetwork.format(missing.count())), mx + MISSING_CELL / 2, my + 18, 0xFFFF9090);
            if (mouseX >= mx && mouseX < mx + MISSING_CELL && mouseY >= my && mouseY < my + MISSING_CELL + 4) {
                graphics.setTooltipForNextFrame(font, List.of(missing.item().getHoverName(),
                        Component.literal(String.format(Locale.ROOT, "%,d", missing.count()))), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    /// The recipes to choose from: "Auto", then one box per recipe (its main ingredient, and a dot with the kind:
    /// brown = crafting table, orange = Gravity Core, gray = furnace).
    private void drawCraftOptions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Pattern> options = craftOptions();
        if (options.size() <= 1) return;
        for (int i = -1; i < options.size(); i++) {
            int[] box = optionBox(i);
            boolean over = mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + OPTION;
            boolean chosen = i < 0 ? craftChoice == null : options.get(i).equals(craftChoice);
            graphics.fill(box[0], box[1], box[0] + box[2], box[1] + OPTION, over ? CELL_HOVER : SLOT_COLOR);
            graphics.outline(box[0], box[1], box[2], OPTION, chosen ? 0xFF40C070 : PANEL_BORDER);
            if (i < 0) {
                graphics.centeredText(font, Component.translatable("screen.simpleores.terminal.craft.auto"), box[0] + box[2] / 2, box[1] + 6, TEXT);
                if (over) {
                    graphics.setTooltipForNextFrame(font, List.of(Component.translatable("screen.simpleores.terminal.craft.auto_info")),
                            Optional.empty(), mouseX, mouseY);
                }
                continue;
            }
            Pattern recipe = options.get(i);
            graphics.item(mainInput(recipe), box[0] + 2, box[1] + 2);
            int dot = recipe.isCore() ? 0xFFFF9A2A : recipe.furnace() ? 0xFFA0A0A0 : 0xFFA0703A;
            graphics.fill(box[0] + box[2] - 5, box[1] + 2, box[0] + box[2] - 2, box[1] + 5, dot);
            if (over) {
                List<Component> lines = new ArrayList<>();
                String kind = recipe.isCore() ? "core" : recipe.furnace() ? "furnace" : "crafting";
                lines.add(Component.translatable("screen.simpleores.quasar.tab." + kind).append(" -> " + recipe.count() + " x ")
                        .append(recipe.output().create().getHoverName()));
                for (int input = 0; input < recipe.inputs().size(); input++) {
                    List<ItemStackTemplate> slot = recipe.inputs().get(input);
                    if (slot.isEmpty()) continue;
                    String amount = recipe.amount(input) > 1 ? recipe.amount(input) + " x " : "";
                    lines.add(Component.literal(" - " + amount).append(slot.get(0).create().getHoverName()));
                }
                graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    /// A click on the row of recipes: chooses one. false = not on a box.
    private boolean clickCraftOption(double x, double y) {
        List<Pattern> options = craftOptions();
        if (options.size() <= 1) return false;
        for (int i = -1; i < options.size(); i++) {
            int[] box = optionBox(i);
            if (x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + OPTION) {
                craftChoice = i < 0 ? null : options.get(i);
                return true;
            }
        }
        return false;
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
        if (!scrollTopLeft(x, y, step)) scroll = Mth.clamp(scroll + step, 0, maxScroll());
        return true;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (NetworkTabs.click(event.x(), event.y(), leftPos, topPos, imageWidth, tabsReserved(), menu.networkPos())) return true;
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
            if (event.x() >= x && event.x() < x + CRAFT_W && event.y() >= y && event.y() < y + craftHeight()) {
                if (clickCraftOption(event.x(), event.y())) return true;
                return super.mouseClicked(event.x(), event.y(), event.button());
            }
            return true;
        }
        if (clickTopLeft(event)) return true;
        // Another view in the middle (the Quasar's auto-crafts): the whole width of the panel (its X buttons are on the right)
        if (!showNetwork() && event.x() >= leftPos && event.x() < leftPos + imageWidth
                && event.y() >= networkTop() && event.y() < networkTop() + layout.rows() * TerminalLayout.CELL_H) {
            return clickMiddle(event);
        }
        if (isOverExperience(event.x(), event.y())) {
            send(TerminalAction.TAKE_XP, Optional.empty());
            return true;
        }
        if (isOverNetwork(event.x(), event.y())) {
            boolean right = event.button() == 1;
            int fluidIndex = cellAt(event.x(), event.y());
            if (fluidIndex >= 0 && shown.get(fluidIndex).fluid() != null
                    && (menu.getCarried().isEmpty() || menu.getCarried().is(Items.BUCKET))) {
                // A liquid: fills a bucket (from the cursor, the network or the inventory)
                send(TerminalAction.TAKE_FLUID, Optional.of(shown.get(fluidIndex).item()));
                return true;
            }
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
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    /// While writing in the search box, the keys write (the inventory key doesn't close the screen).
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean keyPressed(KeyEvent event) {
        if (craftItem != null) {
            // Craft panel: Esc closes it, Enter starts, the rest writes the amount
            if (event.isEscape()) closeCraft();
            else if (event.isConfirmation()) startCraft();
            else if (amountBox != null) amountBox.keyPressed(event.key(), event.scancode(), event.modifiers());
            return true;
        }
        if (searchBox != null && searchBox.isFocused() && !event.isEscape()) {
            // Enter: stop writing in the search box
            if (event.isConfirmation()) {
                searchBox.setFocused(false);
                setFocused(null);
                return true;
            }
            searchBox.keyPressed(event.key(), event.scancode(), event.modifiers());
            return true;
        }
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    private int experienceY() {
        return networkTop() + layout.rows() * TerminalLayout.CELL_H + 6;
    }

    private String experienceText() {
        return StarNetwork.format(contents.experience()) + " XP";
    }

    private boolean isOverExperience(double x, double y) {
        return contents.experience() > 0 && x >= networkLeft() && x < networkLeft() + font.width(experienceText())
                && y >= experienceY() - 1 && y < experienceY() + 9;
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

    /// JEI: the item (or liquid) drawn under the mouse: an item of the network, or of the craft panel.
    @Override
    public @Nullable JeiTarget jeiTargetAt(double mouseX, double mouseY) {
        if (craftItem != null) {
            int x = craftLeft(), y = craftTop();
            if (mouseX >= x + 10 && mouseX < x + 26 && mouseY >= y + 9 && mouseY < y + 25) return JeiTarget.of(craftItem.create(), x + 10, y + 9);
            // The recipes to choose from (their main ingredient)
            List<Pattern> options = craftOptions();
            for (int i = 0; options.size() > 1 && i < options.size(); i++) {
                int[] box = optionBox(i);
                if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + OPTION) {
                    return JeiTarget.of(mainInput(options.get(i)), box[0] + 2, box[1] + 2);
                }
            }
            // The missing items
            int shownMissing = Math.min(missingItems.size(), missingRows() * MISSING_COLUMNS);
            for (int i = 0; i < shownMissing; i++) {
                int mx = x + 4 + (i % MISSING_COLUMNS) * MISSING_CELL, my = y + 70 + optionsHeight() + (i / MISSING_COLUMNS) * (MISSING_CELL + 4);
                if (mouseX >= mx && mouseX < mx + MISSING_CELL && mouseY >= my && mouseY < my + MISSING_CELL + 4) {
                    return JeiTarget.of(missingItems.get(i).item().copyWithCount(1), mx + (MISSING_CELL - 16) / 2, my + 2);
                }
            }
            return null;
        }
        if (!showNetwork()) return null;
        int index = cellAt(mouseX, mouseY);
        if (index < 0) return null;
        TerminalContents.Item item = shown.get(index);
        int visible = index - scroll * TerminalLayout.COLUMNS;
        int x = networkLeft() + (visible % TerminalLayout.COLUMNS) * TerminalLayout.CELL_W + (TerminalLayout.CELL_W - 16) / 2;
        int y = networkTop() + (visible / TerminalLayout.COLUMNS) * TerminalLayout.CELL_H + 3;
        return new JeiTarget(item.item().create(), item.fluid(), x, y, 16);
    }

    protected int cellsLeft() {
        return leftPos + 10;
    }

    protected int cellsWidth() {
        return 160;
    }

    protected boolean isOverCells(double x, double y) {
        return x >= cellsLeft() && x < cellsLeft() + cellsWidth() && y >= topPos + TerminalLayout.TOP && y < networkTop();
    }

    // ---------------------------------------------------------------- drawing

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        // The tabs of the other blocks of the network (top right)
        NetworkTabs.draw(graphics, font, leftPos, topPos, imageWidth, tabsReserved(), menu.networkPos(), mouseX, mouseY);

        // Panel
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        // The border; the top line has a gap where a tab joins the panel (the Quasar)
        int[] gap = topGap();
        if (gap == null) {
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, PANEL_BORDER);
        } else {
            graphics.fill(leftPos, topPos, gap[0], topPos + 1, PANEL_BORDER);
            graphics.fill(gap[1], topPos, leftPos + imageWidth, topPos + 1, PANEL_BORDER);
        }
        graphics.fill(leftPos, topPos + imageHeight - 1, leftPos + imageWidth, topPos + imageHeight, PANEL_BORDER);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, PANEL_BORDER);
        graphics.fill(leftPos + imageWidth - 1, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL_BORDER);
        graphics.centeredText(font, title, leftPos + imageWidth / 2, topPos + 7, TEXT);

        if (craftItem == null) drawTopLeft(graphics, mouseX, mouseY);
        int hovered = -1;
        if (showNetwork()) hovered = drawNetwork(graphics, mouseX, mouseY);
        else drawMiddle(graphics, mouseX, mouseY);
        if (craftItem != null) {
            hovered = -1;
            drawCraftPanel(graphics, mouseX, mouseY);
        }

        // The experience of the auto-crafts (click: the player gets it)
        if (contents.experience() > 0) {
            boolean over = isOverExperience(mouseX, mouseY);
            graphics.text(font, Component.literal(experienceText()), networkLeft(), experienceY(), over ? 0xFFFFFF80 : XP_COLOR);
        }

        // Inventory and crafting: the frames of the slots (the items are drawn by the screen itself)
        int top = topPos + layout.inventoryTop();
        graphics.text(font, Component.translatable("screen.simpleores.terminal.inventory"), leftPos + TerminalLayout.inventoryLeft(), top - 11, TEXT_DIM);
        if (showGrid()) graphics.text(font, gridLabel(), leftPos + TerminalLayout.craftLeft(), top - 11, TEXT_DIM);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
            if (slot.index == menu.resultSlot()) {
                graphics.fill(x - 2, y - 2, x + TerminalLayout.SLOT + 2, y + TerminalLayout.SLOT + 2, RESULT_COLOR);
            }
            graphics.fill(x + 1, y + 1, x + TerminalLayout.SLOT - 1, y + TerminalLayout.SLOT - 1, SLOT_COLOR);
        }
        // Arrow between the crafting grid and the result
        if (showGrid()) {
            int ax = leftPos + TerminalLayout.craftLeft() + 3 * TerminalLayout.SLOT + 4, ay = topPos + layout.resultY() + 8;
            graphics.fill(ax, ay, ax + 12, ay + 2, TEXT_DIM);
            for (int i = 0; i < 4; i++) graphics.fill(ax + 8 + i, ay - 3 + i, ax + 9 + i, ay + 5 - i, TEXT_DIM);
        }
        if (clearButton != null) clearButton.visible = showGrid();

        super.render(graphics.graphics(), mouseX, mouseY, a);
        graphics.afterItems();

        // The X of the button that empties the crafting grid (5x5 pixels, in the middle of the button)
        if (clearButton != null && clearButton.visible) {
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
            if (item.fluid() != null) {
                lines.add(StarFluids.name(item.fluid()));
                lines.add(Component.literal(String.format(Locale.ROOT, "%,d mB", item.count())).withColor(FLUID_TEXT));
                graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
                drawMore(graphics, mouseX, mouseY);
                return;
            }
            lines.add(item.item().create().getHoverName());
            lines.add(Component.translatable("screen.simpleores.terminal.stored", String.format(Locale.ROOT, "%,d", item.count())));
            if (item.count() > 0 && isCraftable(item.item())) lines.add(Component.translatable("screen.simpleores.terminal.craft.ctrl").withColor(0xFF7FE0C0));
            graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
        }
        drawMore(graphics, mouseX, mouseY);
    }

    /// The title and "Inventory" are drawn above (in extractRenderState).
    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
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
                if (item.fluid() != null) {
                    // A liquid: a small drop in the corner, and the buckets
                    graphics.fill(x + 3, y + 3, x + 5, y + 7, FLUID_COLOR);
                    graphics.fill(x + 2, y + 5, x + 6, y + 7, FLUID_COLOR);
                    String amount = item.count() < StarFluids.BUCKET ? item.count() + "mB" : StarNetwork.format(item.count() / StarFluids.BUCKET) + "B";
                    graphics.centeredText(font, Component.literal(amount), x + TerminalLayout.CELL_W / 2, y + TerminalLayout.CELL_H - 10, FLUID_TEXT);
                } else if (item.count() == 0) {
                    TinyText.craft(graphics, x + (TerminalLayout.CELL_W - TinyText.CRAFT_WIDTH) / 2, y + TerminalLayout.CELL_H - 8, 0xFF7FE0C0);
                } else {
                    graphics.centeredText(font, Component.literal(StarNetwork.format(item.count())), x + TerminalLayout.CELL_W / 2, y + TerminalLayout.CELL_H - 10, TEXT);
                }
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
    protected void drawCells(GuiGraphicsExtractor graphics) {
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
            double capacity = cell.fluid() ? tier.fluidCapacity : tier.capacity;
            double full = capacity <= 0 ? 0 : Math.min(1, cell.used() / capacity);
            String percent = full > 0 && full < 0.01 ? "<1%" : Math.round(full * 100) + "%";
            int barLeft = x + 9, barRight = x + width - font.width("100%") - 4;
            graphics.fill(x, ry + 1, x + 6, ry + 7, 0xFF000000 | (cell.fluid() ? tier.fluidGlowColor : tier.glowColor));
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
