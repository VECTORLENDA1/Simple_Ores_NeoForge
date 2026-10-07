package com.vector.simpleores.compat;

import com.vector.simpleores.SimpleOres;
import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.gravity.GravityCoreRecipe;
import com.vector.simpleores.recipe.ModRecipes;
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

        // CRAFTING TABLES (disabled)
        //registration.addRecipeCategories(new SimpleCraftingTableRecipeCategory(guiHelper));
        //registration.addRecipeCategories(new UltraCraftingTableRecipeCategory(guiHelper));
        //registration.addRecipeCategories(new AtomicCraftingTableRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Given as RecipeHolder (recipe + ID) so JEI can bookmark them
        List<RecipeHolder<GravityCoreRecipe>> gravityRecipes =
                new ArrayList<>(syncedRecipes.byType(ModRecipes.GRAVITY_COLLAPSE_TYPE.get()));
        registration.addRecipes(GravityCoreRecipeCategory.RECIPE_TYPE, gravityRecipes);

        // CRAFTING TABLES (disabled)
        //registration.addRecipes(SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        syncedRecipes.byType(ModRecipes.SIMPLE_CRAFTING_TABLE_TYPE.get()).stream().map(RecipeHolder::value).toList());
        //registration.addRecipes(UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        syncedRecipes.byType(ModRecipes.ULTRA_CRAFTING_TABLE_TYPE.get()).stream().map(RecipeHolder::value).toList());
        //registration.addRecipes(AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        syncedRecipes.byType(ModRecipes.ATOMIC_CRAFTING_TABLE_TYPE.get()).stream().map(RecipeHolder::value).toList());
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

    // CRAFTING TABLES (disabled): clickable area on the arrow of each table
    //@Override
    //public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    //    registration.addRecipeClickArea(SimpleCraftingTableScreen.class, 112, 54, 22, 16,
    //            SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    //    registration.addRecipeClickArea(UltraCraftingTableScreen.class, 148, 73, 22, 16,
    //            UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    //    registration.addRecipeClickArea(AtomicCraftingTableScreen.class, 185, 91, 22, 16,
    //            AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    //}

    // Shows the recipes when the block is clicked inside JEI
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(GravityCoreRecipeCategory.RECIPE_TYPE,
                ModBlocks.GRAVITY_CORE_SUN.get(), ModBlocks.GRAVITY_CORE_RED_GIANT.get(),
                ModBlocks.GRAVITY_CORE_PULSAR.get(), ModBlocks.GRAVITY_CORE_BLACK_HOLE.get());

        // CRAFTING TABLES (disabled)
        //registration.addCraftingStation(SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        ModBlocks.SIMPLE_CRAFTING_TABLE.get());
        //registration.addCraftingStation(UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        ModBlocks.ULTRA_CRAFTING_TABLE.get());
        //registration.addCraftingStation(AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        ModBlocks.ATOMIC_CRAFTING_TABLE.get());
    }

    // Server: asks for our recipes to be sent to the players
    @EventBusSubscriber(modid = SimpleOres.MODID)
    public static class ServerRecipeSync {
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            event.sendRecipes(
                    ModRecipes.GRAVITY_COLLAPSE_TYPE.get(),
                    ModRecipes.SIMPLE_CRAFTING_TABLE_TYPE.get(),
                    ModRecipes.ULTRA_CRAFTING_TABLE_TYPE.get(),
                    ModRecipes.ATOMIC_CRAFTING_TABLE_TYPE.get());
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
