package com.vector.simpleores.gravitycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.vector.simpleores.recipe.ModRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/// A Gravity Core recipe. Recipe files go in data/simpleores/recipe/gravity_core/. Example:
///
/// {
///   "type": "simpleores:gravity_collapse",
///   "tier": 1,                                         <- minimum core tier (1 = Sun ... 4 = Black Hole)
///   "time": 100,                                       <- collapse duration in ticks (20 ticks = 1 second)
///   "ingredients": [
///     { "item": "simpleores:raw_ignithra", "count": 4 },
///     { "item": "#c:ingots/iron" }                     <- "#" = item tag; "count" is optional (default 1)
///   ],
///   "result": { "id": "simpleores:ignithra", "count": 6 }
/// }
///
/// MORE OUTPUT IN HIGHER TIERS (optional): add "count_per_tier" to choose how many items the
/// result gives depending on the tier of the core doing the craft. The first number is for tier 1
/// (Sun), the second for tier 2 (Red Giant), and so on. If the list is shorter than the tiers,
/// the last number is used for the higher tiers. Without it, the "count" of "result" is used.
///
///   "count_per_tier": [2, 4, 8, 16]                    <- Sun = 2, Red Giant = 4, Pulsar = 8, Black Hole = 16
///
/// ONLY THE RECIPE'S ITEMS: a recipe only starts when every item orbiting the core is one of its
/// ingredients (extra amounts of those ingredients are fine; they stay in orbit). Any other item
/// blocks it. So to build a recipe that shares ingredients with a smaller one, drop one of its
/// unique items first.
///
/// UPGRADE RECIPES: if the result is a Gravity Core (e.g. "simpleores:gravity_core_red_giant"),
/// the recipe is an upgrade. It only works in a core of exactly that "tier", and instead of
/// giving an item, the core swallows the ingredients, slowly turns into the new tier and
/// drops itself on the ground, already upgraded.
///
/// DIFFERENT TIME IN HIGHER TIERS (optional): add "time_per_tier" to choose how long the collapse
/// takes (in ticks) depending on the tier of the core doing the craft. Works like "count_per_tier":
/// the first number is for tier 1 (Sun), and the last number is used for higher tiers if the list
/// is shorter. Without it, "time" is used for every tier. A value of 0 makes the craft instant.
///
///   "time_per_tier": [100, 80, 40, 0]                  <- Sun = 5s, Red Giant = 4s, Pulsar = 2s, Black Hole = instant
///
public class GravityCoreRecipe implements Recipe<GravityCoreRecipe.Input> {

    /// One ingredient with an amount (e.g. 4x raw_ignithra).
    public record Part(Ingredient item, int count, List<Integer> countPerTier) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Ingredient.CODEC.fieldOf("item").forGetter(Part::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Part::count),
                Codec.INT.listOf().optionalFieldOf("count_per_tier", List.of()).forGetter(Part::countPerTier)
        ).apply(inst, Part::new));

        /// How many of this ingredient are needed for a core of the given tier.
        public int countFor(int coreTier) {
            if (countPerTier.isEmpty()) return count;
            int index = Math.min(coreTier, countPerTier.size()) - 1; // tier 1 = first number
            return countPerTier.get(Math.max(0, index));
        }
    }

    /// What is inside a core: the orbiting items and the core's tier level.
    public record Input(List<ItemStack> items, int tier) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    private final int tier;
    private final int time;
    private final List<Part> ingredients;
    private final ItemStackTemplate result;
    private final List<Integer> countPerTier;
    private final List<Integer> timePerTier;

    public GravityCoreRecipe(int tier, int time, List<Part> ingredients, ItemStackTemplate result, List<Integer> countPerTier, List<Integer> timePerTier) {
        this.tier = tier;
        this.time = time;
        this.ingredients = ingredients;
        this.result = result;
        this.countPerTier = countPerTier;
        this.timePerTier = timePerTier;
    }

    public int getTier() {
        return tier;
    }

    public int getTime() {
        return time;
    }

    public List<Part> getParts() {
        return ingredients;
    }

    public ItemStackTemplate getResult() {
        return result;
    }

    public List<Integer> getCountPerTier() {
        return countPerTier;
    }
    public List<Integer> getTimePerTier() {
        return timePerTier;
    }

    /// The result for a core of the given tier (uses "count_per_tier" if the recipe has it).
    /// The amount can be bigger than a full stack (e.g. 128): it is split when it's delivered.
    public ItemStack createResult(int coreTier) {
        ItemStack stack = result.create();
        if (countPerTier.isEmpty()) return stack;

        int index = Math.min(coreTier, countPerTier.size()) - 1; // tier 1 = first number
        stack.setCount(countPerTier.get(Math.max(0, index)));
        return stack;
    }

    /// If the result is a Gravity Core, returns its tier (this recipe is an upgrade). Otherwise returns null.
    public @Nullable CoreTier getUpgradeTier() {
        if (result.item().value() instanceof BlockItem blockItem && blockItem.getBlock() instanceof GravityCoreBlock core) {
            return core.tier;
        }
        return null;
    }

    /// Normal recipes work in this tier or higher; upgrades only work in exactly this tier.
    public boolean worksInTier(int coreTier) {
        return getUpgradeTier() != null ? coreTier == tier : coreTier >= tier;
    }

    /// The recipe works if the core has the right tier, every orbiting item belongs to this
    /// recipe, and there are enough items of every ingredient.
    @Override
    public boolean matches(Input input, Level level) {
        if (!worksInTier(input.tier())) return false;

        // Every orbiting item must be an ingredient of this recipe. An item it doesn't use
        // blocks it — this is how a player building a bigger recipe (e.g. an upgrade) stops a
        // smaller one that shares ingredients from firing halfway through.
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            boolean used = false;
            for (Part part : ingredients) {
                if (part.item().test(stack)) {
                    used = true;
                    break;
                }
            }
            if (!used) return false;
        }

        for (Part part : ingredients) {
            int found = 0;
            for (ItemStack stack : input.items()) {
                if (part.item().test(stack)) found += stack.getCount();
            }
            if (found < part.countFor(input.tier)) return false;
        }
        return true;
    }

    /// Craft duration (in ticks) for a core of the given tier.
    /// Uses "time_per_tier" if present (same rules as "count_per_tier"), otherwise "time".
    public int timeFor(int coreTier) {
        if (timePerTier.isEmpty()) return time;
        int index = Math.min(coreTier, timePerTier.size()) - 1; // tier 1 = first number
        return timePerTier.get(Math.max(0, index));
    }

    /// Turns ticks into text: 100 -> "5.0s", 0 or 1 -> "0s" (the craft is instant).
    public String timeText(int coreTier) {
        int ticks = timeFor(coreTier);
        return ticks <= 1 ? "instant" : String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20f);
    }

    @Override
    public ItemStack assemble(Input input) {
        return createResult(input.tier());
    }

    /// Gravity Core recipes don't show up in the vanilla recipe book
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<GravityCoreRecipe> getSerializer() {
        return ModRecipes.GRAVITY_CRAFT_SERIALIZER.get();
    }

    @Override
    public RecipeType<GravityCoreRecipe> getType() {
        return ModRecipes.GRAVITY_CRAFT_TYPE.get();
    }

    /// How the recipe is read from JSON (CODEC) and sent over the network (STREAM_CODEC)
    public static final MapCodec<GravityCoreRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.optionalFieldOf("tier", 1).forGetter(GravityCoreRecipe::getTier),
            Codec.INT.optionalFieldOf("time", 100).forGetter(GravityCoreRecipe::getTime),
            Part.CODEC.listOf().fieldOf("ingredients").forGetter(GravityCoreRecipe::getParts),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(GravityCoreRecipe::getResult),
            Codec.INT.listOf().optionalFieldOf("count_per_tier", List.of()).forGetter(GravityCoreRecipe::getCountPerTier),
            Codec.INT.listOf().optionalFieldOf("time_per_tier", List.of()).forGetter(GravityCoreRecipe::getTimePerTier)
    ).apply(inst, GravityCoreRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GravityCoreRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}