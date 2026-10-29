package com.exoticmoss.simpleores.starstorage.autocraft;

import com.exoticmoss.simpleores.starstorage.network.StarAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/// The crafts made on an altar with pedestals around it: one item in the middle (the first ingredient of the recipe)
/// and the others one on each pedestal. The pedestals are filled first, then the middle, then the altar is started
/// (like the player does it) and the result comes out of the altar. Read without needing the mods to be installed
/// (by name):
///  - Mystical Agriculture: the Infusion Altar and its Infusion Pedestals (started like with a redstone signal);
///  - Draconic Evolution: the Fusion Crafting Core and its Fusion Crafting Injectors (started like with its button);
///  - Ars Nouveau: the Enchanting Apparatus and its Arcane Pedestals (it starts by itself when the middle item goes in);
///  - Actually Additions: the Empowerer and its 4 Display Stands (it starts by itself).
/// The slot 0 of the altar is the middle item; the result is in the slot 1 (Mystical Agriculture, Draconic
/// Evolution) or takes the place of the middle item (Ars Nouveau, Actually Additions).
final class Altars {
    private Altars() {
    }

    /// An altar: its slots, the positions of its pedestals, how to start it and if it is crafting.
    record Altar(IItemHandler center, List<BlockPos> pedestals, Runnable start, BooleanSupplier crafting) {
        /// Crafting, or the result not taken out yet.
        boolean busy() {
            return crafting.getAsBoolean() || !center.getStackInSlot(0).isEmpty()
                    || center.getSlots() > 1 && !center.getStackInSlot(1).isEmpty();
        }
    }

    /// The altar at this position, or null if it is not one.
    static @Nullable Altar of(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        String name = blockEntity.getClass().getName();
        try {
            switch (name) {
                case "com.blakebr0.mysticalagriculture.tileentity.InfusionAltarTileEntity" -> {
                    IItemHandler center = StarAccess.items(level, pos);
                    if (center == null) return null;
                    List<BlockPos> pedestals = new ArrayList<>();
                    if (method(blockEntity, "getPedestalPositions").invoke(blockEntity) instanceof List<?> list) {
                        for (Object at : list) {
                            if (at instanceof BlockPos pedestal && isType(level, pedestal, "InfusionPedestalTileEntity")) pedestals.add(pedestal);
                        }
                    }
                    Method activate = method(blockEntity, "activate");
                    return new Altar(center, pedestals, () -> invoke(activate, blockEntity), () -> false);
                }
                case "com.brandon3055.draconicevolution.blocks.tileentity.TileFusionCraftingCore" -> {
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
                    Method crafting = method(blockEntity, "isCrafting");
                    return new Altar(center, pedestals, () -> invoke(start, blockEntity), () -> isTrue(crafting, blockEntity));
                }
                case "com.hollingsworth.arsnouveau.common.block.tile.EnchantingApparatusTile" -> {
                    IItemHandler center = StarAccess.items(level, pos);
                    if (center == null) return null;
                    List<BlockPos> pedestals = new ArrayList<>();
                    if (method(blockEntity, "pedestalList").invoke(blockEntity) instanceof List<?> list) {
                        for (Object at : list) {
                            if (at instanceof BlockPos pedestal && isType(level, pedestal, "ArcanePedestalTile")) pedestals.add(pedestal);
                        }
                    }
                    Field crafting = blockEntity.getClass().getField("isCrafting");
                    return new Altar(center, pedestals, () -> {
                    }, () -> isTrue(crafting, blockEntity));
                }
                case "de.ellpeck.actuallyadditions.mod.tile.TileEntityEmpowerer" -> {
                    IItemHandler center = StarAccess.items(level, pos);
                    if (center == null) return null;
                    List<BlockPos> pedestals = new ArrayList<>();
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        BlockPos stand = pos.relative(side, 3);
                        if (isType(level, stand, "TileEntityDisplayStand")) pedestals.add(stand);
                    }
                    // (it only works with its 4 stands)
                    if (pedestals.size() < 4) return null;
                    return new Altar(center, pedestals, () -> {
                    }, () -> false);
                }
                default -> {
                    return null;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    /// The slots of a pedestal (null = there is no pedestal there any more).
    static @Nullable IItemHandler pedestal(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
    }

    private static boolean isType(Level level, BlockPos pos, String type) {
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

    private static boolean isTrue(Method method, Object target) {
        try {
            return method.invoke(target) instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean isTrue(Field field, Object target) {
        try {
            return field.get(target) instanceof Boolean value && value;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
