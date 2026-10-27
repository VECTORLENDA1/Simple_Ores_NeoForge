package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.binary.BinaryAction;
import com.exoticmoss.simpleores.starstorage.binary.BinaryMenu;
import com.exoticmoss.simpleores.starstorage.binary.BinaryView;
import com.exoticmoss.simpleores.starstorage.binary.SlotRule;
import com.exoticmoss.simpleores.item.ModItems;
import com.exoticmoss.simpleores.starstorage.binary.BinaryStarBlockEntity;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;
import com.exoticmoss.simpleores.starstorage.network.StarNetwork;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/// The screen of the Binary Star: every block with an inventory around it (chests, furnaces...),
/// one card per block with all its slots. Mouse wheel scrolls the cards.
///  - Left-click on a slot: next mode: take (orange) -> keep full (green) -> input and output (light purple) -> nothing.
///    With an item in the mouse cursor (taken from the inventory below): the slot keeps that item.
///  - Right-click on a slot: no rule.
///  - "Take all": every slot of the block is TAKE. "Clear": no rules for the block.
/// The screen only asks the server (BinaryAction); the server changes the rules and sends back the view.
public class BinaryScreen extends AbstractContainerScreen<BinaryMenu> implements JeiTarget.Source {
    private static final int WIDTH = 220;
    private static final int SLOT = 18;
    private static final int COLUMNS = 9;
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
    private int scroll;
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
        scroll = Mth.clamp(scroll, 0, Math.max(0, view.blocks().size() - 1));
    }

    private void send(BlockPos block, int action, int slot) {
        ClientPacketDistributor.sendToServer(new BinaryAction(pos, block, action, slot));
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
        int y = areaTop();
        for (int i = scroll; i < view.blocks().size(); i++) {
            int cardHeight = CARD_HEADER + rows(view.blocks().get(i)) * SLOT + 6;
            if (!cards.isEmpty() && y + cardHeight > areaBottom()) break;
            cards.add(new Card(i, left + 10, y, cardHeight));
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
    private static int[] button(Card card, int which) {
        int clearWidth = 36, takeWidth = 52;
        int right = card.x() + WIDTH - 20 - 2;
        return which == 1 ? new int[]{right - clearWidth, card.y() + 3, clearWidth, 14}
                : new int[]{right - clearWidth - 3 - takeWidth, card.y() + 3, takeWidth, 14};
    }

    private static boolean inside(int[] box, double x, double y) {
        return x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3];
    }

    // ---------------------------------------------------------------- mouse

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) scroll = Mth.clamp(scroll + (scrollY > 0 ? -1 : 1), 0, Math.max(0, view.blocks().size() - 1));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (NetworkTabs.click(event.x(), event.y(), left, top, WIDTH, 0, menu.pos)) return true;
        boolean right = event.button() == 1;
        for (Card card : cards()) {
            BinaryView.Block block = view.blocks().get(card.index());
            if (inside(button(card, 0), event.x(), event.y())) {
                send(block.pos(), BinaryAction.TAKE_ALL, 0);
                return true;
            }
            if (inside(button(card, 1), event.x(), event.y())) {
                send(block.pos(), BinaryAction.CLEAR, 0);
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
        return super.mouseClicked(event, doubleClick);
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        NetworkTabs.draw(graphics, font, left, top, WIDTH, 0, menu.pos, mouseX, mouseY);
        graphics.fill(left, top, left + WIDTH, top + height2, PANEL);
        graphics.outline(left, top, WIDTH, height2, PANEL_BORDER);
        graphics.centeredText(font, title, left + WIDTH / 2, top + 7, TEXT);

        if (view.blocks().isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.simpleores.binary.no_blocks", view.radius()),
                    left + WIDTH / 2, areaTop() + 10, TEXT_DIM);
        }

        List<Component> tooltip = null;
        List<Card> cards = cards();
        for (Card card : cards) {
            BinaryView.Block block = view.blocks().get(card.index());
            Map<Integer, SlotRule> rules = new HashMap<>();
            for (SlotRule rule : block.rules()) rules.put(rule.slot(), rule);

            graphics.fill(card.x(), card.y(), card.x() + WIDTH - 20, card.y() + card.height(), CARD);
            graphics.item(block.icon(), card.x() + 2, card.y() + 2);
            int nameRight = button(card, 0)[0] - 4;
            graphics.text(font, Component.literal(fit(block.icon().getHoverName().getString(), nameRight - card.x() - 22)),
                    card.x() + 22, card.y() + 6, TEXT);
            drawButton(graphics, button(card, 0), Component.translatable("screen.simpleores.binary.take_all"), mouseX, mouseY);
            drawButton(graphics, button(card, 1), Component.translatable("screen.simpleores.binary.clear"), mouseX, mouseY);

            for (int slot = 0; slot < cells(block); slot++) {
                int x = slotX(card, slot), y = slotY(card, slot);
                boolean hovered = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
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
                graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, hovered ? SLOT_HOVER : SLOT_COLOR);
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
        // More cards below / above
        if (!cards.isEmpty() && cards.get(cards.size() - 1).index() < view.blocks().size() - 1) {
            graphics.centeredText(font, Component.literal("..."), left + WIDTH / 2, areaBottom() - 2, TEXT_DIM);
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

        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (tooltip != null && menu.getCarried().isEmpty()) graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
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

    private static int color(SlotRule rule) {
        if (rule.takes() && rule.keeps()) return BOTH_COLOR;
        return rule.keeps() ? KEEP_COLOR : TAKE_COLOR;
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
