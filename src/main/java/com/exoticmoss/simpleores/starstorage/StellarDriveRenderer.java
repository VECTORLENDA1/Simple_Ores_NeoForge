package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// Draws the cells inside a Stellar Drive: one small star per cell (colors from CellTier),
/// turning slowly around the middle of the see-through block.
public class StellarDriveRenderer implements BlockEntityRenderer<StellarDriveBlockEntity, StellarDriveRenderer.State> {
    /// Radius of each small star (0.5 = one block wide).
    private static final float STAR_RADIUS = 0.09f;
    /// Distance of the stars from the middle of the block.
    private static final float ORBIT = 0.22f;
    /// How fast they turn (degrees per tick).
    private static final float TURN_SPEED = 0.8f;

    public static class State extends BlockEntityRenderState {
        float time;
        int[] tiers = new int[0];
    }

    public StellarDriveRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StellarDriveBlockEntity drive, State state, float partialTick, Vec3 cameraPos,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(drive, state, partialTick, cameraPos, breakProgress);
        state.time = (drive.getLevel().getGameTime() % 100000) + partialTick;
        state.tiers = drive.getClientTiers();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int count = state.tiers.length;
        if (count == 0) return;
        Vec3 center = Vec3.atCenterOf(state.blockPos);
        Vec3 toCamera = camera.pos.subtract(center).normalize();
        CoreDrawing.Shading shading = (p, x, y, z) -> (float) (x * toCamera.x + y * toCamera.y + z * toCamera.z);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.time * TURN_SPEED));
        for (int i = 0; i < count; i++) {
            CellTier tier = CellTier.values()[Mth.clamp(state.tiers[i], 0, CellTier.values().length - 1)];
            // One star: alone in the middle; more: spread around the middle, slowly going up and down
            float angle = Mth.TWO_PI * i / count;
            float orbit = count == 1 ? 0 : ORBIT;
            float bob = 0.04f * Mth.sin(state.time * 0.05f + i * 1.7f);
            pose.pushPose();
            pose.translate(Mth.cos(angle) * orbit, bob, Mth.sin(angle) * orbit);
            CoreDrawing.drawStar(pose, collector, STAR_RADIUS, state.time + i * 37, tier.coreColor, tier.spotColor,
                    tier.glowColor, tier.diskColor, 0x00000000, 0, shading);
            pose.popPose();
        }
        pose.popPose();
    }
}
