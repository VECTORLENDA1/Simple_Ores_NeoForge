package com.vector.simpleores.compat;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.gravity.GravityCoreRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/// Mostra as receitas do Nucleo Gravitacional no JEI:
/// ingredientes a esquerda, resultado a direita e, por baixo, o tier e o tempo.
public class GravityCoreRecipeCategory implements IRecipeCategory<GravityCoreRecipe> {
    public static final IRecipeType<GravityCoreRecipe> RECIPE_TYPE =
            IRecipeType.create(Identifier.fromNamespaceAndPath("simpleores", "gravity_collapse"), GravityCoreRecipe.class);

    private static final int WIDTH = 160;
    private static final int HEIGHT = 66;
    private static final int COLUMNS = 6; // ingredientes por linha

    private final IDrawable icon;

    public GravityCoreRecipeCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.GRAVITY_CORE_SUN.get()));
    }

    @Override
    public IRecipeType<GravityCoreRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.simpleores.gravity_collapse");
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
    public void setRecipe(IRecipeLayoutBuilder builder, GravityCoreRecipe recipe, IFocusGroup focuses) {
        List<GravityCoreRecipe.Part> parts = recipe.getParts();
        for (int i = 0; i < parts.size(); i++) {
            GravityCoreRecipe.Part part = parts.get(i);
            // Cada item possivel do ingrediente, ja com a quantidade certa
            List<ItemStack> stacks = part.item().items().map(item -> new ItemStack(item, part.count())).toList();
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % COLUMNS) * 18, 1 + (i / COLUMNS) * 18)
                    .setStandardSlotBackground()
                    .addItemStacks(stacks);
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 19)
                .setOutputSlotBackground()
                .add(recipe.getResult());
    }

    @Override
    public void draw(GravityCoreRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        // "Tier 2 - 10s" na parte de baixo
        Component text = Component.translatable("jei.simpleores.gravity_collapse.info", recipe.getTier(), recipe.getTime() / 20f);
        guiGraphics.text(Minecraft.getInstance().font, text, 1, HEIGHT - 9, 0xFF404040, false);
    }
}
