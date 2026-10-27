package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.starstorage.network.StarTabs;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Optional;

/// The tabs of the network on top of the screens of the star storage (on the right; see StarTabs): one for each
/// terminal, the Magnetar, each Quasar and each Binary Star of the field. Clicking one opens its screen.
public final class NetworkTabs {
    private static final int TAB = 22;
    private static final int GAP = 2;
    private static final int TAB_OFF = 0xF0060609;
    private static final int TAB_ON = 0xFF2A2440;
    private static final int BORDER = 0xFF8060C0;
    private static final int BORDER_ON = 0xFFC0A0FF;

    private static List<StarTabs.Tab> tabs = List.of();

    private NetworkTabs() {
    }

    public static void receive(List<StarTabs.Tab> received) {
        tabs = received;
    }

    /// The tabs shown on the screen of the block at "current" (none if it is alone, or not in the list).
    /// "reserved" = the space on the left of the top of the panel used by other tabs.
    private static List<StarTabs.Tab> shown(BlockPos current, int width, int reserved) {
        int index = indexOf(current);
        if (index < 0 || tabs.size() < 2) return List.of();
        int fit = Math.max(1, (width - 8 - reserved) / (TAB + GAP));
        if (tabs.size() <= fit) return tabs;
        // Too many: the ones around the block shown
        int start = Math.max(0, Math.min(index - fit / 2, tabs.size() - fit));
        return tabs.subList(start, start + fit);
    }

    private static int indexOf(BlockPos pos) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).pos().equals(pos)) return i;
        }
        return -1;
    }

    /// The left of the tab "i" of "count" (they end at the right of the panel).
    private static int tabX(int left, int width, int count, int i) {
        return left + width - 4 - (count - i) * (TAB + GAP) + GAP;
    }

    private static int tabY(int top) {
        return top - TAB + 2;
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, int left, int top, int width, int reserved,
                            BlockPos current, int mouseX, int mouseY) {
        List<StarTabs.Tab> list = shown(current, width, reserved);
        int y = tabY(top);
        for (int i = 0; i < list.size(); i++) {
            StarTabs.Tab tab = list.get(i);
            int x = tabX(left, width, list.size(), i);
            boolean on = tab.pos().equals(current);
            graphics.fill(x, y, x + TAB, top, on ? TAB_ON : TAB_OFF);
            graphics.outline(x, y, TAB, top - y, on ? BORDER_ON : BORDER);
            graphics.item(tab.icon(), x + 3, y + 2);
            if (mouseX >= x && mouseX < x + TAB && mouseY >= y && mouseY < top) {
                graphics.setTooltipForNextFrame(font, List.of(tab.icon().getHoverName()), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    /// A click on a tab: opens the screen of its block. Returns true if a tab was clicked.
    public static boolean click(double mouseX, double mouseY, int left, int top, int width, int reserved, BlockPos current) {
        List<StarTabs.Tab> list = shown(current, width, reserved);
        int y = tabY(top);
        for (int i = 0; i < list.size(); i++) {
            int x = tabX(left, width, list.size(), i);
            if (mouseX >= x && mouseX < x + TAB && mouseY >= y && mouseY < top) {
                BlockPos pos = list.get(i).pos();
                if (!pos.equals(current)) ClientPacketDistributor.sendToServer(new StarTabs.Open(pos));
                return true;
            }
        }
        return false;
    }
}
