package com.exoticmoss.simpleores.block.entity;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "simpleores");

    // A single block entity for the 4 cores
    public static final Supplier<BlockEntityType<GravityCoreBlockEntity>> GRAVITY_CORE_BE =
            BLOCK_ENTITIES.register("gravity_core_be", () -> new BlockEntityType<>(
                    GravityCoreBlockEntity::new,
                    ModBlocks.GRAVITY_CORE_SUN.get(), ModBlocks.GRAVITY_CORE_RED_GIANT.get(),
                    ModBlocks.GRAVITY_CORE_PULSAR.get(), ModBlocks.GRAVITY_CORE_BLACK_HOLE.get()));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
