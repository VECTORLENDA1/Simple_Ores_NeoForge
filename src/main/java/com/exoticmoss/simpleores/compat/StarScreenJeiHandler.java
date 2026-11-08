package com.exoticmoss.simpleores.compat;

import com.exoticmoss.simpleores.starstorage.client.JeiTarget;
import com.exoticmoss.simpleores.starstorage.gas.Gases;
import com.exoticmoss.simpleores.starstorage.network.Virtual;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
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
        if (target == null) {
            // A slot with a liquid or a gas (as its item, see Virtual): JEI sees the liquid or the gas
            net.minecraft.world.inventory.Slot slot = screen.getSlotUnderMouse();
            if (slot == null || !Virtual.isVirtual(slot.getItem())) return Optional.empty();
            Rect2i area = new Rect2i(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, 16, 16);
            return typed(slot.getItem()).flatMap(typed -> clickable(typed, area));
        }
        Rect2i area = new Rect2i(target.x(), target.y(), target.size(), target.size());
        if (target.fluid() == null && Virtual.isVirtual(target.item())) {
            return typed(target.item()).flatMap(typed -> clickable(typed, area));
        }
        if (target.fluid() != null) {
            return ingredients.createClickableIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(target.fluid(), 1000), area, false)
                    .map(ingredient -> ingredient);
        }
        return ingredients.createClickableIngredient(target.item(), area, false).map(ingredient -> ingredient);
    }

    /// What JEI knows of an item: a liquid or a gas shown as an item (Virtual) is that liquid or gas
    /// (the gas as the ingredient of Mekanism); any other item is itself.
    public static Optional<ITypedIngredient<?>> typed(ItemStack stack) {
        if (ingredients == null || stack.isEmpty()) return Optional.empty();
        Fluid fluid = Virtual.fluidOf(stack);
        if (fluid != null) return ingredients.createTypedIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid, 1000)).map(typed -> typed);
        ResourceLocation gas = Virtual.gasOf(stack);
        if (gas != null) {
            Object chemical = Gases.stack(gas, 1000);
            return chemical == null ? Optional.empty() : typedOf(chemical);
        }
        return ingredients.createTypedIngredient(VanillaTypes.ITEM_STACK, stack.copyWithCount(1)).map(typed -> typed);
    }

    private static <V> Optional<ITypedIngredient<?>> typedOf(V ingredient) {
        Optional<ITypedIngredient<?>> typed = ingredients.getIngredientTypeChecked(ingredient)
                .flatMap(type -> ingredients.createTypedIngredient(type, ingredient)).map(found -> found);
        if (typed.isPresent()) return typed;
        // (the type of the gases of Mekanism, found by the class of its ingredients)
        for (mezz.jei.api.ingredients.IIngredientType<?> type : ingredients.getRegisteredIngredientTypes()) {
            if (type.getIngredientClass().isInstance(ingredient)) {
                Optional<ITypedIngredient<?>> found = typedAs(type, ingredient);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private static <T> Optional<ITypedIngredient<?>> typedAs(mezz.jei.api.ingredients.IIngredientType<T> type, Object ingredient) {
        return ingredients.createTypedIngredient(type, type.getIngredientClass().cast(ingredient)).map(found -> found);
    }

    private static <V> Optional<IClickableIngredient<?>> clickable(ITypedIngredient<V> typed, Rect2i area) {
        return ingredients.createClickableIngredient(typed.getType(), typed.getIngredient(), area, false).map(ingredient -> ingredient);
    }
}
