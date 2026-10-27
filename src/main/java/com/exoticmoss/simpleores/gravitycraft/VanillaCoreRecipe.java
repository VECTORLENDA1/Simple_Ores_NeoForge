package com.exoticmoss.simpleores.gravitycraft;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.*;

/// A crafting table recipe (of Minecraft or of any mod) that also works in the Gravity Core.
///
/// There are no files for these: when the server starts (and after /reload) every crafting table recipe
/// is turned into one of these. The shape of the grid is ignored, only the amounts
/// count: a stone pickaxe becomes "3 cobblestone + 2 sticks". When two recipes use the same items
/// (pickaxe and axe), the selection bar above the core lets the player pick one.
///
/// Rules for all of them:
///  - they work from the first tier (Sun);
///  - they all take the same time (VANILLA_TIME), except in the Black Hole, where they are instant;
///  - they give the same amount as the crafting table (no bonus in higher tiers);
///  - items that stay behind in the crafting table (e.g. the empty buckets of a cake) are given back.
///
/// Crafting table recipes of every mod are used; furnace recipes only of Minecraft itself ("minecraft:" IDs).
/// Special recipes without fixed ingredients (fireworks, dyeing armor, copying maps...) are skipped.
public class VanillaCoreRecipe extends GravityCoreRecipe {
    /// Craft time (in ticks, 20 = 1 second) of every vanilla recipe in the Sun, Red Giant and Pulsar.
    public static final int VANILLA_TIME = 40;
    /// Time per tier: the same for the first 3 tiers, instant (0) in the Black Hole.
    private static final List<Integer> TIME_PER_TIER = List.of(VANILLA_TIME, VANILLA_TIME, VANILLA_TIME, 0);

    /// The crafting table recipe this came from. null for furnace (cooking) recipes.
    private final @Nullable CraftingRecipe original;
    private final ItemStack result;

    private VanillaCoreRecipe(@Nullable CraftingRecipe original, List<Part> parts, ItemStack result) {
        // No result template: the result is a ready ItemStack (see createResult / getUpgradeTier below)
        super(1, VANILLA_TIME, parts, null, List.of(), TIME_PER_TIER);
        this.original = original;
        this.result = result;
    }

    /// Always the same amount as the crafting table, in every tier.
    @Override
    public ItemStack createResult(int coreTier) {
        return result.copy();
    }

    /// Vanilla recipes never upgrade a core.
    @Override
    public @Nullable CoreTier getUpgradeTier() {
        return null;
    }

    /// Items left behind by the used ingredients (e.g. a milk bucket leaves an empty bucket).
    public List<ItemStack> remainders(List<ItemStack> used) {
        if (original == null) return List.of(); // furnace recipes leave nothing behind
        List<ItemStack> singles = new ArrayList<>();
        for (ItemStack stack : used) {
            for (int i = 0; i < stack.getCount(); i++) singles.add(stack.copyWithCount(1));
        }
        if (singles.isEmpty()) return List.of();

        NonNullList<ItemStack> remaining = original.getRemainingItems(CraftingInput.of(singles.size(), 1, singles));
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : remaining) {
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    // ---------------------------------------------------------------- converting the vanilla recipes

    /// The recipes are converted only once per recipe list (a new list is made after /reload).
    private static @Nullable RecipeManager cachedFor = null;
    private static List<RecipeHolder<GravityCoreRecipe>> cached = List.of();
    private static Map<ResourceKey<Recipe<?>>, RecipeHolder<GravityCoreRecipe>> cachedById = Map.of();

    /// Every vanilla crafting recipe, already converted for the Gravity Core.
    public static List<RecipeHolder<GravityCoreRecipe>> all(ServerLevel level) {
        update(level);
        return cached;
    }

    /// The converted vanilla recipe with this ID. Empty if it isn't one.
    public static Optional<RecipeHolder<GravityCoreRecipe>> find(ServerLevel level, ResourceKey<Recipe<?>> id) {
        update(level);
        return Optional.ofNullable(cachedById.get(id));
    }

    private static synchronized void update(ServerLevel level) {
        RecipeManager manager = level.recipeAccess();
        if (manager == cachedFor) return;

        List<RecipeHolder<GravityCoreRecipe>> list = new ArrayList<>();
        Map<ResourceKey<Recipe<?>>, RecipeHolder<GravityCoreRecipe>> byId = new HashMap<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            boolean minecraft = holder.id().identifier().getNamespace().equals("minecraft");
            try {
                // Crafting table recipes (of every mod) and furnace recipes (of Minecraft: cooking food...)
                VanillaCoreRecipe converted;
                if (holder.value() instanceof CraftingRecipe crafting) converted = convert(crafting);
                else if (minecraft && holder.value() instanceof SmeltingRecipe smelting) converted = convertCooking(smelting);
                else continue;
                if (converted == null) continue;
                RecipeHolder<GravityCoreRecipe> convertedHolder = new RecipeHolder<>(holder.id(), converted);
                list.add(convertedHolder);
                byId.put(holder.id(), convertedHolder);
            } catch (Exception e) {
                // A strange recipe shouldn't break the core: just leave it out
                SimpleOres.LOGGER.debug("Gravity Core: skipped vanilla recipe {}", holder.id(), e);
            }
        }

        cached = List.copyOf(list);
        cachedById = Map.copyOf(byId);
        cachedFor = manager;
        SimpleOres.LOGGER.info("Gravity Core: {} crafting table and cooking recipes available", cached.size());
    }

    /// Turns a crafting recipe into a Gravity Core recipe. null = it can't be used (special recipe).
    private static @Nullable VanillaCoreRecipe convert(CraftingRecipe recipe) {
        if (recipe.isSpecial() || recipe.placementInfo().isImpossibleToPlace()) return null;

        // One ingredient per used slot of the grid: equal ingredients are grouped and counted
        // (e.g. 3x cobblestone + 2x stick)
        Map<List<Holder<Item>>, Part> parts = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
            List<Holder<Item>> items = ingredient.items().toList();
            if (items.isEmpty()) return null; // empty tag: the recipe can never be made
            parts.compute(items, (k, old) -> new Part(ingredient, old == null ? 1 : old.count() + 1, List.of()));
        }
        if (parts.isEmpty()) return null;

        ItemStack result = recipe.assemble(CraftingInput.EMPTY);
        if (result.isEmpty()) {
            // Some recipes of other mods only give their result for real ingredients: try the first item of each
            List<ItemStack> first = new ArrayList<>();
            for (Ingredient ingredient : recipe.placementInfo().ingredients()) first.add(new ItemStack(ingredient.items().findFirst().get()));
            result = recipe.assemble(CraftingInput.of(first.size(), 1, first));
        }
        if (result.isEmpty()) return null;

        return new VanillaCoreRecipe(recipe, new ArrayList<>(parts.values()), result);
    }

    /// Turns a furnace recipe (e.g. raw beef -> steak, sand -> glass) into a Gravity Core recipe:
    /// 1 ingredient -> the result. null = it can't be used.
    private static @Nullable VanillaCoreRecipe convertCooking(SmeltingRecipe recipe) {
        Ingredient input = recipe.input();
        if (input.items().findAny().isEmpty()) return null; // empty tag: the recipe can never be made
        // Ores and raw ores are left out: they have their own recipes with fuel (antracite or coal)
        // that multiply the result, like the Simple Ores ores (gravity_core/vanilla/)
        if (input.items().anyMatch(item -> item.is(Tags.Items.ORES) || item.is(Tags.Items.RAW_MATERIALS))) return null;

        ItemStack result = recipe.assemble(new SingleRecipeInput(ItemStack.EMPTY));
        if (result.isEmpty()) return null;

        return new VanillaCoreRecipe(null, List.of(new Part(input, 1, List.of())), result);
    }
}
