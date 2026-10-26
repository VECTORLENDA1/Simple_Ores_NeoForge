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
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.List;

/// JEI integration.
///
/// The 3 crafting tables are DISABLED: their code is commented out below.
/// To enable them again in JEI, uncomment the parts marked with "CRAFTING TABLES"
/// (and also registerGuiHandlers, which uses their screens).
@JeiPlugin
public class JEISimpleOresPlugin implements IModPlugin {
    // The client no longer receives recipes automatically, so we keep the ones the server sends
    private static RecipeMap syncedRecipes = RecipeMap.EMPTY;

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
        List<RecipeHolder<GravityCoreRecipe>> gravityRecipes =
                new ArrayList<>(syncedRecipes.byType(ModRecipes.GRAVITY_CRAFT_TYPE.get()));
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
        // "+" on crafting table recipes with the Quasar screen open: puts the recipe in its grid
        registration.addRecipeTransferHandler(new QuasarTransferHandler(), mezz.jei.api.constants.RecipeTypes.CRAFTING);
        // "+" on Gravity Core recipes with the Quasar screen open: saves the recipe in the Quasar
        registration.addRecipeTransferHandler(new QuasarCoreTransferHandler(), GravityCoreRecipeCategory.RECIPE_TYPE);
        // "+" on furnace recipes with the Quasar screen open: puts the input in the furnace tab
        registration.addRecipeTransferHandler(new QuasarFurnaceTransferHandler(), mezz.jei.api.constants.RecipeTypes.SMELTING);
    }

    // The Quasar screen shows recipes in JEI (left-click on a saved recipe or on an item of the JEI list)
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        com.exoticmoss.simpleores.starstorage.client.QuasarScreen.showRecipes = stack -> {
            if (!stack.isEmpty()) runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory()
                    .createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack));
        };
        com.exoticmoss.simpleores.starstorage.client.QuasarScreen.jeiTick = () -> QuasarGhostHandler.tick(runtime);
    }

    // Drag and drop from the JEI list onto the recipe grid of the Quasar
    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(com.exoticmoss.simpleores.starstorage.client.QuasarScreen.class, new QuasarGhostHandler());
        // R / U (and the clicks of JEI) on the items drawn by the screens of the star storage
        registration.addGuiContainerHandler(StarScreenJeiHandler.screen(com.exoticmoss.simpleores.starstorage.client.TerminalScreen.class), new StarScreenJeiHandler());
        registration.addGuiContainerHandler(StarScreenJeiHandler.screen(com.exoticmoss.simpleores.starstorage.client.QuasarScreen.class), new StarScreenJeiHandler());
        registration.addGuiContainerHandler(StarScreenJeiHandler.screen(com.exoticmoss.simpleores.starstorage.client.BinaryScreen.class), new StarScreenJeiHandler());
    }

    // Shows the recipes when the block is clicked inside JEI
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(GravityCoreRecipeCategory.RECIPE_TYPE,
                ModBlocks.GRAVITY_CORE_SUN.get(), ModBlocks.GRAVITY_CORE_RED_GIANT.get(),
                ModBlocks.GRAVITY_CORE_PULSAR.get(), ModBlocks.GRAVITY_CORE_BLACK_HOLE.get());

    }

    // Server: asks for our recipes to be sent to the players
    @EventBusSubscriber(modid = SimpleOres.MODID)
    public static class ServerRecipeSync {
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            event.sendRecipes(
                    ModRecipes.GRAVITY_CRAFT_TYPE.get());
        }
    }

    // Client: keeps the recipes received from the server so JEI can use them
    @EventBusSubscriber(modid = SimpleOres.MODID, value = Dist.CLIENT)
    public static class ClientRecipeSync {
        @SubscribeEvent
        public static void onRecipesReceived(RecipesReceivedEvent event) {
            syncedRecipes = event.getRecipeMap();
        }
    }
}
