package com.exoticmoss.simpleores.starstorage.autocraft;

import com.exoticmoss.simpleores.starstorage.network.StarAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/// The crafts made on an altar with pedestals around it: one item in the middle (the first ingredient of the recipe)
/// and the others one on each pedestal, then the altar is started (like the player does it) and the result comes out
/// of the altar. Read without needing the mods to be installed (by name):
///  - Mystical Agriculture: the Infusion Altar and its Infusion Pedestals (started like with a redstone signal);
///  - Draconic Evolution: the Fusion Crafting Core and its Fusion Crafting Injectors (started like with its button).
/// In both, the slot 0 of the altar is the middle item and the slot 1 the result.
final class Altars {
    private Altars() {
    }

    /// An altar: its slots, the positions of its pedestals and how to start it.
    record Altar(IItemHandler center, List<BlockPos> pedestals, Runnable start) {
        /// Crafting, or the result not taken out yet.
        boolean busy() {
            return !center.getStackInSlot(0).isEmpty() || center.getSlots() > 1 && !center.getStackInSlot(1).isEmpty();
        }
    }

    /// The altar at this position, or null if it is not one.
    static @Nullable Altar of(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        String name = blockEntity.getClass().getName();
        try {
            if (name.equals("com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity")) {
                IItemHandler center = StarAccess.items(level, pos);
                if (center == null) return null;
                List<BlockPos> pedestals = new ArrayList<>();
                if (method(blockEntity, "getPedestalPositions").invoke(blockEntity) instanceof List<?> list) {
                    for (Object at : list) {
                        if (at instanceof BlockPos pedestal && isPedestal(level, pedestal, "InfusionPedestalTileEntity")) pedestals.add(pedestal);
                    }
                }
                Method activate = method(blockEntity, "activate");
                return new Altar(center, pedestals, () -> invoke(activate, blockEntity));
            }
            if (name.equals("com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore")) {
                IItemHandler center = StarAccess.items(level, pos);
                if (center == null) return null;
                method(blockEntity, "updateInjectors").invoke(blockEntity);
                List<BlockPos> pedestals = new ArrayList<>();
                if (method(blockEntity, "getInjectors").invoke(blockEntity) instanceof List<?> list) {
                    for (Object injector : list) {
                        if (injector instanceof BlockEntity at) pedestals.add(at.getBlockPos());
                    }
                }
                Method start = method(blockEntity, "startCraft");
                return new Altar(center, pedestals, () -> invoke(start, blockEntity));
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
        return null;
    }

    /// The slots of a pedestal (null = there is no pedestal there any more).
    static @Nullable IItemHandler pedestal(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    private static boolean isPedestal(Level level, BlockPos pos, String type) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity != null && blockEntity.getClass().getSimpleName().equals(type);
    }

    private static Method method(Object target, String name) throws NoSuchMethodException {
        return target.getClass().getMethod(name);
    }

    private static void invoke(Method method, Object target) {
        try {
            method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // (the altar didn't start: the job keeps waiting, and the items stay on it)
        }
    }
}
