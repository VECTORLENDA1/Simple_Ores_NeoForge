package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.exoticmoss.simpleores.SimpleOres.MODID;
import com.exoticmoss.simpleores.starstorage.cell.CellContents;
import com.exoticmoss.simpleores.starstorage.cell.FluidContents;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    /// The items kept inside a Star Cell
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CellContents>> CELL_CONTENTS =
            DATA_COMPONENTS.registerComponentType("cell_contents", builder -> builder
                    .persistent(CellContents.CODEC)
                    .networkSynchronized(CellContents.STREAM_CODEC)
                    .cacheEncoding());

    /// The liquids kept inside a Fluid Cell
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FluidContents>> FLUID_CONTENTS =
            DATA_COMPONENTS.registerComponentType("fluid_contents", builder -> builder
                    .persistent(FluidContents.CODEC)
                    .networkSynchronized(FluidContents.STREAM_CODEC)
                    .cacheEncoding());

    /// The Magnetar a Wormhole Link points to (its dimension and position)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINK =
            DATA_COMPONENTS.registerComponentType("link", builder -> builder
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC));

    /// The recipes of a broken Quasar (kept by the world, see QuasarRecipes): the Quasar placed with the item gets them back
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.exoticmoss.simpleores.starstorage.quasar.QuasarRecipes.Id>> QUASAR_RECIPES =
            DATA_COMPONENTS.registerComponentType("quasar_recipes", builder -> builder
                    .persistent(com.exoticmoss.simpleores.starstorage.quasar.QuasarRecipes.Id.CODEC)
                    .networkSynchronized(com.exoticmoss.simpleores.starstorage.quasar.QuasarRecipes.Id.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
