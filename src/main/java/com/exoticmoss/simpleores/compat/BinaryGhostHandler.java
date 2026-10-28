package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.binary.BinaryAction;
import com.exoticmoss.simpleores.starstorage.client.BinaryScreen;
import com.exoticmoss.simpleores.starstorage.network.StarFluids;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// Drag and drop (left mouse button) from the JEI list onto the slots of the Binary Star screen: the slot keeps
/// that item (like a left-click with it in the mouse cursor). A liquid (or its bucket) can be dropped on a tank.
/// Like in the Quasar, a quick click on the JEI list (not dropped on a slot) still shows the recipes of the item.
public class BinaryGhostHandler implements IGhostIngredientHandler<BinaryScreen> {
    /// A press shorter than this (ms) that was not dropped on a slot is a click.
    private static final long CLICK_TIME = 300;

    private static ITypedIngredient<?> pressed;
    private static long pressTime;

    /// Every tick of the Binary Star screen: a press that ended without a drop shows the recipes of the item.
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
    public <I> List<Target<I>> getTargetsTyped(BinaryScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        Optional<ItemStack> item = ingredient.getItemStack();
        // A liquid of JEI: its bucket
        ItemStack bucket = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).map(FluidStack::getFluid)
                .map(StarFluids::bucket).orElse(ItemStack.EMPTY);
        if (item.isEmpty() && bucket.isEmpty()) return List.of();
        if (doStart) {
            pressed = ingredient;
            pressTime = System.currentTimeMillis();
        }
        ItemStack stack = item.orElse(bucket).copyWithCount(1);
        boolean liquid = !bucket.isEmpty() || StarFluids.fromBucket(stack) != null;
        List<Target<I>> targets = new ArrayList<>();
        for (BinaryScreen.DropSlot slot : screen.dropSlots()) {
            // Tanks take only liquids
            if (slot.tank() && !liquid) continue;
            Rect2i area = new Rect2i(slot.x(), slot.y(), 16, 16);
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return area;
                }

                @Override
                public void accept(I ingredient) {
                    pressed = null;
                    PacketDistributor.sendToServer(new BinaryAction.Ghost(screen.getMenu().pos, slot.block(), slot.slot(), stack));
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }
}
