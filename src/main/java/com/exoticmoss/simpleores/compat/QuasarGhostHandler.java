package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.QuasarMenu;
import com.exoticmoss.simpleores.starstorage.QuasarSlot;
import com.exoticmoss.simpleores.starstorage.client.QuasarScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// Drag and drop from the JEI list onto the recipe slots of the Quasar (the ones of the tab shown).
public class QuasarGhostHandler implements IGhostIngredientHandler<QuasarScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(QuasarScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> item = ingredient.getItemStack();
        if (item.isEmpty()) return List.of();
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
                    ClientPacketDistributor.sendToServer(new QuasarSlot(index, item.get().copyWithCount(1)));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
