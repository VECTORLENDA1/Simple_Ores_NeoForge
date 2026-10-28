package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.quasar.QuasarMenu;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarSlot;
import com.exoticmoss.simpleores.starstorage.client.QuasarScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// Drag and drop from the JEI list onto the recipe slots of the Quasar (the ones of the tab shown).
/// JEI starts the drag as soon as the left button is pressed on its list, and then a simple click does not
/// show the recipes of the item any more: so a click (released quickly, not dropped on a slot) shows them here.
public class QuasarGhostHandler implements IGhostIngredientHandler<QuasarScreen> {
    /// A press shorter than this (ms) that was not dropped on a slot is a click.
    private static final long CLICK_TIME = 300;

    private static ITypedIngredient<?> pressed;
    private static long pressTime;

    /// Every tick of the Quasar screen: a press that ended without a drop shows the recipes of the item.
    static void tick(IJeiRuntime runtime) {
        if (pressed == null || Minecraft.getInstance().mouseHandler.isLeftPressed()) return;
        ITypedIngredient<?> ingredient = pressed;
        pressed = null;
        if (System.currentTimeMillis() - pressTime <= CLICK_TIME) show(runtime, ingredient);
    }

    private static <V> void show(IJeiRuntime runtime, ITypedIngredient<V> ingredient) {
        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, ingredient));
    }

    @Override
    public <I> List<Target<I>> getTargetsTyped(QuasarScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> item = ingredient.getItemStack();
        if (item.isEmpty()) return List.of();
        if (doStart) {
            pressed = ingredient;
            pressTime = System.currentTimeMillis();
        }
        List<Target<I>> targets = new ArrayList<>();
        for (int i = 0; i < QuasarMenu.INVENTORY_START; i++) {
            Slot slot = screen.getMenu().slots.get(i);
            if (i == QuasarMenu.RESULT_SLOT || !slot.isActive()) continue;
            int index = i;
            Rect2i area = new Rect2i(screen.left() + slot.x, screen.top() + slot.y, 16, 16);
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I ingredient) {
                    pressed = null;
                    PacketDistributor.sendToServer(new QuasarSlot(index, item.get().copyWithCount(1)));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
