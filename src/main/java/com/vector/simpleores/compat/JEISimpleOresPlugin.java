package com.vector.simpleores.compat;


import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.recipe.AtomicCraftingTableRecipe;
import com.vector.simpleores.recipe.ModRecipes;
import com.vector.simpleores.recipe.SimpleCraftingTableRecipe;
import com.vector.simpleores.recipe.UltraCraftingTableRecipe;
import com.vector.simpleores.screen.custom.SimpleCraftingTableScreen;
import com.vector.simpleores.screen.custom.UltraCraftingTableScreen;
import com.vector.simpleores.screen.custom.AtomicCraftingTableScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;


@JeiPlugin
public class JEISimpleOresPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("simpleores", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SimpleCraftingTableRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new UltraCraftingTableRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AtomicCraftingTableRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<SimpleCraftingTableRecipe> simpleCraftingTableRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.SIMPLE_CRAFTING_TABLE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE, simpleCraftingTableRecipes);

        List<UltraCraftingTableRecipe> ultraCraftingTableRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.ULTRA_CRAFTING_TABLE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE, ultraCraftingTableRecipes);

        List<AtomicCraftingTableRecipe> atomicCraftingTableRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.ATOMIC_CRAFTING_TABLE_TYPE.get())
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE, atomicCraftingTableRecipes);
    }

    // A clickable area for the Crafting Table arrow
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(SimpleCraftingTableScreen.class,112,54,22,16,
                SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);

        registration.addRecipeClickArea(UltraCraftingTableScreen.class,148,73,22,16,
                UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);

        registration.addRecipeClickArea(AtomicCraftingTableScreen.class,185,91,22,16,
                AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    }

    // You can houver the Crafting Table in JEI to see the recipes
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SIMPLE_CRAFTING_TABLE.get().asItem()),
                SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ULTRA_CRAFTING_TABLE.get().asItem()),
                UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ATOMIC_CRAFTING_TABLE.get().asItem()),
                AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    }
}