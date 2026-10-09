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
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

/// Shows the Gravity Core recipes in JEI:
/// ingredients on the left, result on the right, and the tier and time at the bottom.
/// The bottom line cycles through the tiers once per second (real clock), and the amounts drawn on
/// the slots ("count_per_tier") follow the same clock, so they always belong to the tier shown.
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

        // The slots only show the items (amount 1, so JEI draws no number). The amount is drawn on top
        // by AmountOverlay, following the same clock as the bottom line: JEI stops cycling its slots
        // while shift is held down, and this way the amounts keep changing with the tier anyway.
        for (int i = 0; i < parts.size(); i++) {
            GravityCoreRecipe.Part part = parts.get(i);
            List<ItemStack> stacks = part.item().items().map(item -> new ItemStack(item, 1)).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % COLUMNS) * 18, 1 + (i / COLUMNS) * 18)
                    .setStandardSlotBackground()
                    .addItemStacks(stacks)
                    .setOverlay(new AmountOverlay(recipe, part::countFor), 0, 0);
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 19)
                .setOutputSlotBackground().add(recipe.createResult(recipe.getTier()).copyWithCount(1))
                .setOverlay(new AmountOverlay(recipe, tier -> recipe.createResult(tier).getCount()), 0, 0);
    }

    /// Draws the amount of a slot (bottom right, like Minecraft does) for the tier the bottom line shows.
    /// Nothing is drawn when the amount is 1, also like Minecraft.
    private record AmountOverlay(GravityCoreRecipe recipe, IntUnaryOperator amountForTier) implements IDrawable {
        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
            int amount = amountForTier.applyAsInt(shownTier(recipe));
            if (amount == 1) return;
            var font = Minecraft.getInstance().font;
            String text = String.valueOf(amount);
            guiGraphics.text(font, Component.literal(text), xOffset + 17 - font.width(text), yOffset + 9, 0xFFFFFFFF, true);
        }
    }

    @Override
    public void draw(RecipeHolder<GravityCoreRecipe> holder, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        GravityCoreRecipe recipe = holder.value();
        var font = Minecraft.getInstance().font;

        /// Bottom line: "Tier 2 - 4.0s". It cycles through the tiers once per second
        int shownTier = shownTier(recipe);
        Component text = Component.translatable("jei.simpleores.gravity_craft.info",
                shownTier, recipe.timeFor(shownTier) / 20f);
        guiGraphics.text(font, text, 1, HEIGHT - 9, 0xFF404040, false);
    }

    /// Tiers in which the recipe works (e.g. 1, 2, 3, 4 for a tier 1 recipe; only 2 for an upgrade from tier 2).
    private static List<Integer> tiersOf(GravityCoreRecipe recipe) {
        List<Integer> tiers = new ArrayList<>();
        for (CoreTier tier : CoreTier.values()) {
            if (recipe.worksInTier(tier.level)) tiers.add(tier.level);
        }
        if (tiers.isEmpty()) tiers.add(recipe.getTier());
        return tiers;
    }

    /// Which tier the bottom line shows right now: it changes every second, using the real clock,
    /// so it keeps cycling everywhere (recipe screen, bookmarks, inventory, with or without shift).
    private static int shownTier(GravityCoreRecipe recipe) {
        List<Integer> tiers = tiersOf(recipe);
        return tiers.get((int) ((System.currentTimeMillis() / 1000) % tiers.size()));
    }
}