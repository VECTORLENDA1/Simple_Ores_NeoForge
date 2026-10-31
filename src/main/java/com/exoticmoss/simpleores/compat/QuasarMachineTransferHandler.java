package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.ModMenus;
import com.exoticmoss.simpleores.starstorage.quasar.Pattern;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarFill;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// JEI's "+" button on the recipes of the machines of other mods (Mekanism...) while the Quasar screen is open
/// (crafting table, furnace and Gravity Core recipes have their own handlers): puts the ingredients of the recipe
/// in the recipe grid of the Quasar, with their amounts and the result. The machines are the blocks JEI shows
/// for that kind of recipe; the auto-craft uses the ones in the field of the Magnetar.
/// Only items: liquids and the other things of a recipe (gases of Mekanism...) are not used.
public class QuasarMachineTransferHandler implements IUniversalRecipeTransferHandler<QuasarMenu> {
    /// Set by the plugin when JEI is ready (to find the machines of a recipe).
    static @Nullable IJeiRuntime runtime;
    private final IRecipeTransferHandlerHelper helper;

    public QuasarMachineTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
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
    @SuppressWarnings("removal")
    public @Nullable IRecipeTransferError transferRecipe(QuasarMenu container, Object recipe, IRecipeSlotsView recipeSlots,
                                                         Player player, boolean maxTransfer, boolean doTransfer) {
        // The ingredients (one slot of the grid each, up to 9) and the result
        List<List<ItemStack>> slots = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        // Every slot that is not a result: some mods show an extra ingredient (e.g. the redstone of the Metallurgic
        // Infuser of Mekanism) as a slot of another kind
        for (IRecipeSlotView view : recipeSlots.getSlotViews()) {
            if (view.getRole() == RecipeIngredientRole.OUTPUT) continue;
            List<ItemStack> items = view.getItemStacks().filter(stack -> !stack.isEmpty()).limit(QuasarFill.MAX_CHOICES).toList();
            if (items.isEmpty()) continue;
            List<ItemStack> choices = items.stream().map(stack -> stack.copyWithCount(1)).toList();
            // The same item in more slots (e.g. the injectors of the fusion crafting of Draconic Evolution): one slot
            // of the grid with all of them. Never with the first slot: on an altar it is the middle item, with its
            // own amount (e.g. 4 crystals in the Fusion Crafting Core)
            int same = slots.size() > 1 ? indexOf(slots.subList(1, slots.size()), choices) : -1;
            if (same >= 0) same++;
            if (same >= 0) {
                amounts.set(same, amounts.get(same) + items.get(0).getCount());
                continue;
            }
            if (slots.size() >= Pattern.SIZE) continue;
            slots.add(choices);
            amounts.add(items.get(0).getCount());
        }
        ItemStack output = ItemStack.EMPTY;
        for (IRecipeSlotView view : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
            Optional<ItemStack> first = view.getItemStacks().filter(stack -> !stack.isEmpty()).findFirst();
            if (first.isPresent()) {
                output = first.get().copy();
                break;
            }
        }
        if (slots.isEmpty() || output.isEmpty()) {
            return helper.createUserErrorWithTooltip(Component.translatable("screen.simpleores.quasar.machine.no_items"));
        }
        if (!doTransfer) return null;
        List<ItemStack> machines = machines(recipe);
        if (machines.isEmpty()) {
            return helper.createUserErrorWithTooltip(Component.translatable("screen.simpleores.quasar.machine.none"));
        }
        while (slots.size() < Pattern.SIZE) {
            slots.add(List.of());
            amounts.add(1);
        }
        container.tab = QuasarMenu.TAB_CRAFTING;
        PacketDistributor.sendToServer(new QuasarFill(slots, amounts, output, machines));
        return null;
    }

    /// The slot of the grid that has exactly these items (the same choices), or -1.
    private static int indexOf(List<List<ItemStack>> slots, List<ItemStack> choices) {
        for (int i = 0; i < slots.size(); i++) {
            List<ItemStack> slot = slots.get(i);
            if (slot.size() != choices.size()) continue;
            boolean equal = true;
            for (int j = 0; j < slot.size() && equal; j++) equal = ItemStack.isSameItemSameComponents(slot.get(j), choices.get(j));
            if (equal) return i;
        }
        return -1;
    }

    /// The blocks that make this recipe: the ones JEI shows next to its kind of recipe.
    /// (createRecipeCatalystLookup is marked to be removed in a later JEI: it still works in this one.)
    @SuppressWarnings("removal")
    private static List<ItemStack> machines(Object recipe) {
        if (runtime == null) return List.of();
        IRecipeManager manager = runtime.getRecipeManager();
        for (IRecipeCategory<?> category : manager.createRecipeCategoryLookup().get().toList()) {
            RecipeType<?> type = category.getRecipeType();
            if (!type.getRecipeClass().isInstance(recipe) || !has(manager, type, recipe)) continue;
            return manager.createRecipeCatalystLookup(type).getItemStack()
                    .filter(stack -> stack.getItem() instanceof BlockItem)
                    .limit(QuasarFill.MAX_MACHINES).map(stack -> stack.copyWithCount(1)).toList();
        }
        return List.of();
    }

    /// True if the recipe is one of the recipes of this kind.
    private static <R> boolean has(IRecipeManager manager, RecipeType<R> type, Object recipe) {
        return manager.createRecipeLookup(type).get().anyMatch(other -> other == recipe);
    }
}
