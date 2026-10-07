package com.vector.simpleores.compat;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.recipe.UltraCraftingTableRecipe;
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

public class UltraCraftingTableRecipeCategory implements IRecipeCategory<UltraCraftingTableRecipe> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simpleores", "textures/gui/ultra_crafting_table/ultra_crafting_table_gui_jei.png");
    public static final Identifier UID = Identifier.fromNamespaceAndPath("simpleores", "ultra_crafting_table");

    public static final IRecipeType<UltraCraftingTableRecipe> ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE =
            IRecipeType.create(UID, UltraCraftingTableRecipe.class);

    private static final int WIDTH = 212;
    private static final int HEIGHT = 162;

    // Input grid: 7 x 7, 18px slots
    private static final int COLUMNS = 7;
    private static final int ROWS = 7;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_START = 18;

    private static final int OUTPUT_X = 181;
    private static final int OUTPUT_Y = 72;

    private final IDrawable background;
    private final IDrawable icon;

    public UltraCraftingTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ULTRA_CRAFTING_TABLE.get()));
    }

    @Override
    public IRecipeType<UltraCraftingTableRecipe> getRecipeType() {
        return ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.simpleores.ultra_crafting_table");
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
    public void draw(UltraCraftingTableRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, UltraCraftingTableRecipe recipe, IFocusGroup focuses) {
        var ingredients = recipe.getIngredients();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = row * COLUMNS + col;
                if (index < ingredients.size()) {
                    builder.addSlot(RecipeIngredientRole.INPUT,
                                    GRID_START + col * SLOT_SIZE,
                                    GRID_START + row * SLOT_SIZE)
                            .add(Ingredient.optionalIngredientToDisplay(ingredients.get(index)));
                }
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .add(recipe.getResult());
    }
}