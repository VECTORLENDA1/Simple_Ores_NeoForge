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

import java.util.ArrayList;
import java.util.List;
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
        // "Tier 2 - 10s" at the bottom
        Component text = Component.translatable("jei.simpleores.gravity_craft.info", recipe.getTier(), recipe.getTime() / 20f);
        guiGraphics.text(Minecraft.getInstance().font, text, 1, HEIGHT - 9, 0xFF404040, false);

        // "Per tier: 2 / 4 / 8 / 16" above it, only for recipes with "count_per_tier"
        if (!recipe.getCountPerTier().isEmpty()) {
            String counts = recipe.getCountPerTier().stream().map(String::valueOf).collect(Collectors.joining(" / "));
            Component perTier = Component.translatable("jei.simpleores.gravity_craft.per_tier", counts);
            guiGraphics.text(Minecraft.getInstance().font, perTier, 1, HEIGHT - 19, 0xFF404040, false);
        }
    }
}
