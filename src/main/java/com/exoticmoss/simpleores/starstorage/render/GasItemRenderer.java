package com.exoticmoss.simpleores.starstorage.render;

import com.exoticmoss.simpleores.starstorage.gas.Gases;
import com.exoticmoss.simpleores.starstorage.network.Virtual;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/// The item of a gas (see Virtual): the texture of the gas (the same Mekanism draws in its tanks, moving) with the
/// color of the gas, like JEI shows the gases.
public class GasItemRenderer extends BlockEntityWithoutLevelRenderer {
    /// When the gas has no texture (or Mekanism is not installed).
    private static final ResourceLocation FALLBACK = ResourceLocation.withDefaultNamespace("block/water_still");

    public GasItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ResourceLocation gas = Virtual.gasOf(stack);
        ResourceLocation icon = gas == null ? null : Gases.icon(gas);
        int color = gas == null ? 0xFFA0A0A0 : Gases.color(gas);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon == null ? FALLBACK : icon);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose last = pose.last();
        // A square a little smaller than the slot, facing the camera
        float min = 1 / 16f, max = 15 / 16f, z = 0.5f;
        int full = LightTexture.FULL_BRIGHT;
        vertex(consumer, last, min, min, z, sprite.getU0(), sprite.getV1(), color, full);
        vertex(consumer, last, max, min, z, sprite.getU1(), sprite.getV1(), color, full);
        vertex(consumer, last, max, max, z, sprite.getU1(), sprite.getV0(), color, full);
        vertex(consumer, last, min, max, z, sprite.getU0(), sprite.getV0(), color, full);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, int light) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, 0, 0, 1);
    }
}
