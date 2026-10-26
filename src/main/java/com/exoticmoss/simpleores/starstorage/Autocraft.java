package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The auto-craft of the Quasar (crafting table recipes).
///  1. plan: works out everything that is needed, also the sub-recipes (planks -> sticks -> tool), using
///     what the network has; if something is missing, it says what and how many (nothing is crafted);
///  2. run: crafts every step, in order, taking the ingredients from the network and putting the results
///     (and what stays, like empty buckets) back in the network.
public final class Autocraft {
    /// Sub-recipes can go this deep at most (stops recipes that need themselves).
    private static final int MAX_DEPTH = 16;
    /// At most this many crafts in one request (so a huge request can't freeze the server).
    public static final int MAX_CRAFTS = 20_000;

    /// One step of the plan: craft "times" times the pattern, with these items in the 9 slots (null = empty).
    private record Step(Pattern pattern, List<ItemStackTemplate> items, long times) {
    }

    /// The result of a request: what is missing (empty = it was crafted), or an error message key.
    public record Result(Map<ItemStackTemplate, Long> missing, String error) {
        public boolean ok() {
            return missing.isEmpty() && error == null;
        }
    }

    private final Map<ItemStackTemplate, Long> stock = new HashMap<>();
    private final List<Pattern> patterns;
    private final List<Step> steps = new ArrayList<>();
    private final Map<ItemStackTemplate, Long> missing = new LinkedHashMap<>();
    private long crafts;

    private Autocraft(StarNetwork network) {
        for (Map.Entry<ItemStackTemplate, Long> entry : network.contents()) stock.put(entry.getKey(), entry.getValue());
        this.patterns = network.patterns();
    }

    /// Plans and, if nothing is missing, crafts "amount" of "item" into the network.
    public static Result request(ServerLevel level, StarNetwork network, ItemStackTemplate item, long amount) {
        Autocraft craft = new Autocraft(network);
        Pattern pattern = craft.patternFor(item);
        if (pattern == null) return new Result(Map.of(), "screen.simpleores.terminal.craft.no_recipe");
        // The items asked for are always crafted (even if the network already has some)
        craft.craft(pattern, item, amount, 0);
        if (craft.crafts > MAX_CRAFTS) return new Result(Map.of(), "screen.simpleores.terminal.craft.too_big");
        if (!craft.missing.isEmpty()) return new Result(craft.missing, null);
        String error = craft.run(level, network);
        return new Result(Map.of(), error);
    }

    private Pattern patternFor(ItemStackTemplate item) {
        ItemStack stack = item.create();
        for (Pattern pattern : patterns) {
            if (pattern.makes(stack)) return pattern;
        }
        return null;
    }

    // ---------------------------------------------------------------- plan

    /// Uses "amount" of "item" from the network, crafting what is not there. false = something is missing.
    private boolean need(ItemStackTemplate item, long amount, int depth) {
        long have = stock.getOrDefault(item, 0L);
        long used = Math.min(have, amount);
        if (used > 0) stock.put(item, have - used);
        amount -= used;
        if (amount <= 0) return true;
        Pattern pattern = patternFor(item);
        if (pattern == null || depth >= MAX_DEPTH) {
            missing.merge(item, amount, Long::sum);
            return false;
        }
        return craft(pattern, item, amount, depth);
    }

    /// Plans crafting at least "amount" of "item" with "pattern" (the extra goes to the stock).
    private boolean craft(Pattern pattern, ItemStackTemplate item, long amount, int depth) {
        long times = (amount + pattern.count() - 1) / pattern.count();
        crafts += times;
        if (crafts > MAX_CRAFTS) return false;
        boolean ok = true;
        List<ItemStackTemplate> items = new ArrayList<>();
        for (List<ItemStackTemplate> choices : pattern.inputs()) {
            if (choices.isEmpty()) {
                items.add(null);
                continue;
            }
            ItemStackTemplate choice = choose(choices, times);
            items.add(choice);
            ok &= need(choice, times, depth + 1);
        }
        steps.add(new Step(pattern, items, times));
        long extra = times * pattern.count() - amount;
        if (extra > 0) stock.merge(item, extra, Long::sum);
        return ok;
    }

    /// The item to use for a slot: one the network has enough of, else one it has some of,
    /// else one that has a recipe, else the first one.
    private ItemStackTemplate choose(List<ItemStackTemplate> choices, long times) {
        for (ItemStackTemplate choice : choices) {
            if (stock.getOrDefault(choice, 0L) >= times) return choice;
        }
        for (ItemStackTemplate choice : choices) {
            if (stock.getOrDefault(choice, 0L) > 0) return choice;
        }
        for (ItemStackTemplate choice : choices) {
            if (patternFor(choice) != null) return choice;
        }
        return choices.get(0);
    }

    // ---------------------------------------------------------------- run

    /// Crafts every step in order (the sub-recipes first). Returns an error message key, or null.
    private String run(ServerLevel level, StarNetwork network) {
        for (Step step : steps) {
            for (long time = 0; time < step.times(); time++) {
                List<ItemStack> grid = new ArrayList<>();
                for (ItemStackTemplate item : step.items()) {
                    if (item == null) {
                        grid.add(ItemStack.EMPTY);
                        continue;
                    }
                    ItemStack got = network.extract(item.create(), 1);
                    if (got.isEmpty()) {
                        giveBack(network, grid);
                        return "screen.simpleores.terminal.craft.changed";
                    }
                    grid.add(got);
                }
                CraftingInput input = CraftingInput.of(3, 3, grid);
                Optional<RecipeHolder<CraftingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
                if (recipe.isEmpty()) {
                    giveBack(network, grid);
                    return "screen.simpleores.terminal.craft.no_recipe";
                }
                ItemStack result = recipe.get().value().assemble(input);
                NonNullList<ItemStack> remaining = recipe.get().value().getRemainingItems(input);
                network.insert(result);
                for (ItemStack rest : remaining) {
                    if (!rest.isEmpty()) network.insert(rest);
                }
            }
        }
        return null;
    }

    private static void giveBack(StarNetwork network, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (!item.isEmpty()) network.insert(item);
        }
    }
}
