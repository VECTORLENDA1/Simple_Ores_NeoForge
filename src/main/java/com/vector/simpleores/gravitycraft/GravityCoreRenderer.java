package com.vector.simpleores.gravitycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vector.simpleores.SimpleOres;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Draws a placed Gravity Core: the star (see CoreDrawing), the items orbiting around it
/// and the selection bar with the possible recipes (see CoreSelectionBar).
public class GravityCoreRenderer implements BlockEntityRenderer<GravityCoreBlockEntity, GravityCoreRenderer.State> {
    /// How tilted (in degrees) the orbit of the items is.
    private static final float ORBIT_TILT = 12;
    /// Length of the Pulsar light beams, in blocks.
    private static final float BEAM_LENGTH = 3.5f;

    /// Selection bar: plain white texture (the colors are below), always fully lit.
    private static final RenderType BAR_TYPE = RenderTypes.entityTranslucentEmissive(
            Identifier.fromNamespaceAndPath(SimpleOres.MODID, "textures/entity/gravity_core.png"));
    /// Colors of the selection bar (0xAARRGGBB).
    private static final int BAR_BACKGROUND = 0xA0101018;
    private static final int SLOT_MISSING = 0x70606070;   // recipe still missing items
    private static final int SLOT_COMPLETE = 0x9040B050;  // every item is orbiting
    private static final int SLOT_PICKED = 0xB0FFB020;    // the recipe the player picked
    private static final int SLOT_HOVERED = 0xFFFFFFFF;   // border of the slot the player is looking at
    private static final int BAR_DOTS = 0xE0FFFFFF;       // "..." = more recipes (mouse wheel)
    /// Size of the items inside the slots.
    private static final float BAR_ITEM_SCALE = 0.36f;

    /// Everything the drawing needs, copied from the core
    /// (Minecraft first "reads" the data, then "draws" it in a separate step).
    public static class State extends BlockEntityRenderState {
        float time;
        float progress;
        float radius;
        float orbitRadius;
        float orbitAngle;
        int coreColor;
        int spotColor;
        int glowColor;
        int diskColor;
        int beamColor;
        List<ItemStackRenderState> items = new ArrayList<>();

        /// Selection bar (empty list = no bar)
        List<ItemStackRenderState> barItems = new ArrayList<>();
        List<Boolean> barComplete = new ArrayList<>();
        boolean barPicked;
        int barHovered;
        boolean barMoreLeft;
        boolean barMoreRight;
        float barHeight;
    }

    private final ItemModelResolver itemModelResolver;

    public GravityCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GravityCoreBlockEntity core, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPos, breakProgress);
        state.time = (core.getLevel().getGameTime() % 100000) + partialTick;
        state.progress = core.getCraftProgress(partialTick);
        state.orbitAngle = core.getOrbitAngle(partialTick);

        /// During an upgrade the core slowly turns into the next tier: "blend" goes from 0 (old) to 1 (new).
        /// When not upgrading, "from" and "to" are the same tier, so nothing changes.
        CoreTier from = core.getTier();
        CoreTier to = core.getUpgradeTarget() != null ? core.getUpgradeTarget() : from;
        float blend = state.progress;

        state.radius = Mth.lerp(blend, from.coreSize, to.coreSize) / 2;
        state.orbitRadius = Mth.lerp(blend, from.orbitRadius, to.orbitRadius);
        state.coreColor = ARGB.srgbLerp(blend, from.coreColor, to.coreColor);
        state.spotColor = ARGB.srgbLerp(blend, from.spotColor, to.spotColor);
        state.glowColor = ARGB.srgbLerp(blend, from.glowColor, to.glowColor);
        state.diskColor = ARGB.srgbLerp(blend, from.diskColor, to.diskColor);
        state.beamColor = ARGB.srgbLerp(blend, from.beamColor, to.beamColor);

        state.items = new ArrayList<>();
        List<ItemStack> items = core.getItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModelResolver.updateForTopItem(itemState, items.get(i), ItemDisplayContext.GROUND, core.getLevel(), null, i);
            state.items.add(itemState);
        }

        /// ---- Selection bar: the recipes the orbiting items can still become
        state.barItems = new ArrayList<>();
        state.barComplete = new ArrayList<>();
        List<CoreCandidate> candidates = core.getCandidates();
        if (!candidates.isEmpty()) {
            int first = CoreSelectionBar.scrollOf(core.getBlockPos(), candidates.size());
            int count = Math.min(CoreSelectionBar.VISIBLE, candidates.size() - first);
            for (int i = 0; i < count; i++) {
                CoreCandidate candidate = candidates.get(first + i);
                ItemStackRenderState itemState = new ItemStackRenderState();
                itemModelResolver.updateForTopItem(itemState, candidate.result(), ItemDisplayContext.GUI, core.getLevel(), null, i);
                state.barItems.add(itemState);
                state.barComplete.add(candidate.complete());
            }
            state.barPicked = core.getTarget() != null;
            state.barHovered = CoreSelectionBar.hoveredSlotOf(core.getBlockPos());
            state.barMoreLeft = first > 0;
            state.barMoreRight = first + count < candidates.size();
            state.barHeight = CoreSelectionBar.barHeight(core.getTier());
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        float radius = state.radius * (1 + 0.03f * Mth.sin(time * 0.15f)); // the sphere slowly "breathes"

        /// Direction from the sphere to the camera: the parts of the sphere facing the camera are brighter
        Vec3 center = new Vec3(state.blockPos.getX() + 0.5, state.blockPos.getY() + state.radius, state.blockPos.getZ() + 0.5);
        Vec3 toCamera = camera.pos.subtract(center).normalize();
        CoreDrawing.Shading shading = (p, x, y, z) -> (float) (x * toCamera.x + y * toCamera.y + z * toCamera.z);

        pose.pushPose();
        pose.translate(0.5, state.radius, 0.5); // center of the sphere: sitting on top of the block below

        /// ---- The star (sphere, glow, disk, beams)
        CoreDrawing.drawStar(pose, collector, radius, time, state.coreColor, state.spotColor, state.glowColor,
                state.diskColor, state.beamColor, BEAM_LENGTH, shading);

        /// ---- Orbiting items, on a slightly tilted ring.
        /// During a collapse they spiral into the center (the speed is handled in GravityCoreBlockEntity).
        pose.mulPose(Axis.XP.rotationDegrees(ORBIT_TILT));
        int count = state.items.size();
        for (int i = 0; i < count; i++) {
            float angle = state.orbitAngle + i * Mth.TWO_PI / count; // items spread evenly around the ring
            float orbit = state.orbitRadius * (1 - state.progress);
            float bob = 0.08f * Mth.sin(time * 0.1f + i); // small up and down movement

            pose.pushPose();
            pose.translate(Mth.cos(angle) * orbit, bob, Mth.sin(angle) * orbit);
            pose.mulPose(Axis.YP.rotation(-angle));
            state.items.get(i).submit(pose, collector, CoreDrawing.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        pose.popPose();

        if (!state.barItems.isEmpty()) {
            drawBar(state, pose, collector, camera);
        }
    }

    // ---------------------------------------------------------------- selection bar

    /// Draws the selection bar above the core, turned to face the camera.
    /// The layout (sizes and positions) comes from CoreSelectionBar, so what is drawn
    /// is exactly what the player can click.
    private void drawBar(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 barCenter = new Vec3(state.blockPos.getX() + 0.5, state.blockPos.getY() + state.barHeight, state.blockPos.getZ() + 0.5);
        float yaw = CoreSelectionBar.yawTowards(barCenter, camera.pos);
        int count = state.barItems.size();
        float half = CoreSelectionBar.SLOT / 2;
        float width = CoreSelectionBar.barWidth(count);
        float padding = 0.05f;

        pose.pushPose();
        pose.translate(0.5, state.barHeight, 0.5);
        pose.mulPose(Axis.YP.rotation(yaw)); // from here on, +X = right of the viewer, +Z = towards the viewer

        collector.submitCustomGeometry(pose, BAR_TYPE, (p, v) -> {
            // Dark background behind every slot
            quad(p, v, -width / 2 - padding, -half - padding, width / 2 + padding, half + padding, 0, BAR_BACKGROUND);

            for (int i = 0; i < count; i++) {
                float x = CoreSelectionBar.slotX(i, count);
                if (i == state.barHovered) {
                    float border = 0.03f;
                    quad(p, v, x - half - border, -half - border, x + half + border, half + border, 0.005f, SLOT_HOVERED);
                }
                int color = state.barPicked ? SLOT_PICKED : state.barComplete.get(i) ? SLOT_COMPLETE : SLOT_MISSING;
                quad(p, v, x - half, -half, x + half, half, 0.01f, color);
            }

            // "..." outside the bar when there are more recipes on that side
            float dot = 0.035f;
            for (int k = 0; k < 3; k++) {
                float offset = width / 2 + padding + 0.06f + k * 0.07f;
                if (state.barMoreRight) quad(p, v, offset, -dot, offset + 2 * dot, dot, 0.01f, BAR_DOTS);
                if (state.barMoreLeft) quad(p, v, -offset - 2 * dot, -dot, -offset, dot, 0.01f, BAR_DOTS);
            }
        });

        // The result of each recipe, in front of its slot
        for (int i = 0; i < count; i++) {
            pose.pushPose();
            pose.translate(CoreSelectionBar.slotX(i, count), 0, 0.04);
            pose.scale(BAR_ITEM_SCALE, BAR_ITEM_SCALE, BAR_ITEM_SCALE);
            state.barItems.get(i).submit(pose, collector, CoreDrawing.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        pose.popPose();
    }

    /// A flat rectangle facing +Z (towards the viewer), from (x1, y1) to (x2, y2).
    private static void quad(PoseStack.Pose pose, VertexConsumer v, float x1, float y1, float x2, float y2, float z, int color) {
        barVertex(pose, v, x1, y1, z, color);
        barVertex(pose, v, x2, y1, z, color);
        barVertex(pose, v, x2, y2, z, color);
        barVertex(pose, v, x1, y2, z, color);
    }

    private static void barVertex(PoseStack.Pose pose, VertexConsumer v, float x, float y, float z, int color) {
        v.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(CoreDrawing.FULL_BRIGHT)
                .setNormal(pose, 0, 0, 1);
    }

    /// The sphere, disk, beams and orbits go outside the block: this makes Minecraft draw them
    /// even when the block itself is just off screen.
    @Override
    public AABB getRenderBoundingBox(GravityCoreBlockEntity core) {
        return new AABB(core.getBlockPos()).inflate(5);
    }
}