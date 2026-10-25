package com.exoticmoss.simpleores.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/// A simple sword that makes Wither Skeletons drop their skull much more often
/// (see WitherReaperEvents). Helps getting Nether Stars without farming for hours.
public class WitherReaperItem extends Item {
    /// Chance (0 to 1) of a Wither Skeleton killed with this sword dropping its skull.
    public static final float SKULL_CHANCE = 0.33f;

    public WitherReaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.simpleores.wither_reaper.tooltip").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
