package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.ModMenus;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarMenu;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarSlot;
import com.exoticmoss.simpleores.starstorage.client.QuasarScreen;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/// JEI's "+" button on furnace recipes (Furnace, Blast Furnace and Smoker) while the Quasar screen is open: puts the
/// input in the furnace tab, with that kind of furnace chosen.
public class QuasarFurnaceTransferHandler<R extends AbstractCookingRecipe> implements IRecipeTransferHandler<QuasarMenu, RecipeHolder<R>> {
    private final RecipeType<RecipeHolder<R>> type;
    private final int kind;

    public QuasarFurnaceTransferHandler(RecipeType<RecipeHolder<R>> type, int kind) {
        this.type = type;
        this.kind = kind;
    }

    @Override
    public Class<? extends QuasarMenu> getContainerClass() {
        return QuasarMenu.class;
    }

    @Override
    public Optional<MenuType<QuasarMenu>> getMenuType() {
        return Optional.of(ModMenus.QUASAR.get());
    }

    @Override
    public RecipeType<RecipeHolder<R>> getRecipeType() {
        return type;
    }

    @Override
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(QuasarMenu container, RecipeHolder<R> recipe,
                                                         IRecipeSlotsView recipeSlots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            Optional<ItemStack> input = inputs.isEmpty() ? Optional.empty() : inputs.get(0).getItemStacks().findFirst();
            if (input.isPresent()) {
                if (Minecraft.getInstance().screen instanceof QuasarScreen screen) {
                    screen.setTab(QuasarMenu.TAB_FURNACE);
                    screen.setCooking(kind);
                }
                PacketDistributor.sendToServer(new QuasarSlot(QuasarMenu.FURNACE_SLOT, input.get().copyWithCount(1)));
            }
        }
        return null;
    }
}
