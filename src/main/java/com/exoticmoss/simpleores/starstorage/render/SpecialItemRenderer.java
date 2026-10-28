package com.exoticmoss.simpleores.starstorage.render;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlock;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreItemRenderer;
import com.exoticmoss.simpleores.starstorage.cell.FluidCellItem;
import com.exoticmoss.simpleores.starstorage.cell.StarCellItem;
import com.exoticmoss.simpleores.util.render.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/// Draws the items that are 3D stars (Gravity Cores, the stars of the star storage, the Star and Fluid Cells): their
/// item models are "builtin/entity", so Minecraft asks this to draw them. Size, angle and position in each place
/// (hotbar, hand, ground...) come from the "display" part of their item model files.
public class SpecialItemRenderer extends BlockEntityWithoutLevelRenderer {
    /// One drawing for each item (made the first time).
    private final Map<Item, Drawing> drawings = new HashMap<>();

    private interface Drawing {
        void draw(PoseStack pose, SubmitNodeCollector collector, int light, int overlay);
    }

    public SpecialItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Drawing drawing = drawings.computeIfAbsent(stack.getItem(), SpecialItemRenderer::drawingOf);
        if (drawing == null) return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5); // the middle of the item
        drawing.draw(pose, new SubmitNodeCollector(buffers), light, overlay);
        pose.popPose();
    }

    private static Drawing drawingOf(Item item) {
        if (item instanceof StarCellItem cell) {
            CellItemRenderer renderer = new CellItemRenderer(cell.tier, false);
            return (pose, collector, light, overlay) -> renderer.submit(pose, collector, light, overlay, false, 0);
        }
        if (item instanceof FluidCellItem cell) {
            CellItemRenderer renderer = new CellItemRenderer(cell.tier, true);
            return (pose, collector, light, overlay) -> renderer.submit(pose, collector, light, overlay, false, 0);
        }
        Block block = item instanceof BlockItem blockItem ? blockItem.getBlock() : null;
        if (block instanceof GravityCoreBlock core) {
            GravityCoreItemRenderer renderer = new GravityCoreItemRenderer(core.tier);
            return (pose, collector, light, overlay) -> renderer.submit(pose, collector, light, overlay, false, 0);
        }
        StarLook look = block == ModBlocks.MAGNETAR_CORE.get() ? StarLook.MAGNETAR
                : block == ModBlocks.STELLAR_TERMINAL.get() ? StarLook.TERMINAL
                : block == ModBlocks.ADVANCED_TERMINAL.get() ? StarLook.TERMINAL_2
                : block == ModBlocks.BINARY_STAR.get() ? StarLook.BINARY
                : block == ModBlocks.QUASAR.get() ? StarLook.QUASAR : null;
        if (look == null) return null;
        StarItemRenderer renderer = new StarItemRenderer(look);
        return (pose, collector, light, overlay) -> renderer.submit(pose, collector, light, overlay, false, 0);
    }
}
