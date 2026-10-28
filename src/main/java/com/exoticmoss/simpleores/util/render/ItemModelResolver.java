package com.exoticmoss.simpleores.util.render;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/// Gets an item ready to be drawn (see ItemStackRenderState).
public class ItemModelResolver {
    public void updateForTopItem(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, @Nullable Level level,
                                 @Nullable LivingEntity entity, int seed) {
        state.stack = stack;
        state.context = context;
        state.level = level;
        state.seed = seed;
    }
}
