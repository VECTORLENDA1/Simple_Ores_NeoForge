package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.ModMenus;
import com.exoticmoss.simpleores.starstorage.QuasarMenu;
import com.exoticmoss.simpleores.starstorage.QuasarSlot;
import com.exoticmoss.simpleores.starstorage.client.QuasarScreen;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/// JEI's "+" button on furnace recipes while the Quasar screen is open: puts the input in the furnace tab.
public class QuasarFurnaceTransferHandler implements IRecipeTransferHandler<QuasarMenu, RecipeHolder<SmeltingRecipe>> {
    @Override
    public Class<? extends QuasarMenu> getContainerClass() {
        return QuasarMenu.class;
    }

    @Override
    public Optional<MenuType<QuasarMenu>> getMenuType() {
        return Optional.of(ModMenus.QUASAR.get());
    }

    @Override
    public IRecipeType<RecipeHolder<SmeltingRecipe>> getRecipeType() {
        return RecipeTypes.SMELTING;
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(QuasarMenu container, RecipeHolder<SmeltingRecipe> recipe,
                                                         IRecipeSlotsView recipeSlots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            Optional<ItemStack> input = inputs.isEmpty() ? Optional.empty() : inputs.get(0).getItemStacks().findFirst();
            if (input.isPresent()) {
                if (Minecraft.getInstance().gui.screen() instanceof QuasarScreen screen) screen.setTab(QuasarMenu.TAB_FURNACE);
                ClientPacketDistributor.sendToServer(new QuasarSlot(QuasarMenu.FURNACE_SLOT, input.get().copyWithCount(1)));
            }
        }
        return null;
    }
}
