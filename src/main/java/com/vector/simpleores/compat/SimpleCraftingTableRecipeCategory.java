package com.vector.simpleores.compat;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SimpleCraftingTableRecipeCategory implements IRecipeCategory<SimpleCraftingTableRecipe> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("simpleores", "textures/gui/simple_crafting_table/simple_crafting_table_gui_jei.png");
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("simpleores", "simple_crafting_table");


    public static final RecipeType<SimpleCraftingTableRecipe> SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE =
            new RecipeType<>(UID, SimpleCraftingTableRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public SimpleCraftingTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176,126 );
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.SIMPLE_CRAFTING_TABLE.get()));
    }

    @Override
    public RecipeType<SimpleCraftingTableRecipe> getRecipeType() {
        return SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.simpleores.simple_crafting_table");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SimpleCraftingTableRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 18).addIngredients(recipe.getIngredients().get(0));
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 18).addIngredients(recipe.getIngredients().get(1));
        builder.addSlot(RecipeIngredientRole.INPUT, 54, 18).addIngredients(recipe.getIngredients().get(2));
        builder.addSlot(RecipeIngredientRole.INPUT, 72, 18).addIngredients(recipe.getIngredients().get(3));
        builder.addSlot(RecipeIngredientRole.INPUT, 90, 18).addIngredients(recipe.getIngredients().get(4));
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 36).addIngredients(recipe.getIngredients().get(5));
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 36).addIngredients(recipe.getIngredients().get(6));
        builder.addSlot(RecipeIngredientRole.INPUT, 54, 36).addIngredients(recipe.getIngredients().get(7));
        builder.addSlot(RecipeIngredientRole.INPUT, 72, 36).addIngredients(recipe.getIngredients().get(8));
        builder.addSlot(RecipeIngredientRole.INPUT, 90, 36).addIngredients(recipe.getIngredients().get(9));
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 54).addIngredients(recipe.getIngredients().get(10));
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 54).addIngredients(recipe.getIngredients().get(11));
        builder.addSlot(RecipeIngredientRole.INPUT, 54, 54).addIngredients(recipe.getIngredients().get(12));
        builder.addSlot(RecipeIngredientRole.INPUT, 72, 54).addIngredients(recipe.getIngredients().get(13));
        builder.addSlot(RecipeIngredientRole.INPUT, 90, 54).addIngredients(recipe.getIngredients().get(14));
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 72).addIngredients(recipe.getIngredients().get(15));
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 72).addIngredients(recipe.getIngredients().get(16));
        builder.addSlot(RecipeIngredientRole.INPUT, 54, 72).addIngredients(recipe.getIngredients().get(17));
        builder.addSlot(RecipeIngredientRole.INPUT, 72, 72).addIngredients(recipe.getIngredients().get(18));
        builder.addSlot(RecipeIngredientRole.INPUT, 90, 72).addIngredients(recipe.getIngredients().get(19));
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 90).addIngredients(recipe.getIngredients().get(20));
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 90).addIngredients(recipe.getIngredients().get(21));
        builder.addSlot(RecipeIngredientRole.INPUT, 54, 90).addIngredients(recipe.getIngredients().get(22));
        builder.addSlot(RecipeIngredientRole.INPUT, 72, 90).addIngredients(recipe.getIngredients().get(23));
        builder.addSlot(RecipeIngredientRole.INPUT, 90, 90).addIngredients(recipe.getIngredients().get(24));

        builder.addSlot(RecipeIngredientRole.OUTPUT, 145, 55).addItemStack(recipe.getResultItem(null));
    }
}