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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.List;

/// Integracao com o JEI.
///
/// As 3 crafting tables estao DESATIVADAS: o codigo delas esta comentado mais abaixo.
/// Para as voltar a ativar no JEI, descomenta as partes marcadas com "CRAFTING TABLES"
/// (e tambem o registerGuiHandlers, que usa os ecras delas).
@JeiPlugin
public class JEISimpleOresPlugin implements IModPlugin {
    // O cliente ja nao recebe as receitas automaticamente, por isso guardamos as que o servidor envia
    private static RecipeMap syncedRecipes = RecipeMap.EMPTY;

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(SimpleOres.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new GravityCoreRecipeCategory(guiHelper));

        // CRAFTING TABLES (desativadas)
        //registration.addRecipeCategories(new SimpleCraftingTableRecipeCategory(guiHelper));
        //registration.addRecipeCategories(new UltraCraftingTableRecipeCategory(guiHelper));
        //registration.addRecipeCategories(new AtomicCraftingTableRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<GravityCoreRecipe> gravityRecipes = syncedRecipes
                .byType(ModRecipes.GRAVITY_COLLAPSE_TYPE.get())
                .stream().map(RecipeHolder::value).toList();
        registration.addRecipes(GravityCoreRecipeCategory.RECIPE_TYPE, gravityRecipes);

        // CRAFTING TABLES (desativadas)
        //registration.addRecipes(SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        syncedRecipes.byType(ModRecipes.SIMPLE_CRAFTING_TABLE_TYPE.get()).stream().map(RecipeHolder::value).toList());
        //registration.addRecipes(UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        syncedRecipes.byType(ModRecipes.ULTRA_CRAFTING_TABLE_TYPE.get()).stream().map(RecipeHolder::value).toList());
        //registration.addRecipes(AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        syncedRecipes.byType(ModRecipes.ATOMIC_CRAFTING_TABLE_TYPE.get()).stream().map(RecipeHolder::value).toList());
    }

    // CRAFTING TABLES (desativadas): area clicavel na seta de cada mesa
    //@Override
    //public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    //    registration.addRecipeClickArea(SimpleCraftingTableScreen.class, 112, 54, 22, 16,
    //            SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    //    registration.addRecipeClickArea(UltraCraftingTableScreen.class, 148, 73, 22, 16,
    //            UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    //    registration.addRecipeClickArea(AtomicCraftingTableScreen.class, 185, 91, 22, 16,
    //            AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE);
    //}

    // Mostra as receitas ao clicar no bloco dentro do JEI
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(GravityCoreRecipeCategory.RECIPE_TYPE,
                ModBlocks.GRAVITY_CORE_SUN.get(), ModBlocks.GRAVITY_CORE_RED_GIANT.get(),
                ModBlocks.GRAVITY_CORE_PULSAR.get(), ModBlocks.GRAVITY_CORE_BLACK_HOLE.get());

        // CRAFTING TABLES (desativadas)
        //registration.addCraftingStation(SimpleCraftingTableRecipeCategory.SIMPLE_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        ModBlocks.SIMPLE_CRAFTING_TABLE.get());
        //registration.addCraftingStation(UltraCraftingTableRecipeCategory.ULTRA_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        ModBlocks.ULTRA_CRAFTING_TABLE.get());
        //registration.addCraftingStation(AtomicCraftingTableRecipeCategory.ATOMIC_CRAFTING_TABLE_RECIPE_CATEGORY_RECIPE_TYPE,
        //        ModBlocks.ATOMIC_CRAFTING_TABLE.get());
    }

    // Servidor: pede para enviar as nossas receitas aos jogadores
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

    // Cliente: guarda as receitas recebidas do servidor para o JEI usar
    @EventBusSubscriber(modid = SimpleOres.MODID, value = Dist.CLIENT)
    public static class ClientRecipeSync {
        @SubscribeEvent
        public static void onRecipesReceived(RecipesReceivedEvent event) {
            syncedRecipes = event.getRecipeMap();
        }
    }
}
