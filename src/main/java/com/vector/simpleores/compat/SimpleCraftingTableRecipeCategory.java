package com.vector.simpleores.compat;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class SimpleCraftingTableRecipeCategory implements IRecipeCategory<SimpleCraftingTableRecipe> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simpleores", "textures/gui/simple_crafting_table/simple_crafting_table_gui_jei.png");
    public static final Identifier UID = Identifier.fromNamespaceAndPath("simpleores", "simple_crafting_table");

    public static final IRecipeType<SimpleCraftingTableRecipe> SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE =
            IRecipeType.create(UID, SimpleCraftingTableRecipe.class);

    private static final int WIDTH = 176;
    private static final int HEIGHT = 126;

    // Input grid: 5 x 5, 18px slots starting at (18, 18)
    private static final int COLUMNS = 5;
    private static final int ROWS = 5;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_START = 18;

    private static final int OUTPUT_X = 145;
    private static final int OUTPUT_Y = 55;

    private final IDrawable background;
    private final IDrawable icon;

    public SimpleCraftingTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.SIMPLE_CRAFTING_TABLE.get()));
    }

    @Override
    public IRecipeType<SimpleCraftingTableRecipe> getRecipeType() {
        return SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.simpleores.simple_crafting_table");
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
    public void draw(SimpleCraftingTableRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SimpleCraftingTableRecipe recipe, IFocusGroup focuses) {
        var ingredients = recipe.getIngredients();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                builder.addSlot(RecipeIngredientRole.INPUT,
                                GRID_START + col * SLOT_SIZE,
                                GRID_START + row * SLOT_SIZE)
                        .add(Ingredient.optionalIngredientToDisplay(ingredients.get(row * COLUMNS + col)));
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .add(recipe.getResult());
    }
}