package com.vector.simpleores.gravitycraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// Draws a placed Gravity Core: the star (see CoreDrawing) and the items orbiting around it.
public class GravityCoreRenderer implements BlockEntityRenderer<GravityCoreBlockEntity, GravityCoreRenderer.State> {
    /// How tilted (in degrees) the orbit of the items is.
    private static final float ORBIT_TILT = 12;
    /// Length of the Pulsar light beams, in blocks.
    private static final float BEAM_LENGTH = 3.5f;

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

        // During an upgrade the core slowly turns into the next tier: "blend" goes from 0 (old) to 1 (new).
        // When not upgrading, "from" and "to" are the same tier, so nothing changes.
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
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        float radius = state.radius * (1 + 0.03f * Mth.sin(time * 0.15f)); // the sphere slowly "breathes"

        // Direction from the sphere to the camera: the parts of the sphere facing the camera are brighter
        Vec3 center = new Vec3(state.blockPos.getX() + 0.5, state.blockPos.getY() + state.radius, state.blockPos.getZ() + 0.5);
        Vec3 toCamera = camera.pos.subtract(center).normalize();
        CoreDrawing.Shading shading = (p, x, y, z) -> (float) (x * toCamera.x + y * toCamera.y + z * toCamera.z);

        pose.pushPose();
        pose.translate(0.5, state.radius, 0.5); // center of the sphere: sitting on top of the block below

        // ---- The star (sphere, glow, disk, beams)
        CoreDrawing.drawStar(pose, collector, radius, time, state.coreColor, state.spotColor, state.glowColor,
                state.diskColor, state.beamColor, BEAM_LENGTH, shading);

        // ---- Orbiting items, on a slightly tilted ring.
        // During a collapse they spiral into the center (the speed is handled in GravityCoreBlockEntity).
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
    }

    /// The sphere, disk, beams and orbits go outside the block: this makes Minecraft draw them
    /// even when the block itself is just off screen.
    @Override
    public AABB getRenderBoundingBox(GravityCoreBlockEntity core) {
        return new AABB(core.getBlockPos()).inflate(5);
    }
}
