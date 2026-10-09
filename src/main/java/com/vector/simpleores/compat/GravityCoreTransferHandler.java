package com.vector.simpleores.compat;

import com.vector.simpleores.gravitycraft.GravityCoreBlockEntity;
import com.vector.simpleores.gravitycraft.GravityCoreRecipe;
import com.vector.simpleores.gravitycraft.MoveItemsToCore;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// The "Move Items" (+) button on Gravity Core recipes in JEI.
///
/// It works while the player's inventory is open: instead of moving items into a crafting grid,
/// it sends the ingredients from the player's inventory into the nearest Gravity Core
/// (the real work happens on the server, in MoveItemsToCore).
///
/// Before that, it checks what's missing so JEI can show it:
///  - missing ingredients are highlighted in red (like normal recipes), using the amounts
///    for the tier of the nearest core;
///  - if there is no suitable core nearby, the button shows a message.
public class GravityCoreTransferHandler<C extends AbstractContainerMenu>
        implements IRecipeTransferHandler<C, RecipeHolder<GravityCoreRecipe>> {
    private final Class<? extends C> containerClass;
    private final IRecipeTransferHandlerHelper helper;

    public GravityCoreTransferHandler(Class<? extends C> containerClass, IRecipeTransferHandlerHelper helper) {
        this.containerClass = containerClass;
        this.helper = helper;
    }

    @Override
    public Class<? extends C> getContainerClass() {
        return containerClass;
    }

    @Override
    public Optional<MenuType<C>> getMenuType() {
        return Optional.empty(); // the player inventory has no menu type
    }

    @Override
    public IRecipeType<RecipeHolder<GravityCoreRecipe>> getRecipeType() {
        return GravityCoreRecipeCategory.RECIPE_TYPE;
    }

    /// Called by JEI when the "+" button is shown (doTransfer = false) and when it is clicked (doTransfer = true).
    /// Returning null means "OK"; returning an error shows it on the button.
    @Override
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(C container, RecipeHolder<GravityCoreRecipe> holder,
                                                         IRecipeSlotsView recipeSlots, Player player,
                                                         boolean maxTransfer, boolean doTransfer) {
        GravityCoreRecipe recipe = holder.value();

        // The amounts can depend on the tier of the core ("count_per_tier"), so first find the core.
        // Without a core nearby, the amounts of the recipe's own tier are used to mark what's missing.
        GravityCoreBlockEntity core = MoveItemsToCore.findCore(player, recipe);
        int coreTier = core != null ? core.getTier().level : recipe.getTier();

        // 1. Mark in red the ingredients the player doesn't have enough of.
        //    The JEI input slots are in the same order as the recipe ingredients.
        List<IRecipeSlotView> inputSlots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        List<IRecipeSlotView> missing = new ArrayList<>();
        List<GravityCoreRecipe.Part> parts = recipe.getParts();
        for (int i = 0; i < parts.size() && i < inputSlots.size(); i++) {
            if (MoveItemsToCore.countInInventory(player, parts.get(i)) < parts.get(i).countFor(coreTier)) {
                missing.add(inputSlots.get(i));
            }
        }
        if (!missing.isEmpty()) {
            return helper.createUserErrorForMissingSlots(Component.translatable("jei.simpleores.missing_items"), missing);
        }

        // 2. There must be a core nearby that can make this recipe.
        if (core == null) {
            return helper.createUserErrorWithTooltip(Component.translatable("jei.simpleores.no_core_nearby", MoveItemsToCore.RANGE));
        }

        // 3. Everything is OK: when clicked, ask the server to move the items.
        if (doTransfer) {
            ClientPacketDistributor.sendToServer(new MoveItemsToCore(holder.id(), maxTransfer));
        }
        return null;
    }
}