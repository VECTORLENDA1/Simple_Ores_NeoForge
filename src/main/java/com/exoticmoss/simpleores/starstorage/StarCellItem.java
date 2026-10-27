package com.exoticmoss.simpleores.starstorage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/// A Star Cell: keeps items for the Magnetar network. It works inside a Stellar Drive.
/// The items are kept in the cell itself (CELL_CONTENTS), so a cell taken out of a drive keeps everything.
public class StarCellItem extends Item {
    /// How many kinds of items the tooltip shows (the rest is "...and X more").
    private static final int TOOLTIP_LINES = 6;

    public final CellTier tier;

    public StarCellItem(CellTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public static CellContents getContents(ItemStack cell) {
        return cell.getOrDefault(ModDataComponents.CELL_CONTENTS.get(), CellContents.EMPTY);
    }

    private static void setContents(ItemStack cell, CellContents contents) {
        if (contents.isEmpty()) cell.remove(ModDataComponents.CELL_CONTENTS.get());
        else cell.set(ModDataComponents.CELL_CONTENTS.get(), contents);
    }

    /// True if the cell has no items inside.
    public static boolean isEmptyCell(ItemStack cell) {
        return getContents(cell).isEmpty();
    }

    /// How many more items the cell can take (any kind of item: there is no limit of kinds).
    public int space(ItemStack cell) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, tier.capacity - getContents(cell).totalCount()));
    }

    /// Puts "amount" of this item in the cell (call space first).
    public void insert(ItemStack cell, ItemStack stack, int amount) {
        if (amount > 0) setContents(cell, getContents(cell).plus(stack, amount));
    }

    /// Takes up to "amount" of this item out of the cell. Returns how many were taken.
    public int extract(ItemStack cell, ItemStack stack, int amount) {
        CellContents contents = getContents(cell);
        int index = contents.indexOf(stack);
        if (index < 0 || amount <= 0) return 0;
        int taken = Math.min(amount, contents.entries().get(index).count());
        setContents(cell, contents.minus(index, taken));
        return taken;
    }

    /// A cell with items shines like an enchanted item.
    @Override
    public boolean isFoil(ItemStack stack) {
        return !isEmptyCell(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        CellContents contents = getContents(stack);
        tooltip.accept(Component.translatable("item.simpleores.star_cell.items",
                StarNetwork.format(contents.totalCount()), StarNetwork.format(tier.capacity)).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.simpleores.star_cell.types",
                contents.entries().size()).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < Math.min(TOOLTIP_LINES, contents.entries().size()); i++) {
            CellContents.Entry entry = contents.entries().get(i);
            tooltip.accept(Component.literal(" " + StarNetwork.format(entry.count()) + "x ")
                    .append(entry.create(1).getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (contents.entries().size() > TOOLTIP_LINES) {
            tooltip.accept(Component.translatable("item.simpleores.star_cell.more", contents.entries().size() - TOOLTIP_LINES)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("item.simpleores.star_cell.hint").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
