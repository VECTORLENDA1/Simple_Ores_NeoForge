package com.vector.simpleores.screen;

import com.vector.simpleores.SimpleOres;
import com.vector.simpleores.gravitycraft.CoreCandidate;
import com.vector.simpleores.gravitycraft.CoreTier;
import com.vector.simpleores.gravitycraft.GravityCoreBlockEntity;
import com.vector.simpleores.gravitycraft.SelectCoreRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.awt.Color.blue;
import static java.awt.SystemColor.text;

/// The selection bar that floats above a Gravity Core (client side only).
///
/// When the orbiting items fit more than one recipe, the bar shows the possible results
/// (up to VISIBLE at a time) and always turns to face the player:
///  - look at a recipe: it lights up and the action bar shows what it needs (green = done, red = missing);
///  - right-click: picks that recipe (right-click it again to unpick it);
///  - mouse wheel: scrolls when there are more than VISIBLE recipes ("..." at the ends of the bar);
///  - throw more items: only the recipes that use all of them stay in the bar.
///
/// This class only handles the looking/clicking. The bar is drawn by GravityCoreRenderer,
/// and the candidates come from the server (GravityCoreBlockEntity).
@EventBusSubscriber(modid = SimpleOres.MODID, value = Dist.CLIENT)
public class CoreSelectionBar {
    /// How many recipes the bar shows at the same time.
    public static final int VISIBLE = 5;
    /// Size of one slot (in blocks) and the space between slots.
    public static final float SLOT = 0.5f;
    public static final float GAP = 0.06f;
    /// Height of the bar above the top of the sphere (in blocks).
    public static final float HEIGHT_ABOVE_CORE = 0.45f;
    /// How far (in blocks) the player can be to use the bar.
    public static final double REACH = 6;
    /// How far around the player the client looks for cores with a bar.
    private static final int SEARCH = 6;

    /// Core and slot the player is looking at right now (null / -1 = none).
    private static @Nullable BlockPos hoveredCore = null;
    private static int hoveredSlot = -1;
    /// First recipe shown in the bar of each core (changed with the mouse wheel).
    private static final Map<BlockPos, Integer> scroll = new HashMap<>();
    /// Used to refresh the text in the action bar every now and then while looking at a slot.
    private static int textTimer = 0;
    /// Game time of the last right-click on a bar. Holding the button down repeats the click
    /// every few ticks, so only the first one counts (otherwise the recipe would keep being picked and unpicked).
    private static long lastClick = -100;

    // ---------------------------------------------------------------- layout (also used by GravityCoreRenderer)

    /// Height of the center of the bar, from the bottom of the core's block.
    public static float barHeight(CoreTier tier) {
        return tier.coreSize + HEIGHT_ABOVE_CORE;
    }

    /// Center of the bar in the world.
    public static Vec3 barCenter(BlockPos pos, CoreTier tier) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + barHeight(tier), pos.getZ() + 0.5);
    }

    /// Width of a bar with "count" slots.
    public static float barWidth(int count) {
        return count * SLOT + (count - 1) * GAP;
    }

    /// Horizontal position of the center of a slot, from the center of the bar (left = negative).
    public static float slotX(int index, int count) {
        return -barWidth(count) / 2 + SLOT / 2 + index * (SLOT + GAP);
    }

    /// Angle (around the vertical axis) that makes the bar face the viewer.
    public static float yawTowards(Vec3 barCenter, Vec3 viewer) {
        return (float) Mth.atan2(viewer.x - barCenter.x, viewer.z - barCenter.z);
    }

    /// First recipe shown in the bar of this core (0 = the first one).
    public static int scrollOf(BlockPos pos, int total) {
        int max = Math.max(0, total - VISIBLE);
        return Mth.clamp(scroll.getOrDefault(pos, 0), 0, max);
    }

    /// Slot of this core's bar the player is looking at (-1 = none).
    public static int hoveredSlotOf(BlockPos pos) {
        return pos.equals(hoveredCore) ? hoveredSlot : -1;
    }

    /// Which slot (0 = first visible one) the line from "eye" in direction "look" hits. -1 = none.
    private static int slotHit(GravityCoreBlockEntity core, int count, Vec3 eye, Vec3 look) {
        Vec3 center = barCenter(core.getBlockPos(), core.getTier());
        double dx = eye.x - center.x;
        double dz = eye.z - center.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4) return -1; // exactly below/above the bar

        // The bar is a vertical rectangle facing the player: (sin, 0, cos) points from the bar to the player
        double sin = dx / length;
        double cos = dz / length;
        double facing = look.x * sin + look.z * cos;
        if (facing >= -1.0E-4) return -1; // looking away from the bar

        double distance = ((center.x - eye.x) * sin + (center.z - eye.z) * cos) / facing;
        if (distance < 0 || distance > REACH) return -1;

        Vec3 hit = eye.add(look.scale(distance));
        double x = (hit.x - center.x) * cos - (hit.z - center.z) * sin; // to the right of the player = positive
        double y = hit.y - center.y;
        if (Math.abs(y) > SLOT / 2) return -1;

        for (int i = 0; i < count; i++) {
            if (Math.abs(x - slotX(i, count)) <= SLOT / 2) return i;
        }
        return -1;
    }

    // ---------------------------------------------------------------- events

    /// Every tick: finds the bar slot the player is looking at.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos oldCore = hoveredCore;
        int oldSlot = hoveredSlot;
        hoveredCore = null;
        hoveredSlot = -1;
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1);
        BlockPos center = mc.player.blockPosition();
        double best = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-SEARCH, -SEARCH, -SEARCH), center.offset(SEARCH, 2, SEARCH))) {
            if (!(mc.level.getBlockEntity(pos) instanceof GravityCoreBlockEntity core)) continue;
            List<CoreCandidate> candidates = core.getCandidates();
            if (candidates.isEmpty()) continue;

            int first = scrollOf(pos, candidates.size());
            int count = Math.min(VISIBLE, candidates.size() - first);
            int slot = slotHit(core, count, eye, look);
            double distance = eye.distanceToSqr(barCenter(pos, core.getTier()));
            if (slot >= 0 && distance < best) {
                best = distance;
                hoveredCore = pos.immutable();
                hoveredSlot = slot;
            }
        }

        // Shows what the recipe needs (refreshed every half second, because items keep arriving)
        boolean changed = !Objects.equals(oldCore, hoveredCore) || oldSlot != hoveredSlot;
        if (hoveredCore != null && (changed || ++textTimer >= 10)) {
            textTimer = 0;
            CoreCandidate candidate = hoveredCandidate(mc);
            if (candidate != null) {
                GravityCoreBlockEntity core = (GravityCoreBlockEntity) mc.level.getBlockEntity(hoveredCore);
                boolean picked = core != null && candidate.id().equals(core.getTarget());
                mc.player.sendOverlayMessage(describe(candidate, picked));
            }
        }
    }

    /// Right-click while looking at a slot: picks that recipe (or unpicks it if it's already picked).
    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || hoveredCore == null) return;
        Minecraft mc = Minecraft.getInstance();
        CoreCandidate candidate = hoveredCandidate(mc);
        if (candidate == null || !(mc.level.getBlockEntity(hoveredCore) instanceof GravityCoreBlockEntity core)) return;

        // The click was for the bar: don't use the item in hand / the block behind it
        event.setCanceled(true);
        event.setSwingHand(false);

        long now = mc.level.getGameTime();
        boolean held = now >= lastClick && now - lastClick <= 5;
        lastClick = now;
        if (held) return; // the button is being held down

        boolean picked = candidate.id().equals(core.getTarget());
        ClientPacketDistributor.sendToServer(new SelectCoreRecipe(hoveredCore, picked ? Optional.empty() : Optional.of(candidate.id())));
        textTimer = 10; // refresh the text on the next tick
    }

    /// Mouse wheel while looking at a bar with more than VISIBLE recipes: scrolls it.
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (hoveredCore == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.level.getBlockEntity(hoveredCore) instanceof GravityCoreBlockEntity core)) return;
        int total = core.getCandidates().size();
        if (total <= VISIBLE) return;

        int step = event.getScrollDeltaY() > 0 ? -1 : 1;
        scroll.put(hoveredCore, Mth.clamp(scrollOf(hoveredCore, total) + step, 0, total - VISIBLE));
        textTimer = 10;
        event.setCanceled(true); // don't change the selected hotbar slot
    }

    // ---------------------------------------------------------------- helpers

    /// The recipe of the slot the player is looking at. null = none.
    private static @Nullable CoreCandidate hoveredCandidate(Minecraft mc) {
        if (hoveredCore == null || mc.level == null) return null;
        if (!(mc.level.getBlockEntity(hoveredCore) instanceof GravityCoreBlockEntity core)) return null;
        List<CoreCandidate> candidates = core.getCandidates();
        int index = scrollOf(hoveredCore, candidates.size()) + hoveredSlot;
        return index >= 0 && index < candidates.size() ? candidates.get(index) : null;
    }

    /// Text for the action bar, Right-click to pick".
    private static Component describe(CoreCandidate candidate, boolean picked) {
        MutableComponent text = Component.empty();
        String hint = picked ? "gui.simpleores.core_bar.unpick" : "gui.simpleores.core_bar.pick";
        return text.append(Component.translatable(hint).withStyle(ChatFormatting.GRAY));
    }
}