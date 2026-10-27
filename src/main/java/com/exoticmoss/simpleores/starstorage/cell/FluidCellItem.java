package com.exoticmoss.simpleores.starstorage.cell;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;

import java.util.function.Consumer;
import com.exoticmoss.simpleores.starstorage.ModDataComponents;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;

/// A Fluid Cell: keeps liquids for the Magnetar network (any liquid, also of other mods). It works inside a
/// Stellar Drive, like the Star Cells. The liquids are kept in the cell itself (FLUID_CONTENTS).
public class FluidCellItem extends Item {
    private static final int TOOLTIP_LINES = 6;

    public final CellTier tier;

    public FluidCellItem(CellTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public static FluidContents getContents(ItemStack cell) {
        return cell.getOrDefault(ModDataComponents.FLUID_CONTENTS.get(), FluidContents.EMPTY);
    }

    private static void setContents(ItemStack cell, FluidContents contents) {
        if (contents.isEmpty()) cell.remove(ModDataComponents.FLUID_CONTENTS.get());
        else cell.set(ModDataComponents.FLUID_CONTENTS.get(), contents);
    }

    /// How many more mB the cell can take.
    public long space(ItemStack cell) {
        return Math.max(0, tier.fluidCapacity - getContents(cell).total());
    }

    /// Puts up to "amount" mB of this liquid in the cell. Returns how many mB went in.
    public long insert(ItemStack cell, Fluid fluid, long amount) {
        long put = Math.min(amount, space(cell));
        if (put > 0) setContents(cell, getContents(cell).plus(fluid, put));
        return Math.max(0, put);
    }

    /// Takes up to "amount" mB of this liquid out of the cell. Returns how many mB were taken.
    public long extract(ItemStack cell, Fluid fluid, long amount) {
        FluidContents contents = getContents(cell);
        long taken = Math.min(amount, contents.amountOf(fluid));
        if (taken > 0) setContents(cell, contents.plus(fluid, -taken));
        return Math.max(0, taken);
    }

    /// A cell with liquids shines like an enchanted item.
    @Override
    public boolean isFoil(ItemStack stack) {
        return !getContents(stack).isEmpty() || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        FluidContents contents = getContents(stack);
        tooltip.accept(Component.translatable("item.simpleores.fluid_cell.amount",
                StarFluids.buckets(contents.total()), StarFluids.buckets(tier.fluidCapacity)).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < Math.min(TOOLTIP_LINES, contents.entries().size()); i++) {
            FluidContents.Entry entry = contents.entries().get(i);
            tooltip.accept(Component.literal(" " + StarFluids.buckets(entry.amount()) + " ")
                    .append(StarFluids.name(entry.fluid())).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (contents.entries().size() > TOOLTIP_LINES) {
            tooltip.accept(Component.translatable("item.simpleores.star_cell.more", contents.entries().size() - TOOLTIP_LINES)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("item.simpleores.star_cell.hint").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
