package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.MagnetarAction;
import com.exoticmoss.simpleores.starstorage.MagnetarView;
import com.exoticmoss.simpleores.starstorage.SlotRule;
import com.exoticmoss.simpleores.starstorage.StarNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The screen of the Magnetar Core: the network and every block with an inventory inside its field
/// (chests, furnaces...), one card per block with all its slots. Mouse wheel scrolls the cards.
///  - Left-click on a slot: TAKE on/off (its items go to the network) - orange.
///  - Right-click on a slot: KEEP on/off (kept full of the item in it, or, if empty, the item in your hand) - green.
///  - Both on: input and output in the same slot - light purple.
///  - "Take all": every slot of the block is TAKE. "Clear": no rules for the block.
/// The screen only asks the server (MagnetarAction); the server changes the rules and sends back the view.
public class MagnetarScreen extends Screen {
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
    private static final int TAKE_COLOR = 0xFFE08A30;
    private static final int KEEP_COLOR = 0xFF40C070;
    private static final int BOTH_COLOR = 0xFFC9A6FF;
    private static final int FADE = 0xA0101018;
    private static final int BUTTON = 0xFF3A3A4E;
    private static final int BUTTON_HOVER = 0xFF55557A;
    private static final int BUTTON_BORDER = 0xFF70709A;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A0B0;

    private final BlockPos pos;
    private MagnetarView view;
    /// First card shown.
    private int scroll;
    private int ticks;
    private int left, top, height2;

    /// Where one card is on the screen (only the cards that fit are placed).
    private record Card(int index, int x, int y, int height) {
    }

    public MagnetarScreen(MagnetarView view) {
        super(Component.translatable("screen.simpleores.magnetar.title"));
        this.pos = view.pos();
        this.view = view;
    }

    public BlockPos pos() {
        return pos;
    }

    public void update(MagnetarView view) {
        this.view = view;
        scroll = Mth.clamp(scroll, 0, Math.max(0, view.blocks().size() - 1));
    }

    private void send(BlockPos block, int action, int slot) {
        ClientPacketDistributor.sendToServer(new MagnetarAction(pos, block, action, slot));
    }

    @Override
    protected void init() {
        height2 = Math.min(height - 20, 260);
        left = (width - WIDTH) / 2;
        top = (height - height2) / 2;
    }

    @Override
    public void tick() {
        if (++ticks % REFRESH_TICKS == 0) send(pos, MagnetarAction.REFRESH, 0);
        if (minecraft.player == null || minecraft.player.position().distanceToSqr(Vec3.atCenterOf(pos)) > MagnetarAction.RANGE * MagnetarAction.RANGE) {
            onClose();
        }
    }

    // ---------------------------------------------------------------- where things are

    private int areaTop() {
        return top + 22;
    }

    private int areaBottom() {
        return top + height2 - 8;
    }

    private static int rows(MagnetarView.Block block) {
        return Math.max(1, (block.slots().size() + COLUMNS - 1) / COLUMNS);
    }

    /// The cards that fit, from the first one shown.
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
        boolean right = event.button() == 1;
        for (Card card : cards()) {
            MagnetarView.Block block = view.blocks().get(card.index());
            if (inside(button(card, 0), event.x(), event.y())) {
                send(block.pos(), MagnetarAction.TAKE_ALL, 0);
                return true;
            }
            if (inside(button(card, 1), event.x(), event.y())) {
                send(block.pos(), MagnetarAction.CLEAR, 0);
                return true;
            }
            for (int slot = 0; slot < block.slots().size(); slot++) {
                int x = slotX(card, slot), y = slotY(card, slot);
                if (event.x() >= x && event.x() < x + SLOT && event.y() >= y && event.y() < y + SLOT) {
                    send(block.pos(), right ? MagnetarAction.TOGGLE_KEEP : MagnetarAction.TOGGLE_TAKE, slot);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(left, top, left + WIDTH, top + height2, PANEL);
        graphics.outline(left, top, WIDTH, height2, PANEL_BORDER);
        graphics.centeredText(font, title, left + WIDTH / 2, top + 7, TEXT);

        if (view.blocks().isEmpty()) {
            graphics.centeredText(font, Component.translatable("screen.simpleores.magnetar.no_blocks", StarNetwork.FIELD_RADIUS),
                    left + WIDTH / 2, areaTop() + 10, TEXT_DIM);
        }

        List<Component> tooltip = null;
        List<Card> cards = cards();
        for (Card card : cards) {
            MagnetarView.Block block = view.blocks().get(card.index());
            Map<Integer, SlotRule> rules = new HashMap<>();
            for (SlotRule rule : block.rules()) rules.put(rule.slot(), rule);

            graphics.fill(card.x(), card.y(), card.x() + WIDTH - 20, card.y() + card.height(), CARD);
            graphics.item(block.icon(), card.x() + 2, card.y() + 2);
            int nameRight = button(card, 0)[0] - 4;
            graphics.text(font, Component.literal(fit(block.icon().getHoverName().getString(), nameRight - card.x() - 22)),
                    card.x() + 22, card.y() + 6, TEXT);
            drawButton(graphics, button(card, 0), Component.translatable("screen.simpleores.magnetar.take_all"), mouseX, mouseY);
            drawButton(graphics, button(card, 1), Component.translatable("screen.simpleores.magnetar.clear"), mouseX, mouseY);

            for (int slot = 0; slot < block.slots().size(); slot++) {
                int x = slotX(card, slot), y = slotY(card, slot);
                boolean hovered = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
                graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, hovered ? SLOT_HOVER : SLOT_COLOR);
                ItemStack stack = block.slots().get(slot);
                SlotRule rule = rules.get(slot);
                if (!stack.isEmpty()) {
                    graphics.item(stack, x + 1, y + 1);
                    if (stack.getCount() > 1) {
                        String count = String.valueOf(stack.getCount());
                        graphics.text(font, Component.literal(count), x + SLOT - 1 - font.width(count), y + SLOT - 9, TEXT);
                    }
                } else if (rule != null && rule.keeps()) {
                    // Empty KEEP slot: its item, faded
                    graphics.item(rule.item().get().create(), x + 1, y + 1);
                    graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, FADE);
                }
                if (rule != null) graphics.outline(x, y, SLOT, SLOT, color(rule));

                if (hovered) {
                    tooltip = new ArrayList<>();
                    tooltip.add(stack.isEmpty() ? Component.translatable("screen.simpleores.magnetar.empty") : stack.getHoverName());
                    if (rule != null && rule.takes() && rule.keeps()) {
                        tooltip.add(Component.translatable("screen.simpleores.magnetar.both", rule.item().get().create().getHoverName()).withColor(BOTH_COLOR));
                    } else if (rule != null && rule.keeps()) {
                        tooltip.add(Component.translatable("screen.simpleores.magnetar.keeping", rule.item().get().create().getHoverName()).withColor(KEEP_COLOR));
                    } else if (rule != null && rule.takes()) {
                        tooltip.add(Component.translatable("screen.simpleores.magnetar.taking").withColor(TAKE_COLOR));
                    }
                }
            }
        }
        // More cards below / above
        if (!cards.isEmpty() && cards.get(cards.size() - 1).index() < view.blocks().size() - 1) {
            graphics.centeredText(font, Component.literal("..."), left + WIDTH / 2, areaBottom() - 2, TEXT_DIM);
        }

        super.extractRenderState(graphics, mouseX, mouseY, a);
        if (tooltip != null) graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
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
    public boolean isPauseScreen() {
        return false;
    }
}
