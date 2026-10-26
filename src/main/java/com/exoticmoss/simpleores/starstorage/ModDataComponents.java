package com.exoticmoss.simpleores.starstorage;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.exoticmoss.simpleores.SimpleOres.MODID;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    /// The items kept inside a Star Cell
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CellContents>> CELL_CONTENTS =
            DATA_COMPONENTS.registerComponentType("cell_contents", builder -> builder
                    .persistent(CellContents.CODEC)
                    .networkSynchronized(CellContents.STREAM_CODEC)
                    .cacheEncoding());

    /// The Magnetar a Wormhole Link points to (its dimension and position)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINK =
            DATA_COMPONENTS.registerComponentType("link", builder -> builder
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
