package com.exoticmoss.simpleores.util.render;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.function.BiConsumer;

/// Where the renderers of this mod draw (the way later versions of Minecraft do it): shapes made of vertices
/// with a render type, drawn right away in the buffers of this frame.
/// In the world ("world"), the light of the stars (RenderTypes.glow) is drawn after the blocks with a renderer
/// (chests and other things): it writes no depth, so whatever is drawn after it would cover it.
public record SubmitNodeCollector(MultiBufferSource buffers, boolean world) {
    public SubmitNodeCollector(MultiBufferSource buffers) {
        this(buffers, false);
    }

    /// For the renderers of the blocks in the world.
    public static SubmitNodeCollector world(MultiBufferSource buffers) {
        return new SubmitNodeCollector(buffers, true);
    }

    public void submitCustomGeometry(PoseStack pose, RenderType type, BiConsumer<PoseStack.Pose, VertexConsumer> drawing) {
        if (world && RenderTypes.isGlow(type) && LATER.size() < 100_000) {
            LATER.add(new Later(pose.last().copy(), type, drawing));
            return;
        }
        drawing.accept(pose.last(), buffers.getBuffer(type));
    }

    /// A shape kept to be drawn at the end of the frame.
    private record Later(PoseStack.Pose pose, RenderType type, BiConsumer<PoseStack.Pose, VertexConsumer> drawing) {
    }

    private static final List<Later> LATER = new ArrayList<>();

    /// Draws the light of the stars kept in this frame (after the blocks with a renderer).
    public static void drawLater() {
        if (LATER.isEmpty()) return;
        MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
        // Star by star, the farthest first (each star in the order its parts were given: glow, disk, beams), then
        // the light shells and areas, over everything
        List<Later> stars = new ArrayList<>(), overlays = new ArrayList<>();
        for (Later later : LATER) (RenderTypes.isOverlay(later.type()) ? overlays : stars).add(later);
        LATER.clear();
        stars.sort(java.util.Comparator.comparingDouble((Later later) -> {
            org.joml.Matrix4f matrix = later.pose().pose();
            return matrix.m30() * matrix.m30() + matrix.m31() * matrix.m31() + matrix.m32() * matrix.m32();
        }).reversed());
        for (List<Later> list : List.of(stars, overlays)) {
            java.util.Set<RenderType> types = new java.util.LinkedHashSet<>();
            for (Later later : list) {
                later.drawing().accept(later.pose(), source.getBuffer(later.type()));
                types.add(later.type());
            }
            for (RenderType type : types) source.endBatch(type);
        }
    }

    @EventBusSubscriber(modid = SimpleOres.MODID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onRenderStage(RenderLevelStageEvent event) {
            // After the blocks with a renderer (chests...), and before the see-through blocks (glass, water), so the
            // stars inside a Stellar Drive still show through its glass
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) drawLater();
        }
    }
}
