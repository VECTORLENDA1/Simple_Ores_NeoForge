package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreRecipe;
import com.exoticmoss.simpleores.starstorage.ModMenus;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarCore;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/// JEI's "+" button on Gravity Core recipes while the Quasar screen is open: saves the recipe in the Quasar.
public class QuasarCoreTransferHandler implements IRecipeTransferHandler<QuasarMenu, RecipeHolder<GravityCoreRecipe>> {
    @Override
    public Class<? extends QuasarMenu> getContainerClass() {
        return QuasarMenu.class;
    }

    @Override
    public Optional<MenuType<QuasarMenu>> getMenuType() {
        return Optional.of(ModMenus.QUASAR.get());
    }

    @Override
    public RecipeType<RecipeHolder<GravityCoreRecipe>> getRecipeType() {
        return GravityCoreRecipeCategory.RECIPE_TYPE;
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(QuasarMenu container, RecipeHolder<GravityCoreRecipe> recipe,
                                                         IRecipeSlotsView recipeSlots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) PacketDistributor.sendToServer(new QuasarCore(recipe.id()));
        return null;
    }
}
