package com.exoticmoss.simpleores.gravitycraft;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The experience of "cooking" in the Gravity Core: the same experience a furnace gives.
///
/// When a craft of the core turns something that goes in a furnace (ores, raw ores, raw food, sand...)
/// into what the furnace makes of it, the player gets the furnace's experience for EVERY item made.
/// E.g. iron ore -> iron ingots: 0.7 experience per ingot, also for the extra ingots of the ore processing.
/// Normal crafting (e.g. nuggets -> ingot) gives nothing, like the crafting table.
/// The values come from the furnace / blast furnace / smoker recipes of the game and of the mods.
public final class CookingExperience {
    /// A furnace recipe: what goes in, and the experience for each item that comes out.
    private record Cooking(Ingredient input, float experience) {
    }

    private static @Nullable RecipeManager cachedFor = null;
    private static Map<Holder<Item>, List<Cooking>> byResult = Map.of();

    private CookingExperience() {
    }

    /// The experience for making "result" from the "used" items (0 if it isn't cooking).
    /// Fractions become a whole number by chance, like in the furnace (e.g. 2.5 = 2 or 3).
    public static int of(ServerLevel level, List<ItemStack> used, ItemStack result) {
        update(level);
        List<Cooking> recipes = byResult.get(result.typeHolder());
        if (recipes == null || result.isEmpty()) return 0;

        float perItem = 0;
        for (Cooking cooking : recipes) {
            for (ItemStack stack : used) {
                if (cooking.input().test(stack)) perItem = Math.max(perItem, cooking.experience());
            }
        }
        return roll(level.getRandom(), perItem * result.getCount());
    }

    private static int roll(RandomSource random, float experience) {
        int whole = Mth.floor(experience);
        float fraction = Mth.frac(experience);
        if (fraction > 0 && random.nextFloat() < fraction) whole++;
        return whole;
    }

    /// Reads the furnace recipes again when the recipe list changes (server start, /reload).
    private static synchronized void update(ServerLevel level) {
        RecipeManager manager = level.recipeAccess();
        if (manager == cachedFor) return;
        Map<Holder<Item>, List<Cooking>> map = new HashMap<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!(holder.value() instanceof AbstractCookingRecipe cooking) || cooking.experience() <= 0) continue;
            try {
                ItemStack out = cooking.assemble(new SingleRecipeInput(ItemStack.EMPTY));
                if (out.isEmpty()) continue;
                map.computeIfAbsent(out.typeHolder(), k -> new ArrayList<>()).add(new Cooking(cooking.input(), cooking.experience()));
            } catch (Exception ignored) {
                // a strange recipe is just left out
            }
        }
        byResult = Map.copyOf(map);
        cachedFor = manager;
    }
}
