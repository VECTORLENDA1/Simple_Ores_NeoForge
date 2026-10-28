package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.client.JeiTarget;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

/// JEI on the screens of the star storage (Stellar Terminal, Quasar, Binary Star): the items they draw themselves
/// (not in slots) are given to JEI, so R (recipes), U (uses) and the other keys of JEI work on them.
public class StarScreenJeiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {
    /// The class of a screen, for the registration (the screens of the star storage are generic).
    @SuppressWarnings("unchecked")
    public static Class<? extends AbstractContainerScreen<?>> screen(Class<?> screen) {
        return (Class<? extends AbstractContainerScreen<?>>) screen;
    }

    /// Set by the plugin when JEI is ready.
    static IIngredientManager ingredients;

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(AbstractContainerScreen<?> screen,
                                                                              double mouseX, double mouseY) {
        if (ingredients == null || !(screen instanceof JeiTarget.Source source)) return Optional.empty();
        JeiTarget target = source.jeiTargetAt(mouseX, mouseY);
        if (target == null) return Optional.empty();
        Rect2i area = new Rect2i(target.x(), target.y(), target.size(), target.size());
        if (target.fluid() != null) {
            return ingredients.createClickableIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(target.fluid(), 1000), area, false)
                    .map(ingredient -> ingredient);
        }
        return ingredients.createClickableIngredient(target.item(), area, false).map(ingredient -> ingredient);
    }
}
