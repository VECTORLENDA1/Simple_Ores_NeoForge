package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.client.JeiTarget;
import com.exoticmoss.simpleores.starstorage.network.Virtual;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

/// JEI on the screens of the star storage (Stellar Terminal, Quasar, Binary Star): the items they draw themselves
/// (not in slots) are given to JEI, so R (recipes), U (uses) and the other keys of JEI work on them.
/// A liquid shown as an item (see Virtual) is given to JEI as that liquid.
public class StarScreenJeiHandler implements IGuiContainerHandler<AbstractContainerScreen<?>> {
    /// The class of a screen, for the registration (the screens of the star storage are generic).
    @SuppressWarnings("unchecked")
    public static Class<? extends AbstractContainerScreen<?>> screen(Class<?> screen) {
        return (Class<? extends AbstractContainerScreen<?>>) screen;
    }

    /// Set by the plugin when JEI is ready.
    static IIngredientManager ingredients;

    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory builder,
                                                                                        AbstractContainerScreen<?> screen,
                                                                                        double mouseX, double mouseY) {
        if (!(screen instanceof JeiTarget.Source source)) return Optional.empty();
        JeiTarget target = source.jeiTargetAt(mouseX, mouseY);
        if (target == null) {
            // A slot with a liquid (as its item, see Virtual): JEI sees the liquid
            Slot slot = screen.getSlotUnderMouse();
            if (slot == null) return Optional.empty();
            Fluid fluid = Virtual.fluidOf(slot.getItem());
            if (fluid == null) return Optional.empty();
            return builder.createBuilder(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid, 1000))
                    .buildWithArea(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, 16, 16);
        }
        Fluid fluid = target.fluid() != null ? target.fluid() : Virtual.fluidOf(target.item());
        if (fluid != null) {
            return builder.createBuilder(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid, 1000))
                    .buildWithArea(target.x(), target.y(), target.size(), target.size());
        }
        return builder.createBuilder(target.item()).buildWithArea(target.x(), target.y(), target.size(), target.size());
    }

    /// What JEI knows of an item: a liquid shown as an item (Virtual) is that liquid; any other item is itself.
    public static Optional<ITypedIngredient<?>> typed(ItemStack stack) {
        if (ingredients == null || stack.isEmpty()) return Optional.empty();
        Fluid fluid = Virtual.fluidOf(stack);
        if (fluid != null) return ingredients.createTypedIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid, 1000)).map(typed -> typed);
        return ingredients.createTypedIngredient(VanillaTypes.ITEM_STACK, stack.copyWithCount(1)).map(typed -> typed);
    }
}
