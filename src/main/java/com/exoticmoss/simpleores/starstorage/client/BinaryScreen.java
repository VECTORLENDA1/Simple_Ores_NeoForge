package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.binary.BinaryAction;
import com.exoticmoss.simpleores.starstorage.binary.BinaryMenu;
import com.exoticmoss.simpleores.starstorage.binary.BinaryView;
import com.exoticmoss.simpleores.starstorage.binary.SlotRule;
import com.exoticmoss.simpleores.starstorage.binary.SlotRoles;
import com.exoticmoss.simpleores.item.ModItems;
import com.exoticmoss.simpleores.starstorage.binary.BinaryStarBlockEntity;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;
import net.minecraft.world.level.material.Fluid;
import com.exoticmoss.simpleores.util.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import com.exoticmoss.simpleores.util.gui.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/// The screen of the Binary Star: every block with an inventory around it (chests, furnaces...),
/// one card per block with all its slots. Mouse wheel (or the bar on the right) scrolls the cards.
///  - Left-click on a slot: next mode: take (orange) -> keep full (green) -> input and output (light purple) -> nothing.
///    With an item in the mouse cursor (taken from the inventory below): the slot keeps that item.
///  - Right-click on a slot: no rule.
///  - "Take all": every slot of the block is TAKE. "Clear": no rules for the block.
/// The screen only asks the server (BinaryAction); the server changes the rules and sends back the view.
public class BinaryScreen extends AbstractContainerScreen<BinaryMenu> implements JeiTarget.Source {
    private static final int WIDTH = BinaryMenu.WIDTH;
    private static final int SLOT = 18;
    private static final int COLUMNS = 12;
    private static final int CARD_HEADER = 20;
    private static final int CARD_GAP = 4;
    /// The screen asks for the view again every this many ticks (the slots change all the time).
    private static final int REFRESH_TICKS = 20;
    /// Colors (0xAARRGGBB).
    private static final int PANEL = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF8060C0;
    private static final int CARD = 0xFF1C1C28;
    private static final int SLOT_COLOR = 0xFF2A2A38;
    private static final int SLOT_HOVER = 0xFF5A3A80;
    /// The outline of the upgrade slot (like the range slot of the Magnetar).
    private static final int RANGE_COLOR = 0xFFC060E0;
    /// Inputs and outputs of the machines (the background and the line at the bottom of the slots).
    private static final int INPUT_BACK = 0xFF223A28;
    private static final int OUTPUT_BACK = 0xFF3C2228;
    private static final int INPUT_LINE = 0xFF48C060;
    private static final int OUTPUT_LINE = 0xFFD04848;
    private static final int TANK_COLOR = 0xFF1E3048;
    private static final int TANK_TEXT = 0xFF8FD0FF;
    private static final int TAKE_COLOR = 0xFFE08A30;
    private static final int KEEP_COLOR = 0xFF40C070;
    private static final int BOTH_COLOR = 0xFFC9A6FF;
    private static final int BUTTON = 0xFF3A3A4E;
    private static final int BUTTON_HOVER = 0xFF55557A;
    private static final int BUTTON_BORDER = 0xFF70709A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;

    private BinaryView view;
    /// First card shown.
    /// How far the cards are scrolled (pixels): where they are now, and where they go (they slide there).
    private double scroll;
    private double scrollTarget;
    /// Pixels moved by one turn of the mouse wheel.
    private static final int WHEEL_STEP = 30;
    private int ticks;
    private int left, top, height2;
    private final BlockPos pos;

    /// Where one card is on the screen (only the cards that fit are placed).
    private record Card(int index, int x, int y, int height) {
    }

    public BinaryScreen(BinaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BinaryMenu.WIDTH, menu.height);
        this.pos = menu.pos;
        this.view = BinaryClient.last(menu.pos);
    }

    private Button areaButton;

    private Component areaLabel() {
        return Component.translatable(AreaDisplay.isShown(pos) ? "screen.simpleores.area.hide" : "screen.simpleores.area.show");
    }

    public BlockPos pos() {
        return pos;
    }

    public void update(BinaryView view) {
        this.view = view;
        AreaDisplay.update(pos, view.radius());
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll());
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private void send(BlockPos block, int action, int slot) {
        PacketDistributor.sendToServer(new BinaryAction(pos, block, action, slot));
    }

    @Override
    protected void init() {
        super.init();
        height2 = imageHeight;
        left = leftPos;
        top = topPos;
        // Shows (or hides) the area the Binary Star reaches, around it in the world
        areaButton = addRenderableWidget(Button.builder(areaLabel(), b -> {
            AreaDisplay.toggle(pos, view.radius());
            b.setMessage(areaLabel());
        }).bounds(left + WIDTH - 66, top + 4, 60, 14).build());
    }

    @Override
    protected void containerTick() {
        if (++ticks % REFRESH_TICKS == 1) send(pos, BinaryAction.REFRESH, 0);
        jeiTick.run();
    }

    /// JEI: runs every tick while the screen is open (see BinaryGhostHandler).
    public static Runnable jeiTick = () -> {
    };

    /// A slot (or tank) shown on the screen where an item can be dropped from JEI: the block, the slot number of
    /// the rules (tanks: BinaryStarBlockEntity.TANKS + tank) and where it is.
    public record DropSlot(BlockPos block, int slot, boolean tank, int x, int y) {
    }

    /// JEI: the slots that can be seen whole now.
    public List<DropSlot> dropSlots() {
        List<DropSlot> list = new ArrayList<>();
        if (linkingBlock() != null) return list;
        for (Card card : cards()) {
            BinaryView.Block block = view.blocks().get(card.index());
            for (int slot = 0; slot < cells(block); slot++) {
                int x = slotX(card, slot), y = slotY(card, slot);
                if (y < areaTop() || y + SLOT > areaBottom()) continue;
                list.add(new DropSlot(block.pos(), slotId(block, slot), slot >= block.slots().size(), x + 1, y + 1));
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- where things are

    private int areaTop() {
        return top + 22;
    }

    private int areaBottom() {
        return top + height2 - BinaryMenu.INVENTORY_HEIGHT - 4;
    }

    /// The slots and then the tanks of a block (all drawn as squares).
    private static int cells(BinaryView.Block block) {
        return block.slots().size() + block.tanks().size();
    }

    /// The slot number (for the rules) of square "cell": the tanks are BinaryStarBlockEntity.TANKS + tank.
    private static int slotId(BinaryView.Block block, int cell) {
        return cell < block.slots().size() ? cell : BinaryStarBlockEntity.TANKS + cell - block.slots().size();
    }

    private static int rows(BinaryView.Block block) {
        return Math.max(1, (cells(block) + COLUMNS - 1) / COLUMNS);
    }

    /// The cards that fit, from the first one shown.
    /// JEI: the block of a card, the items of its slots and the liquids of its tanks.
    @Override
    public @Nullable JeiTarget jeiTargetAt(double mouseX, double mouseY) {
        if (mouseY < areaTop() || mouseY >= areaBottom()) return null;
        BinaryView.Block linkingBlock = linkingBlock();
        if (linkingBlock != null) {
            for (int i = 0; i < linkingBlock.options().size(); i++) {
                int[] at = optionAt(i);
                if (mouseX >= at[0] && mouseX < at[0] + SLOT && mouseY >= at[1] && mouseY < at[1] + SLOT) {
                    return JeiTarget.of(linkingBlock.options().get(i).outputStack().copyWithCount(1), at[0] + 1, at[1] + 1);
                }
            }
            return null;
        }
        for (Card card : cards()) {
            BinaryView.Block block = view.blocks().get(card.index());
            if (mouseX >= card.x() + 2 && mouseX < card.x() + 18 && mouseY >= card.y() + 2 && mouseY < card.y() + 18) {
                return JeiTarget.of(block.icon(), card.x() + 2, card.y() + 2);
            }
            for (int slot = 0; slot < cells(block); slot++) {
                int x = slotX(card, slot), y = slotY(card, slot);
                if (mouseX < x || mouseX >= x + SLOT || mouseY < y || mouseY >= y + SLOT) continue;
                if (slot < block.slots().size()) return JeiTarget.of(block.slots().get(slot).copyWithCount(1), x + 1, y + 1);
                ItemStack bucket = block.tanks().get(slot - block.slots().size()).bucket();
                Fluid fluid = StarFluids.fromBucket(bucket);
                return fluid == null ? null : new JeiTarget(bucket, fluid, x + 1, y + 1, 16);
            }
        }
        return null;
    }

    private List<Card> cards() {
        List<Card> cards = new ArrayList<>();
        int y = areaTop() - (int) Math.round(scroll);
        for (int i = 0; i < view.blocks().size(); i++) {
            int cardHeight = cardHeight(i);
            // Only the cards that can be seen (a card cut by the top or the bottom too)
            if (y + cardHeight > areaTop() && y < areaBottom()) cards.add(new Card(i, left + 10, y, cardHeight));
            y += cardHeight + CARD_GAP;
        }
        return cards;
    }

    private static int slotX(Card card, int slot) {
        return card.x() + (WIDTH - 20 - COLUMNS * SLOT) / 2 + (slot % COLUMNS) * SLOT;
    }

    private static int slotY(Card card, int slot) {
        return card.y() + CARD_HEADER + (slot / COLUMNS) * SLOT;
    }

    /// The two buttons of a card: 0 = "Take all", 1 = "Clear". {x, y, width, height}
    /// 2 = "Recipes" (only for machines that can make recipes of the Quasars).
    private static int[] button(Card card, int which) {
        if (which == 2) {
            int[] take = button(card, 0);
            int recipesWidth = 48;
            return new int[]{take[0] - 3 - recipesWidth, take[1], recipesWidth, 14};
        }
        int clearWidth = 36, takeWidth = 52;
        int right = card.x() + WIDTH - 20 - 2;
        return which == 1 ? new int[]{right - clearWidth, card.y() + 3, clearWidth, 14}
                : new int[]{right - clearWidth - 3 - takeWidth, card.y() + 3, takeWidth, 14};
    }

    private static boolean inside(int[] box, double x, double y) {
        return x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3];
    }

    // ---------------------------------------------------------------- mouse

    // ---------------------------------------------------------------- the bar

    private static final int BAR_WIDTH = 4;
    /// True while the bar is being dragged with the mouse.
    private boolean dragging;

    private int barX() {
        return left + WIDTH - 8;
    }

    private int cardHeight(int index) {
        return CARD_HEADER + rows(view.blocks().get(index)) * SLOT + 6;
    }

    /// The height of all the cards together.
    private int contentHeight() {
        int height = 0;
        for (int i = 0; i < view.blocks().size(); i++) height += cardHeight(i) + CARD_GAP;
        return Math.max(0, height - CARD_GAP);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (areaBottom() - areaTop()));
    }

    /// The bar is shown when some cards don't fit.
    private boolean hasBar() {
        return maxScroll() > 0;
    }

    /// The part of the bar that is moved: {top, height}.
    private int[] thumb() {
        int height = areaBottom() - areaTop();
        int size = Math.max(10, height * height / Math.max(1, contentHeight()));
        int max = maxScroll();
        int y = areaTop() + (int) (max == 0 ? 0 : (height - size) * scroll / max);
        return new int[]{y, size};
    }

    /// Moves the cards to where the mouse is on the bar (they slide there).
    private void dragTo(double mouseY) {
        int[] thumb = thumb();
        int height = areaBottom() - areaTop() - thumb[1];
        double part = height <= 0 ? 0 : (mouseY - areaTop() - thumb[1] / 2.0) / height;
        scrollTarget = Mth.clamp(part * maxScroll(), 0, maxScroll());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        return dragged(event, dragX, dragY);
    }

    private boolean dragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            dragTo(event.y());
            return true;
        }
        return super.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return released(new MouseButtonEvent(mouseX, mouseY, button));
    }

    private boolean released(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) scrollTarget = Mth.clamp(scrollTarget - scrollY * WHEEL_STEP, 0, maxScroll());
        return true;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (NetworkTabs.click(event.x(), event.y(), left, top, WIDTH, 0, menu.pos)) return true;
        // The bar: jumps to where it was clicked, and can be dragged
        if (event.button() == 0 && hasBar() && event.x() >= barX() - 2 && event.x() < barX() + BAR_WIDTH + 2
                && event.y() >= areaTop() && event.y() < areaBottom()) {
            dragging = true;
            dragTo(event.y());
            return true;
        }
        if (linkingBlock() != null) {
            clickLinks(event.x(), event.y());
            return true;
        }
        boolean right = event.button() == 1;
        boolean inArea = event.y() >= areaTop() && event.y() < areaBottom();
        for (Card card : inArea ? cards() : List.<Card>of()) {
            BinaryView.Block block = view.blocks().get(card.index());
            if (inside(button(card, 0), event.x(), event.y())) {
                send(block.pos(), BinaryAction.TAKE_ALL, 0);
                return true;
            }
            if (inside(button(card, 1), event.x(), event.y())) {
                send(block.pos(), BinaryAction.CLEAR, 0);
                return true;
            }
            if (!block.options().isEmpty() && inside(button(card, 2), event.x(), event.y())) {
                linking = block.pos();
                return true;
            }
            for (int slot = 0; slot < cells(block); slot++) {
                int x = slotX(card, slot), y = slotY(card, slot);
                if (event.x() >= x && event.x() < x + SLOT && event.y() >= y && event.y() < y + SLOT) {
                    send(block.pos(), right ? BinaryAction.REMOVE : BinaryAction.CLICK, slotId(block, slot));
                    return true;
                }
            }
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    // ---------------------------------------------------------------- drawing

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        NetworkTabs.draw(graphics, font, left, top, WIDTH, 0, menu.pos, mouseX, mouseY);
        graphics.fill(left, top, left + WIDTH, top + height2, PANEL);
        graphics.outline(left, top, WIDTH, height2, PANEL_BORDER);
        graphics.centeredText(font, title, left + WIDTH / 2, top + 7, TEXT);

        if (view.blocks().isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.simpleores.binary.no_blocks", view.radius()),
                    left + WIDTH / 2, areaTop() + 10, TEXT_DIM);
        }

        // The cards slide to where they were scrolled
        scrollTarget = Mth.clamp(scrollTarget, 0, maxScroll());
        scroll += (scrollTarget - scroll) * 0.35;
        if (Math.abs(scrollTarget - scroll) < 0.5) scroll = scrollTarget;
        // Only what is inside the area of the cards is drawn (and can be pointed at)
        int cardMouseY = mouseY >= areaTop() && mouseY < areaBottom() ? mouseY : -10_000;
        graphics.enableScissor(left + 1, areaTop(), left + WIDTH - 1, areaBottom());
        List<Component> tooltip = null;
        BinaryView.Block recipesPanel = null;
        List<Card> cards = linkingBlock() != null ? List.of() : cards();
        for (Card card : cards) {
            BinaryView.Block block = view.blocks().get(card.index());
            Map<Integer, SlotRule> rules = new HashMap<>();
            for (SlotRule rule : block.rules()) rules.put(rule.slot(), rule);

            graphics.fill(card.x(), card.y(), card.x() + WIDTH - 20, card.y() + card.height(), CARD);
            graphics.item(block.icon(), card.x() + 2, card.y() + 2);
            int nameRight = button(card, block.options().isEmpty() ? 0 : 2)[0] - 4;
            graphics.text(font, Component.literal(fit(block.icon().getHoverName().getString(), nameRight - card.x() - 22)),
                    card.x() + 22, card.y() + 6, TEXT);
            drawButton(graphics, button(card, 0), Component.translatable("screen.simpleores.binary.take_all"), mouseX, cardMouseY);
            drawButton(graphics, button(card, 1), Component.translatable("screen.simpleores.binary.clear"), mouseX, cardMouseY);
            if (!block.options().isEmpty()) {
                drawButton(graphics, button(card, 2), Component.translatable("screen.simpleores.binary.recipes"), mouseX, cardMouseY);
                // Mouse over "Recipes": a small panel with the recipes linked to this machine (drawn at the end)
                if (inside(button(card, 2), mouseX, cardMouseY)) recipesPanel = block;
            }

            for (int slot = 0; slot < cells(block); slot++) {
                int x = slotX(card, slot), y = slotY(card, slot);
                boolean hovered = mouseX >= x && mouseX < x + SLOT && cardMouseY >= y && cardMouseY < y + SLOT;
                SlotRule rule = rules.get(slotId(block, slot));
                if (slot >= block.slots().size()) {
                    // A tank: blue square, the bucket of its liquid and the buckets inside
                    BinaryView.Tank tank = block.tanks().get(slot - block.slots().size());
                    graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, hovered ? SLOT_HOVER : TANK_COLOR);
                    if (!tank.bucket().isEmpty()) graphics.item(tank.bucket(), x + 1, y + 1);
                    if (tank.amount() > 0) {
                        String amount = tank.amount() < StarFluids.BUCKET ? tank.amount() + "m" : StarNetwork.format(tank.amount() / StarFluids.BUCKET);
                        graphics.text(font, Component.literal(amount), x + SLOT - 1 - font.width(amount), y + SLOT - 9, TANK_TEXT);
                    }
                    if (rule != null) graphics.outline(x, y, SLOT, SLOT, color(rule));
                    if (hovered) {
                        tooltip = new ArrayList<>();
                        Fluid fluid = StarFluids.fromBucket(tank.bucket());
                        tooltip.add(fluid != null ? StarFluids.name(fluid) : Component.translatable("screen.simpleores.binary.tank"));
                        tooltip.add(Component.literal(String.format(Locale.ROOT, "%,d / %,d mB", tank.amount(), tank.capacity())).withColor(TANK_TEXT));
                        if (rule != null && rule.keeps()) {
                            tooltip.add(Component.translatable(rule.takes() ? "screen.simpleores.binary.both" : "screen.simpleores.binary.keeping", keptNames(rule))
                                    .withColor(rule.takes() ? BOTH_COLOR : KEEP_COLOR));
                        } else if (rule != null && rule.takes()) {
                            tooltip.add(Component.translatable("screen.simpleores.binary.taking").withColor(TAKE_COLOR));
                        }
                    }
                    continue;
                }
                graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, hovered ? SLOT_HOVER : roleColor(block, slot));
                drawRole(graphics, block, slot, x, y);
                ItemStack stack = block.slots().get(slot);
                if (!stack.isEmpty()) {
                    graphics.item(stack, x + 1, y + 1);
                    if (stack.getCount() > 1) {
                        String count = String.valueOf(stack.getCount());
                        graphics.text(font, Component.literal(count), x + SLOT - 1 - font.width(count), y + SLOT - 9, TEXT);
                    }
                }
                if (rule != null) graphics.outline(x, y, SLOT, SLOT, color(rule));

                if (hovered) {
                    tooltip = new ArrayList<>();
                    tooltip.add(stack.isEmpty() ? Component.translatable("screen.simpleores.binary.empty") : stack.getHoverName());
                    addRole(tooltip, block, slot);
                    if (rule != null && rule.takes() && rule.keeps()) {
                        tooltip.add(Component.translatable("screen.simpleores.binary.both", keptNames(rule)).withColor(BOTH_COLOR));
                    } else if (rule != null && rule.keeps()) {
                        tooltip.add(Component.translatable("screen.simpleores.binary.keeping", keptNames(rule)).withColor(KEEP_COLOR));
                    } else if (rule != null && rule.takes()) {
                        tooltip.add(Component.translatable("screen.simpleores.binary.taking").withColor(TAKE_COLOR));
                    }
                }
            }
        }
        graphics.disableScissor();
        // The recipes of a machine (over the cards)
        BinaryView.Block linkingBlock = linkingBlock();
        if (linkingBlock != null) tooltip = drawLinks(graphics, linkingBlock, mouseX, mouseY);
        // The bar on the right (when not every card fits)
        if (linkingBlock == null && hasBar()) {
            int[] thumb = thumb();
            graphics.fill(barX(), areaTop(), barX() + BAR_WIDTH, areaBottom(), SLOT_COLOR);
            graphics.fill(barX(), thumb[0], barX() + BAR_WIDTH, thumb[0] + thumb[1], dragging ? RANGE_COLOR : PANEL_BORDER);
        }

        // Inventory (its items are drawn by the screen itself)
        graphics.text(font, playerInventoryTitle, left + 10, top + height2 - BinaryMenu.INVENTORY_HEIGHT + 2, TEXT_DIM);
        for (Slot slot : menu.slots) {
            graphics.fill(left + slot.x, top + slot.y, left + slot.x + 16, top + slot.y + 16, SLOT_COLOR);
        }
        // The slot of the Magnetic Field Upgrades (the same color as in the Magnetar)
        Slot upgrade = menu.slots.get(0);
        int ux = left + upgrade.x, uy = top + upgrade.y;
        graphics.outline(ux - 1, uy - 1, 18, 18, RANGE_COLOR);
        if (!upgrade.hasItem() && menu.getCarried().isEmpty() && mouseX >= ux && mouseX < ux + 16 && mouseY >= uy && mouseY < uy + 16) {
            tooltip = List.of(ModItems.MAGNETIC_FIELD_UPGRADE.get().getDefaultInstance().getHoverName());
        }

        super.render(graphics.graphics(), mouseX, mouseY, a);
        graphics.afterItems();
        if (recipesPanel != null) drawRecipesPanel(graphics, recipesPanel, mouseX, mouseY);
        else if (tooltip != null && menu.getCarried().isEmpty()) graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    /// The panel of the "Recipes" button: the icons of the recipes linked to the machine (or "none").
    private void drawRecipesPanel(GuiGraphicsExtractor graphics, BinaryView.Block block, int mouseX, int mouseY) {
        List<ItemStack> icons = new ArrayList<>();
        for (int o = 0; o < block.options().size(); o++) {
            if (block.linked().get(o)) icons.add(block.options().get(o).outputStack());
        }
        Component title = Component.translatable("screen.simpleores.binary.linked_title");
        Component none = Component.translatable("screen.simpleores.binary.linked_none");
        int columns = Math.min(Math.max(1, icons.size()), 8);
        int rows = (icons.size() + columns - 1) / columns;
        int width = Math.max(font.width(title), icons.isEmpty() ? font.width(none) : columns * SLOT) + 8;
        int height = 14 + (icons.isEmpty() ? 10 : rows * SLOT) + 4;
        int x = Math.min(mouseX + 10, this.width - width - 2);
        int y = Math.max(2, mouseY - height - 4);
        graphics.fill(x, y, x + width, y + height, 0xF0100010);
        graphics.outline(x, y, width, height, PANEL_BORDER);
        graphics.text(font, title, x + 4, y + 4, TEXT);
        if (icons.isEmpty()) {
            graphics.text(font, none, x + 4, y + 16, TEXT_DIM);
            return;
        }
        for (int i = 0; i < icons.size(); i++) {
            int ix = x + 4 + (i % columns) * SLOT, iy = y + 14 + (i / columns) * SLOT;
            graphics.fill(ix, iy, ix + SLOT - 2, iy + SLOT - 2, INPUT_BACK);
            graphics.outline(ix, iy, SLOT - 2, SLOT - 2, INPUT_LINE);
            graphics.item(icons.get(i), ix, iy);
        }
    }

    /// The names of the items kept by a rule ("Cobblestone, Dirt, Gravel"); liquids by their names.
    private static Component keptNames(SlotRule rule) {
        net.minecraft.network.chat.MutableComponent names = Component.empty();
        for (int i = 0; i < rule.items().size(); i++) {
            if (i > 0) names.append(", ");
            ItemStack item = rule.items().get(i).create();
            Fluid fluid = StarFluids.fromBucket(item);
            names.append(fluid != null ? StarFluids.name(fluid) : item.getHoverName());
        }
        return names;
    }

    /// What slot "slot" of a block is for (SlotRoles: input / output; unknown if the server didn't say).
    private static int role(BinaryView.Block block, int slot) {
        return slot < block.roles().size() ? block.roles().get(slot) : SlotRoles.UNKNOWN;
    }

    /// The background of a slot: greenish for an input, reddish for an output.
    private static int roleColor(BinaryView.Block block, int slot) {
        int role = role(block, slot);
        return role == SlotRoles.INPUT ? INPUT_BACK : role == SlotRoles.OUTPUT ? OUTPUT_BACK : SLOT_COLOR;
    }

    /// A line at the bottom of the slot: green = input, red = output.
    private static void drawRole(GuiGraphicsExtractor graphics, BinaryView.Block block, int slot, int x, int y) {
        int role = role(block, slot);
        if (role == SlotRoles.UNKNOWN) return;
        graphics.fill(x + 1, y + SLOT - 3, x + SLOT - 1, y + SLOT - 1, role == SlotRoles.INPUT ? INPUT_LINE : OUTPUT_LINE);
    }

    private static void addRole(List<Component> tooltip, BinaryView.Block block, int slot) {
        int role = role(block, slot);
        if (role == SlotRoles.INPUT) tooltip.add(Component.translatable("screen.simpleores.binary.input").withColor(INPUT_LINE));
        else if (role == SlotRoles.OUTPUT) tooltip.add(Component.translatable("screen.simpleores.binary.output").withColor(OUTPUT_LINE));
    }

    private static int color(SlotRule rule) {
        if (rule.takes() && rule.keeps()) return BOTH_COLOR;
        return rule.keeps() ? KEEP_COLOR : TAKE_COLOR;
    }

    // ---------------------------------------------------------------- the recipes of a machine

    /// The machine whose recipes are shown (null = the cards are shown).
    private @Nullable BlockPos linking;

    private @Nullable BinaryView.Block linkingBlock() {
        if (linking == null) return null;
        for (BinaryView.Block block : view.blocks()) {
            if (block.pos().equals(linking) && !block.options().isEmpty()) return block;
        }
        linking = null;
        return null;
    }

    private int[] backButton() {
        return new int[]{left + WIDTH - 10 - 2 - 44, areaTop() + 3, 44, 14};
    }

    /// Where the recipe "i" is drawn: {x, y}.
    private int[] optionAt(int i) {
        return new int[]{left + 14 + (i % COLUMNS) * SLOT, areaTop() + CARD_HEADER + 2 + (i / COLUMNS) * SLOT};
    }

    /// The recipes of the Quasars the machine can make: a click links (or unlinks) one. The linked ones are green.
    private @Nullable List<Component> drawLinks(GuiGraphicsExtractor graphics, BinaryView.Block block, int mouseX, int mouseY) {
        graphics.fill(left + 10, areaTop(), left + WIDTH - 10, areaBottom(), CARD);
        graphics.item(block.icon(), left + 12, areaTop() + 2);
        int nameRight = backButton()[0] - 4;
        graphics.text(font, Component.literal(fit(block.icon().getHoverName().getString(), nameRight - left - 32)), left + 32, areaTop() + 6, TEXT);
        drawButton(graphics, backButton(), Component.translatable("screen.simpleores.binary.back"), mouseX, mouseY);
        List<Component> tooltip = null;
        for (int i = 0; i < block.options().size(); i++) {
            int[] at = optionAt(i);
            if (at[1] + SLOT > areaBottom()) break;
            boolean linked = block.linked().get(i);
            boolean hovered = mouseX >= at[0] && mouseX < at[0] + SLOT && mouseY >= at[1] && mouseY < at[1] + SLOT;
            graphics.fill(at[0] + 1, at[1] + 1, at[0] + SLOT - 1, at[1] + SLOT - 1, hovered ? SLOT_HOVER : linked ? INPUT_BACK : SLOT_COLOR);
            if (linked) graphics.outline(at[0], at[1], SLOT, SLOT, INPUT_LINE);
            com.exoticmoss.simpleores.starstorage.quasar.Pattern pattern = block.options().get(i);
            graphics.item(pattern.outputStack(), at[0] + 1, at[1] + 1);
            if (hovered) {
                tooltip = new ArrayList<>();
                tooltip.add(pattern.outputStack().getHoverName().copy().append(" x" + pattern.count()));
                for (int k = 0; k < pattern.inputs().size(); k++) {
                    List<com.exoticmoss.simpleores.util.ItemStackTemplate> slot = pattern.inputs().get(k);
                    if (slot.isEmpty()) continue;
                    String amount = pattern.amount(k) > 1 ? pattern.amount(k) + " x " : "";
                    tooltip.add(Component.literal(" - " + amount).append(slot.get(0).create().getHoverName()));
                }
                tooltip.add(Component.translatable(linked ? "screen.simpleores.binary.linked" : "screen.simpleores.binary.not_linked")
                        .withColor(linked ? INPUT_LINE : TEXT_DIM));
            }
        }
        return tooltip;
    }

    private void clickLinks(double mouseX, double mouseY) {
        BinaryView.Block block = linkingBlock();
        if (block == null) return;
        if (inside(backButton(), mouseX, mouseY)) {
            linking = null;
            return;
        }
        for (int i = 0; i < block.options().size(); i++) {
            int[] at = optionAt(i);
            if (mouseX >= at[0] && mouseX < at[0] + SLOT && mouseY >= at[1] && mouseY < at[1] + SLOT) {
                send(block.pos(), BinaryAction.LINK, i);
                return;
            }
        }
    }

    private void drawButton(GuiGraphicsExtractor graphics, int[] box, Component text, int mouseX, int mouseY) {
        graphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], inside(box, mouseX, mouseY) ? BUTTON_HOVER : BUTTON);
        graphics.outline(box[0], box[1], box[2], box[3], BUTTON_BORDER);
        graphics.centeredText(font, text, box[0] + box[2] / 2, box[1] + 3, TEXT);
    }

    /// The text cut (with "...") to fit in "width" pixels.
    private String fit(String text, int width) {
        if (font.width(text) <= width) return text;
        while (!text.isEmpty() && font.width(text + "...") > width) text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY) {
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
