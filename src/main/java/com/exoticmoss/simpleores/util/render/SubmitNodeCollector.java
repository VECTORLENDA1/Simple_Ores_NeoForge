package com.exoticmoss.simpleores.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.function.BiConsumer;

/// Where the renderers of this mod draw (the way later versions of Minecraft do it): shapes made of vertices
/// with a render type, drawn right away in the buffers of this frame.
public record SubmitNodeCollector(MultiBufferSource buffers) {
    public void submitCustomGeometry(PoseStack pose, RenderType type, BiConsumer<PoseStack.Pose, VertexConsumer> drawing) {
        drawing.accept(pose.last(), buffers.getBuffer(type));
    }
}
