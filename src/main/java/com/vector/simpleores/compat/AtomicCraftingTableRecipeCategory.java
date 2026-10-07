package com.vector.simpleores.compat;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.recipe.AtomicCraftingTableRecipe;
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

public class AtomicCraftingTableRecipeCategory implements IRecipeCategory<AtomicCraftingTableRecipe> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simpleores", "textures/gui/atomic_crafting_table/atomic_crafting_table_gui_jei.png");
    public static final Identifier UID = Identifier.fromNamespaceAndPath("simpleores", "atomic_crafting_table");

    public static final IRecipeType<AtomicCraftingTableRecipe> ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE =
            IRecipeType.create(UID, AtomicCraftingTableRecipe.class);

    private static final int WIDTH = 248;
    private static final int HEIGHT = 198;

    // Input grid: 9 x 9, 18px slots starting at (18, 18)
    private static final int COLUMNS = 9;
    private static final int ROWS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_START = 18;

    private static final int OUTPUT_X = 218;
    private static final int OUTPUT_Y = 90;

    private final IDrawable background;
    private final IDrawable icon;

    public AtomicCraftingTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, WIDTH, HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ATOMIC_CRAFTING_TABLE.get()));
    }

    @Override
    public IRecipeType<AtomicCraftingTableRecipe> getRecipeType() {
        return ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.simpleores.atomic_crafting_table");
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
    public void draw(AtomicCraftingTableRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AtomicCraftingTableRecipe recipe, IFocusGroup focuses) {
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