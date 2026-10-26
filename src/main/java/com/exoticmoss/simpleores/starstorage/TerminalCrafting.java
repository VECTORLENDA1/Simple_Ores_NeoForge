package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// The crafting grid (3x3) of the Stellar Terminal.
/// The grid only keeps a "picture" of each item (no real items are inside it, so nothing can be lost or copied).
/// When crafting, every ingredient is taken from the Magnetar network (or, if the network has none,
/// from the player's inventory), and the result goes to the player's inventory.
public final class TerminalCrafting {
    public static final int SIZE = 9;
    /// The grid of each player (set by the TerminalGrid message).
    private static final Map<UUID, List<ItemStack>> GRIDS = new HashMap<>();

    private TerminalCrafting() {
    }

    /// The player changed the grid (9 slots, EMPTY = empty slot).
    public static void setGrid(ServerPlayer player, List<ItemStack> grid) {
        List<ItemStack> copy = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) copy.add(i < grid.size() ? grid.get(i).copyWithCount(1) : ItemStack.EMPTY);
        GRIDS.put(player.getUUID(), copy);
    }

    private static CraftingInput input(List<ItemStack> grid) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : grid) items.add(stack.copy());
        return CraftingInput.of(3, 3, items);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> recipe(ServerPlayer player, CraftingInput input) {
        if (input.isEmpty() || !(player.level() instanceof ServerLevel level)) return Optional.empty();
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    /// What the grid of the player makes (EMPTY = nothing).
    public static ItemStack result(ServerPlayer player) {
        List<ItemStack> grid = GRIDS.get(player.getUUID());
        if (grid == null) return ItemStack.EMPTY;
        CraftingInput input = input(grid);
        return recipe(player, input).map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    /// Crafts once, or ("stack") as many times as fit in one stack of the result.
    public static void craft(ServerPlayer player, StarNetwork network, boolean stack) {
        List<ItemStack> grid = GRIDS.get(player.getUUID());
        if (grid == null) return;
        ItemStack first = result(player);
        if (first.isEmpty()) return;
        int times = stack ? Math.max(1, first.getMaxStackSize() / Math.max(1, first.getCount())) : 1;

        for (int time = 0; time < times; time++) {
            CraftingInput input = input(grid);
            Optional<RecipeHolder<CraftingRecipe>> recipe = recipe(player, input);
            if (recipe.isEmpty()) return;

            // Takes one of each ingredient; if one is missing, gives back what was already taken and stops
            List<ItemStack> taken = new ArrayList<>();
            for (ItemStack ingredient : grid) {
                if (ingredient.isEmpty()) continue;
                ItemStack got = network.extract(ingredient, 1);
                if (got.isEmpty()) got = takeFromInventory(player.getInventory(), ingredient);
                if (got.isEmpty()) {
                    for (ItemStack back : taken) give(player, network, back);
                    return;
                }
                taken.add(got);
            }

            ItemStack result = recipe.get().value().assemble(input);
            NonNullList<ItemStack> remaining = recipe.get().value().getRemainingItems(input);
            give(player, network, result);
            for (ItemStack rest : remaining) {
                if (!rest.isEmpty()) give(player, network, rest);
            }
        }
    }

    /// Takes one of this exact item from the player's inventory (EMPTY = there is none).
    private static ItemStack takeFromInventory(Inventory inventory, ItemStack ingredient) {
        for (int slot = 0; slot < TerminalAction.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, ingredient)) {
                return stack.split(1);
            }
        }
        return ItemStack.EMPTY;
    }

    /// To the inventory; what doesn't fit goes to the network; what still doesn't fit drops on the ground.
    private static void give(ServerPlayer player, StarNetwork network, ItemStack stack) {
        if (stack.isEmpty()) return;
        player.getInventory().add(stack);
        if (stack.isEmpty()) return;
        ItemStack left = network.insert(stack);
        if (!left.isEmpty()) player.drop(left, false);
    }
}
