package com.exoticmoss.simpleores.item.custom;

import com.exoticmoss.simpleores.item.ModToolMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/// A simple sword that makes Wither Skeletons drop their skull much more often
/// (see WitherReaperEvents). Helps getting Nether Stars without farming for hours.
public class WitherReaperItem extends SwordItem {
    /// Chance (0 to 1) of a Wither Skeleton killed with this sword dropping its skull.
    public static final float SKULL_CHANCE = 0.33f;

    public WitherReaperItem(Properties properties) {
        super(ModToolMaterials.WITHER_REAPER, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.simpleores.wither_reaper.tooltip").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
