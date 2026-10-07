package com.vector.simpleores.compat;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.gravitycraft.CoreTier;
import com.vector.simpleores.gravitycraft.GravityCoreRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/// Shows the Gravity Core recipes in JEI:
/// ingredients on the left, result on the right, and the tier and time at the bottom.
///
/// The recipes are given to JEI as a RecipeHolder (recipe + its ID). Thanks to the ID,
/// JEI can show the "Bookmark" button and the "Move Items" button knows which recipe to send.
public class GravityCoreRecipeCategory implements IRecipeCategory<RecipeHolder<GravityCoreRecipe>> {
    public static final IRecipeHolderType<GravityCoreRecipe> RECIPE_TYPE =
            IRecipeHolderType.create(Identifier.fromNamespaceAndPath("simpleores", "gravity_craft"));

    private static final int WIDTH = 160;
    private static final int HEIGHT = 66;
    private static final int COLUMNS = 6; // ingredients per row

    private final IDrawable icon;

    public GravityCoreRecipeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.GRAVITY_CORE_SUN.get()));
    }

    @Override
    public IRecipeHolderType<GravityCoreRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.simpleores.gravity_craft");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<GravityCoreRecipe> holder, IFocusGroup focuses) {
        GravityCoreRecipe recipe = holder.value();
        List<GravityCoreRecipe.Part> parts = recipe.getParts();
        for (int i = 0; i < parts.size(); i++) {
            GravityCoreRecipe.Part part = parts.get(i);
            // Every possible item of the ingredient, already with the right amount
            List<ItemStack> stacks = part.item().items().map(item -> new ItemStack(item, part.count())).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % COLUMNS) * 18, 1 + (i / COLUMNS) * 18)
                    .setStandardSlotBackground()
                    .addItemStacks(stacks);
        }

        // The result. With "count_per_tier" the slot cycles through the amount of every tier.
        List<ItemStack> results = new ArrayList<>();
        for (CoreTier tier : CoreTier.values()) {
            if (recipe.worksInTier(tier.level)) results.add(recipe.createResult(tier.level));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 19)
                .setOutputSlotBackground()
                .addItemStacks(recipe.getCountPerTier().isEmpty() ? List.of(recipe.createResult(recipe.getTier())) : results);
    }

    @Override
    public void draw(RecipeHolder<GravityCoreRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        GravityCoreRecipe recipe = holder.value();
        var font = Minecraft.getInstance().font;

        /// Bottom line: "Tier 2 - 4.0s". It cycles through the tiers (like the result slot does)
        int shownTier = shownTier(recipe, recipeSlotsView);
        Component text = Component.translatable("jei.simpleores.gravity_craft.info",
                shownTier, recipe.timeFor(shownTier) / 20f);
        guiGraphics.text(font, text, 1, HEIGHT - 9, 0xFF404040, false);
    }

    /// Which tier the bottom line should show right now.
    /// If the recipe has "count_per_tier", it follows the amount JEI is showing in the result slot,
    /// so the text and the slot always match. If several tiers give the same amount (or there is no
    /// "count_per_tier"), it cycles between the possible tiers once per second, like JEI does.
    private static int shownTier(GravityCoreRecipe recipe, IRecipeSlotsView slots) {
        List<Integer> tiers = new ArrayList<>();
        for (CoreTier tier : CoreTier.values()) {
            if (recipe.worksInTier(tier.level)) tiers.add(tier.level);
        }
        if (tiers.isEmpty()) return recipe.getTier();
        if (tiers.size() == 1) return tiers.get(0);

        List<Integer> candidates = tiers;
        if (!recipe.getCountPerTier().isEmpty()) {
            Optional<ItemStack> shown = slots.getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                    .findFirst()
                    .flatMap(slot -> slot.getDisplayedIngredient(VanillaTypes.ITEM_STACK));
            if (shown.isPresent()) {
                int count = shown.get().getCount();
                List<Integer> matching = tiers.stream()
                        .filter(tier -> recipe.createResult(tier).getCount() == count)
                        .toList();
                if (!matching.isEmpty()) candidates = matching;
            }
        }
        int index = (int) ((System.currentTimeMillis() / 1000) % candidates.size());
        return candidates.get(index);
    }
}
