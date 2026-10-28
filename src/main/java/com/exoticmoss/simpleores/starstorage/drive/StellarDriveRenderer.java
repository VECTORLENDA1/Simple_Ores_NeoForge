package com.exoticmoss.simpleores.starstorage.drive;

import com.exoticmoss.simpleores.gravitycraft.CoreDrawing;
import com.exoticmoss.simpleores.SimpleOres;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.exoticmoss.simpleores.util.render.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.exoticmoss.simpleores.util.render.BlockEntityRenderState;

import net.minecraft.client.renderer.RenderType;
import com.exoticmoss.simpleores.util.render.RenderTypes;
import com.exoticmoss.simpleores.util.render.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.exoticmoss.simpleores.starstorage.cell.CellTier;

/// Draws a Stellar Drive:
///  - the purple frame on the edges of the block. Drives side by side join into one big block: the frame is
///    only drawn on the outer edges (where there is no drive on either side of the edge);
///  - the cells inside: one small star per cell (colors from CellTier), turning slowly around the middle.
/// The glass itself is the block model (see-through, see stellar_drive.png).
public class StellarDriveRenderer implements BlockEntityRenderer<StellarDriveBlockEntity> {
    /// Radius of each small star (0.5 = one block wide).
    private static final float STAR_RADIUS = 0.09f;
    /// Distance of the stars from the middle of the block.
    private static final float ORBIT = 0.22f;
    /// How fast they turn (degrees per tick).
    private static final float TURN_SPEED = 0.8f;

    /// Frame: plain white texture (the color is below), how thick it is (1/16 = one pixel) and its color.
    private static final RenderType FRAME_TYPE = RenderTypes.entityCutout(
            ResourceLocation.fromNamespaceAndPath(SimpleOres.MODID, "textures/entity/gravity_core.png"));
    private static final float FRAME = 1 / 16f;
    private static final int FRAME_COLOR = 0xFF8A62D8;

    public static class State extends BlockEntityRenderState {
        float time;
        int[] tiers = new int[0];
        /// Is there another drive on each side (Direction order: down, up, north, south, west, east)?
        boolean[] joined = new boolean[6];
    }

    public StellarDriveRenderer(BlockEntityRendererProvider.Context context) {
    }

    /// The frame and the stars stay visible when the block is just off screen.
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(StellarDriveBlockEntity drive) {
        return new net.minecraft.world.phys.AABB(drive.getBlockPos()).inflate(0.1);
    }

    /// First reads what to draw from the block, then draws it.
    @Override
    public void render(StellarDriveBlockEntity drive, float partialTick, PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
        Vec3 cameraPos = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        State state = new State();
        state.blockPos = drive.getBlockPos();
        state.lightCoords = light;
        extractRenderState(drive, state, partialTick, cameraPos);
        submit(state, pose, new SubmitNodeCollector(buffers), new CameraRenderState(cameraPos));
    }

    private void extractRenderState(StellarDriveBlockEntity drive, State state, float partialTick, Vec3 cameraPos) {
        state.time = (drive.getLevel().getGameTime() % 100000) + partialTick;
        state.tiers = drive.getClientTiers();
        for (Direction direction : Direction.values()) {
            state.joined[direction.ordinal()] = drive.getLevel().getBlockState(drive.getBlockPos().relative(direction)).getBlock() instanceof StellarDriveBlock;
        }
    }

    private void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        drawFrame(state, pose, collector);
        int count = state.tiers.length;
        if (count == 0) return;
        Vec3 center = Vec3.atCenterOf(state.blockPos);
        Vec3 toCamera = camera.pos().subtract(center).normalize();
        CoreDrawing.Shading shading = (p, x, y, z) -> (float) (x * toCamera.x + y * toCamera.y + z * toCamera.z);

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.time * TURN_SPEED));
        for (int i = 0; i < count; i++) {
            boolean fluid = state.tiers[i] >= StellarDriveBlockEntity.FLUID;
            int ordinal = fluid ? state.tiers[i] - StellarDriveBlockEntity.FLUID : state.tiers[i];
            CellTier tier = CellTier.values()[Mth.clamp(ordinal, 0, CellTier.values().length - 1)];
            // One star: alone in the middle; more: spread around the middle, slowly going up and down
            float angle = Mth.TWO_PI * i / count;
            float orbit = count == 1 ? 0 : ORBIT;
            float bob = 0.04f * Mth.sin(state.time * 0.05f + i * 1.7f);
            pose.pushPose();
            pose.translate(Mth.cos(angle) * orbit, bob, Mth.sin(angle) * orbit);
            if (fluid) {
                // Fluid Cells: blue stars
                CoreDrawing.drawStar(pose, collector, STAR_RADIUS, state.time + i * 37, tier.fluidCoreColor, tier.fluidSpotColor,
                        tier.fluidGlowColor, tier.fluidDiskColor, 0x00000000, 0, shading);
            } else {
                CoreDrawing.drawStar(pose, collector, STAR_RADIUS, state.time + i * 37, tier.coreColor, tier.spotColor,
                        tier.glowColor, tier.diskColor, 0x00000000, 0, shading);
            }
            pose.popPose();
        }
        pose.popPose();
    }

    // ---------------------------------------------------------------- frame

    /// The 12 edges of the block. Each edge is between two faces: it is drawn only if there is no drive
    /// on either of those two sides (so a group of drives looks like one big block).
    private static void drawFrame(State state, PoseStack pose, SubmitNodeCollector collector) {
        int light = state.lightCoords;
        collector.submitCustomGeometry(pose, FRAME_TYPE, (p, v) -> {
            for (Direction.Axis axis : Direction.Axis.values()) {
                // The two other axes: the edge is at one of their 4 corners
                Direction.Axis a = axis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
                Direction.Axis b = axis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
                for (int corner = 0; corner < 4; corner++) {
                    boolean highA = (corner & 1) != 0, highB = (corner & 2) != 0;
                    Direction sideA = Direction.fromAxisAndDirection(a, highA ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
                    Direction sideB = Direction.fromAxisAndDirection(b, highB ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
                    if (state.joined[sideA.ordinal()] || state.joined[sideB.ordinal()]) continue;
                    float[] min = new float[3], max = new float[3];
                    set(min, max, axis, 0, 1);
                    set(min, max, a, highA ? 1 - FRAME : 0, highA ? 1 : FRAME);
                    set(min, max, b, highB ? 1 - FRAME : 0, highB ? 1 : FRAME);
                    box(p, v, min, max, light);
                }
            }
        });
    }

    private static void set(float[] min, float[] max, Direction.Axis axis, float from, float to) {
        min[axis.ordinal()] = from;
        max[axis.ordinal()] = to;
    }

    /// A small box (the 6 faces), slightly bigger than the block so it is not hidden by the glass.
    private static void box(PoseStack.Pose pose, VertexConsumer v, float[] min, float[] max, int light) {
        float e = 0.002f;
        float x1 = min[0] - e, y1 = min[1] - e, z1 = min[2] - e, x2 = max[0] + e, y2 = max[1] + e, z2 = max[2] + e;
        quad(pose, v, light, 0, -1, 0, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        quad(pose, v, light, 0, 1, 0, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1);
        quad(pose, v, light, 0, 0, -1, x1, y2, z1, x2, y2, z1, x2, y1, z1, x1, y1, z1);
        quad(pose, v, light, 0, 0, 1, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        quad(pose, v, light, -1, 0, 0, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        quad(pose, v, light, 1, 0, 0, x2, y2, z1, x2, y2, z2, x2, y1, z2, x2, y1, z1);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer v, int light, float nx, float ny, float nz, float... c) {
        for (int i = 0; i < 12; i += 3) {
            v.addVertex(pose, c[i], c[i + 1], c[i + 2])
                    .setColor(FRAME_COLOR)
                    .setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}
