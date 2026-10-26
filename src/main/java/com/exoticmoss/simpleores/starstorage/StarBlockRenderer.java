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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Draws a placed star of the star storage (Magnetar Core or Stellar Terminal), with its StarLook.
/// The star floats in the middle of the block and slowly "pulses".
public class StarBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, StarBlockRenderer.State> {
    private final StarLook look;

    public static class State extends BlockEntityRenderState {
        float time;
    }

    public StarBlockRenderer(BlockEntityRendererProvider.Context context, StarLook look) {
        this.look = look;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, breakProgress);
        state.time = (blockEntity.getLevel().getGameTime() % 100000) + partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        float radius = look.radius * (1 + 0.04f * Mth.sin(time * 0.3f));

        Vec3 center = Vec3.atCenterOf(state.blockPos);
        Vec3 toCamera = camera.pos.subtract(center).normalize();
        CoreDrawing.Shading shading = (p, x, y, z) -> (float) (x * toCamera.x + y * toCamera.y + z * toCamera.z);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        CoreDrawing.drawStar(pose, collector, radius, time * 1.5f, look.coreColor, look.spotColor, look.glowColor,
                0x00000000, look.beamColor, look.beamLength, shading);
        pose.popPose();
    }

    /// The beams go outside the block: this makes Minecraft draw them even when the block is just off screen.
    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(4);
    }
}
