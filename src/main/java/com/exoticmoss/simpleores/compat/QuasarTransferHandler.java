package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.ModMenus;
import com.exoticmoss.simpleores.starstorage.Pattern;
import com.exoticmoss.simpleores.starstorage.QuasarFill;
import com.exoticmoss.simpleores.starstorage.QuasarMenu;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// JEI's "+" button on crafting table recipes while the Quasar screen is open: puts the recipe in the
/// recipe grid of the Quasar (each slot with every item JEI shows for it, e.g. every kind of planks).
public class QuasarTransferHandler implements IRecipeTransferHandler<QuasarMenu, RecipeHolder<CraftingRecipe>> {
    @Override
    public Class<? extends QuasarMenu> getContainerClass() {
        return QuasarMenu.class;
    }

    @Override
    public Optional<MenuType<QuasarMenu>> getMenuType() {
        return Optional.of(ModMenus.QUASAR.get());
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(QuasarMenu container, RecipeHolder<CraftingRecipe> recipe,
                                                         IRecipeSlotsView recipeSlots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            List<List<ItemStack>> slots = new ArrayList<>();
            for (int i = 0; i < Pattern.SIZE; i++) {
                List<ItemStack> items = new ArrayList<>();
                if (i < inputs.size()) {
                    inputs.get(i).getItemStacks().limit(QuasarFill.MAX_CHOICES).forEach(stack -> items.add(stack.copyWithCount(1)));
                }
                slots.add(items);
            }
            container.tab = QuasarMenu.TAB_CRAFTING;
            ClientPacketDistributor.sendToServer(new QuasarFill(slots));
        }
        return null;
    }
}
