package com.vector.simpleores.block.entity;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.block.entity.custom.AtomicCraftingTableEntity;
import com.vector.simpleores.block.entity.custom.SimpleCraftingTableEntity;
import com.vector.simpleores.block.entity.custom.UltraCraftingTableEntity;
import com.vector.simpleores.gravitycraft.GravityCoreBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "simpleores");

public static final Supplier<BlockEntityType<SimpleCraftingTableEntity>> SIMPLE_CRAFTING_TABLE_BE =
        BLOCK_ENTITIES.register("simple_crafting_table_be", () -> new BlockEntityType<>(
                SimpleCraftingTableEntity::new, ModBlocks.SIMPLE_CRAFTING_TABLE.get()));

    public static final Supplier<BlockEntityType<UltraCraftingTableEntity>> ULTRA_CRAFTING_TABLE_BE =
            BLOCK_ENTITIES.register("ultra_crafting_table_be", () -> new BlockEntityType<>(
                    UltraCraftingTableEntity::new, ModBlocks.ULTRA_CRAFTING_TABLE.get()));

    public static final Supplier<BlockEntityType<AtomicCraftingTableEntity>> ATOMIC_CRAFTING_TABLE_BE =
            BLOCK_ENTITIES.register("atomic_crafting_table_be", () -> new BlockEntityType<>(
                    AtomicCraftingTableEntity::new, ModBlocks.ATOMIC_CRAFTING_TABLE.get()));

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
