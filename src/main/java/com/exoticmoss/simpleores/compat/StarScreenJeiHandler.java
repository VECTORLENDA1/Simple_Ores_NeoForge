package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.client.JeiTarget;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

/// JEI on the screens of the star storage (Stellar Terminal, Quasar, Binary Star): the items they draw themselves
/// (not in slots) are given to JEI, so R (recipes), U (uses) and the other keys of JEI work on them.
public class StarScreenJeiHandler<T extends AbstractContainerScreen<?> & JeiTarget.Source> implements IGuiContainerHandler<T> {
    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder, T screen,
                                                                                        double mouseX, double mouseY) {
        JeiTarget target = screen.jeiTargetAt(mouseX, mouseY);
        if (target == null) return Optional.empty();
        if (target.fluid() != null) {
            return builder.createBuilder(NeoForgeTypes.FLUID_STACK, new FluidStack(target.fluid(), 1000))
                    .buildWithArea(target.x(), target.y(), target.size(), target.size());
        }
        return builder.createBuilder(target.item()).buildWithArea(target.x(), target.y(), target.size(), target.size());
    }
}
