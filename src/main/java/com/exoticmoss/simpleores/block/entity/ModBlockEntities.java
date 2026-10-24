package com.exoticmoss.simpleores.block.entity;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class ModBlockEntities {
    private static final BlockEntityType<GravityCoreBlockEntity> GRAVITY_CORE_TYPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath("simpleores", "gravity_core_be"),
            FabricBlockEntityTypeBuilder.create(GravityCoreBlockEntity::new,
                    ModBlocks.GRAVITY_CORE_SUN.get(), ModBlocks.GRAVITY_CORE_RED_GIANT.get(),
                    ModBlocks.GRAVITY_CORE_PULSAR.get(), ModBlocks.GRAVITY_CORE_BLACK_HOLE.get()).build());

    // A single block entity for the 4 cores
    public static final Supplier<BlockEntityType<GravityCoreBlockEntity>> GRAVITY_CORE_BE = () -> GRAVITY_CORE_TYPE;


    /// Loads this class, which registers the block entity
    public static void register() {
    }
}
