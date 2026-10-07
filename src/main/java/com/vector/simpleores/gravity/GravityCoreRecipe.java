package com.vector.simpleores.gravity;

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
/// UPGRADE RECIPES: if the result is a Gravity Core (e.g. "simpleores:gravity_core_red_giant"),
/// the recipe is an upgrade. It only works in a core of exactly that "tier", and instead of
/// giving an item, the core swallows the ingredients, slowly turns into the new tier and
/// drops itself on the ground, already upgraded.
public class GravityCoreRecipe implements Recipe<GravityCoreRecipe.Input> {

    /// One ingredient with an amount (e.g. 4x raw_ignithra).
    public record Part(Ingredient item, int count) {
        public static final Codec<Part> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Ingredient.CODEC.fieldOf("item").forGetter(Part::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Part::count)
        ).apply(inst, Part::new));
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

    public GravityCoreRecipe(int tier, int time, List<Part> ingredients, ItemStackTemplate result) {
        this.tier = tier;
        this.time = time;
        this.ingredients = ingredients;
        this.result = result;
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

    /// The recipe works if the core has the right tier and there are enough items of every ingredient.
    @Override
    public boolean matches(Input input, Level level) {
        if (!worksInTier(input.tier())) return false;

        for (Part part : ingredients) {
            int found = 0;
            for (ItemStack stack : input.items()) {
                if (part.item().test(stack)) found += stack.getCount();
            }
            if (found < part.count()) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input) {
        return result.create();
    }

    // Gravity Core recipes don't show up in the vanilla recipe book
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
        return ModRecipes.GRAVITY_COLLAPSE_SERIALIZER.get();
    }

    @Override
    public RecipeType<GravityCoreRecipe> getType() {
        return ModRecipes.GRAVITY_COLLAPSE_TYPE.get();
    }

    // How the recipe is read from JSON (CODEC) and sent over the network (STREAM_CODEC)
    public static final MapCodec<GravityCoreRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.INT.optionalFieldOf("tier", 1).forGetter(GravityCoreRecipe::getTier),
            Codec.INT.optionalFieldOf("time", 100).forGetter(GravityCoreRecipe::getTime),
            Part.CODEC.listOf().fieldOf("ingredients").forGetter(GravityCoreRecipe::getParts),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(GravityCoreRecipe::getResult)
    ).apply(inst, GravityCoreRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GravityCoreRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());
}
