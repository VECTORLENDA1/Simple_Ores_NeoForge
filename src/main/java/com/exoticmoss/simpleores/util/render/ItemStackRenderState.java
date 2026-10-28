package com.exoticmoss.simpleores.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/// An item to draw (set by ItemModelResolver.updateForTopItem), drawn with "submit".
public class ItemStackRenderState {
    ItemStack stack = ItemStack.EMPTY;
    ItemDisplayContext context = ItemDisplayContext.GROUND;
    @Nullable Level level;
    int seed;

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outlineColor) {
        if (stack.isEmpty()) return;
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, context, light, overlay, pose, collector.buffers(), level, seed);
    }
}
