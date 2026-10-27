package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.exoticmoss.simpleores.starstorage.client.AreaDisplay;
import org.jspecify.annotations.Nullable;

/// Draws a placed star of the star storage (Magnetar Core or Stellar Terminal), with its StarLook.
/// The star floats in the middle of the block and slowly "pulses".
public class StarBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, StarBlockRenderer.State> {
    private final StarLook look;
    /// The area box: very light gray, see-through (0xAARRGGBB).
    private static final int AREA_COLOR = 0x1EE8E8E8;
    private static final int AREA_EDGE_COLOR = 0x70F2F2F2;

    public static class State extends BlockEntityRenderState {
        float time;
        /// The player is looking at the star.
        boolean looked;
        /// The radius of the area shown with the "Show area" button (0 = not shown).
        int area;
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
        state.looked = Minecraft.getInstance().hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(blockEntity.getBlockPos());
        state.area = AreaDisplay.radius(blockEntity.getBlockPos());
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
        StarDrawing.draw(look, pose, collector, radius, time * 1.5f, look.beamLength, shading);
        // Looked at: a light shell around the star (instead of the box outline)
        if (state.looked) CoreDrawing.drawShell(pose, collector, radius * 1.18f, StarLook.HIGHLIGHT_COLOR);
        pose.popPose();
        // The area it reaches (Magnetar, Binary Star): a light gray box
        if (state.area > 0) CoreDrawing.drawArea(pose, collector, -state.area, state.area + 1, AREA_COLOR, AREA_EDGE_COLOR);
    }

    /// The beams (and the area) go outside the block: this makes Minecraft draw them even when the block is just off screen.
    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(Math.max(4, AreaDisplay.radius(blockEntity.getBlockPos()) + 1));
    }

    /// Far enough to see the whole area of a Magnetar with all its upgrades.
    @Override
    public int getViewDistance() {
        return 160;
    }
}
