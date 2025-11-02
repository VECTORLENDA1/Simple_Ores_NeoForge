package com.vector.simpleores.block.entity;

import com.vector.simpleores.block.ModBlocks;
import com.vector.simpleores.block.entity.custom.SimpleCraftingTableEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "weaponseffect");

public static final Supplier<BlockEntityType<SimpleCraftingTableEntity>> SIMPLE_CRAFTING_TABLE_BE =
        BLOCK_ENTITIES.register("simple_crafting_table_be", () -> BlockEntityType.Builder.of(
                SimpleCraftingTableEntity::new, ModBlocks.SIMPLE_CRAFTING_TABLE.get()).build(null));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
