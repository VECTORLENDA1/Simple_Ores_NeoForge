package com.exoticmoss.simpleores.block.entity;

import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlockEntity;
import com.exoticmoss.simpleores.starstorage.BinaryStarBlockEntity;
import com.exoticmoss.simpleores.starstorage.MagnetarBlockEntity;
import com.exoticmoss.simpleores.starstorage.QuasarBlockEntity;
import com.exoticmoss.simpleores.starstorage.StellarDriveBlockEntity;
import com.exoticmoss.simpleores.starstorage.StellarTerminalBlockEntity;
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

    public static final Supplier<BlockEntityType<MagnetarBlockEntity>> MAGNETAR_BE =
            BLOCK_ENTITIES.register("magnetar_be", () -> new BlockEntityType<>(MagnetarBlockEntity::new, ModBlocks.MAGNETAR_CORE.get()));

    public static final Supplier<BlockEntityType<StellarDriveBlockEntity>> STELLAR_DRIVE_BE =
            BLOCK_ENTITIES.register("stellar_drive_be", () -> new BlockEntityType<>(StellarDriveBlockEntity::new, ModBlocks.STELLAR_DRIVE.get()));

    public static final Supplier<BlockEntityType<StellarTerminalBlockEntity>> STELLAR_TERMINAL_BE =
            BLOCK_ENTITIES.register("stellar_terminal_be", () -> new BlockEntityType<>(StellarTerminalBlockEntity::new, ModBlocks.STELLAR_TERMINAL.get()));

    public static final Supplier<BlockEntityType<BinaryStarBlockEntity>> BINARY_STAR_BE =
            BLOCK_ENTITIES.register("binary_star_be", () -> new BlockEntityType<>(BinaryStarBlockEntity::new, ModBlocks.BINARY_STAR.get()));

    public static final Supplier<BlockEntityType<QuasarBlockEntity>> QUASAR_BE =
            BLOCK_ENTITIES.register("quasar_be", () -> new BlockEntityType<>(QuasarBlockEntity::new, ModBlocks.QUASAR.get()));


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
