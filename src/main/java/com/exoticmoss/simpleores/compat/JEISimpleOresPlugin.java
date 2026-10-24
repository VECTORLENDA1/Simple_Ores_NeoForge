package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import com.exoticmoss.simpleores.recipe.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/// JEI integration.
///
/// The 3 crafting tables are DISABLED: their code is commented out below.
/// To enable them again in JEI, uncomment the parts marked with "CRAFTING TABLES"
/// (and also registerGuiHandlers, which uses their screens).
///
/// On Fabric the plugin is registered with the "jei_mod_plugin" entrypoint in fabric.mod.json.
/// The client no longer receives recipes automatically: the Gravity Core recipes are sent to the
/// players by Fabric's recipe sync (see SimpleOres, RecipeSynchronization), and read here.
@JeiPlugin
public class JEISimpleOresPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(SimpleOres.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new GravityCoreRecipeCategory(guiHelper));

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Given as RecipeHolder (recipe + ID) so JEI can bookmark them
        List<RecipeHolder<GravityCoreRecipe>> gravityRecipes = new ArrayList<>();
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            gravityRecipes.addAll(connection.recipes().getSynchronizedRecipes().getAllOfType(ModRecipes.GRAVITY_CRAFT_TYPE.get()));
        }
        registration.addRecipes(GravityCoreRecipeCategory.RECIPE_TYPE, gravityRecipes);

    }

    // "Move Items" (+) button: works with the survival inventory and the creative inventory open
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        var helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(
                new GravityCoreTransferHandler<>(InventoryMenu.class, helper), GravityCoreRecipeCategory.RECIPE_TYPE);
        registration.addRecipeTransferHandler(
                new GravityCoreTransferHandler<>(CreativeModeInventoryScreen.ItemPickerMenu.class, helper), GravityCoreRecipeCategory.RECIPE_TYPE);
    }

    // Shows the recipes when the block is clicked inside JEI
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(GravityCoreRecipeCategory.RECIPE_TYPE,
                ModBlocks.GRAVITY_CORE_SUN.get(), ModBlocks.GRAVITY_CORE_RED_GIANT.get(),
                ModBlocks.GRAVITY_CORE_PULSAR.get(), ModBlocks.GRAVITY_CORE_BLACK_HOLE.get());

    }
}
