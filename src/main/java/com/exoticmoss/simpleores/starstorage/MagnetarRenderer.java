package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Draws a placed Magnetar Core: a small, very bright star with two magnetic beams that spin around it
/// (the same drawing as the Gravity Cores, see CoreDrawing).
public class MagnetarRenderer implements BlockEntityRenderer<MagnetarBlockEntity, MagnetarRenderer.State> {
    /// Colors (0xAARRGGBB).
    public static final int CORE_COLOR = 0xFFEAF2FF;
    public static final int SPOT_COLOR = 0xFF8A6CFF;
    public static final int GLOW_COLOR = 0xFFA070FF;
    public static final int BEAM_COLOR = 0xFFC9A6FF;
    /// Radius of the star (0.5 = one block wide) and length of the beams (blocks).
    private static final float RADIUS = 0.36f;
    private static final float BEAM_LENGTH = 2.5f;

    public static class State extends BlockEntityRenderState {
        float time;
    }

    public MagnetarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MagnetarBlockEntity magnetar, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(magnetar, state, partialTick, cameraPos, breakProgress);
        state.time = (magnetar.getLevel().getGameTime() % 100000) + partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        float radius = RADIUS * (1 + 0.04f * Mth.sin(time * 0.3f)); // it "pulses" faster than the Gravity Cores

        Vec3 center = Vec3.atCenterOf(state.blockPos);
        Vec3 toCamera = camera.pos.subtract(center).normalize();
        CoreDrawing.Shading shading = (p, x, y, z) -> (float) (x * toCamera.x + y * toCamera.y + z * toCamera.z);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        CoreDrawing.drawStar(pose, collector, radius, time * 1.5f, CORE_COLOR, SPOT_COLOR, GLOW_COLOR,
                0x00000000, BEAM_COLOR, BEAM_LENGTH, shading);
        pose.popPose();
    }

    /// The beams go outside the block: this makes Minecraft draw them even when the block is just off screen.
    @Override
    public AABB getRenderBoundingBox(MagnetarBlockEntity magnetar) {
        return new AABB(magnetar.getBlockPos()).inflate(4);
    }
}
