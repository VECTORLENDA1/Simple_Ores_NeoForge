package com.exoticmoss.simpleores.compat.wthit;

import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlockEntity;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import mcp.mobius.waila.api.data.ItemData;
import mcp.mobius.waila.api.data.ProgressData;
import com.exoticmoss.simpleores.starstorage.FluidCellItem;
import com.exoticmoss.simpleores.starstorage.FluidContents;
import com.exoticmoss.simpleores.starstorage.StarCellItem;
import com.exoticmoss.simpleores.starstorage.StellarDriveBlockEntity;
import mcp.mobius.waila.api.data.EnergyData;
import mcp.mobius.waila.api.data.FluidData;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// WTHIT support (optional, only loaded when WTHIT is installed): when looking at a Gravity Core,
/// the tooltip shows the items orbiting it and, while it is crafting, a progress bar with
/// the ingredients -> the result. When looking at a Stellar Drive: how full its cells are, its items and liquids.
/// Registered in wthit_plugins.json.
public class SimpleOresWthitPlugin implements IWailaCommonPlugin {

    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(GravityCoreDataProvider.INSTANCE, GravityCoreBlockEntity.class);
        registrar.blockData(DriveDataProvider.INSTANCE, StellarDriveBlockEntity.class);
        // The other blocks of the star storage see the whole network: WTHIT would show all its items
        registrar.blockData(HideContentsProvider.INSTANCE, com.exoticmoss.simpleores.starstorage.StellarTerminalBlockEntity.class, 0);
        registrar.blockData(HideContentsProvider.INSTANCE, com.exoticmoss.simpleores.starstorage.AdvancedTerminalBlockEntity.class, 0);
        registrar.blockData(HideContentsProvider.INSTANCE, com.exoticmoss.simpleores.starstorage.QuasarBlockEntity.class, 0);
        registrar.blockData(HideContentsProvider.INSTANCE, com.exoticmoss.simpleores.starstorage.BinaryStarBlockEntity.class, 0);
        registrar.blockData(HideContentsProvider.INSTANCE, com.exoticmoss.simpleores.starstorage.MagnetarBlockEntity.class, 0);
        // The drive: only its bars (DriveDataProvider), not the items
        registrar.blockData(HideItemsProvider.INSTANCE, StellarDriveBlockEntity.class, 0);
    }

    /// Hides the items that WTHIT shows by itself.
    public enum HideItemsProvider implements IDataProvider<net.minecraft.world.level.block.entity.BlockEntity> {
        INSTANCE;

        @Override
        public void appendData(IDataWriter data, IServerAccessor<net.minecraft.world.level.block.entity.BlockEntity> accessor, IPluginConfig config) {
            data.blockAll(ItemData.TYPE);
        }
    }

    /// Hides the items and liquids (WTHIT shows them by itself for blocks that let other mods take items out).
    public enum HideContentsProvider implements IDataProvider<net.minecraft.world.level.block.entity.BlockEntity> {
        INSTANCE;

        @Override
        public void appendData(IDataWriter data, IServerAccessor<net.minecraft.world.level.block.entity.BlockEntity> accessor, IPluginConfig config) {
            data.blockAll(ItemData.TYPE);
            data.blockAll(FluidData.TYPE);
        }
    }

    /// Stellar Drive: a bar with how full its Star Cells are (not the items: they can be thousands of kinds) and the
    /// liquids of its Fluid Cells (one bar per liquid, the MAX_FLUIDS with the most). The bar of the items is named in
    /// SimpleOresWthitClientPlugin.
    public enum DriveDataProvider implements IDataProvider<StellarDriveBlockEntity> {
        INSTANCE;

        /// At most this many liquids are shown (the most first).
        private static final int MAX_FLUIDS = 3;

        @Override
        public void appendData(IDataWriter data, IServerAccessor<StellarDriveBlockEntity> accessor, IPluginConfig config) {
            long used = 0, capacity = 0, fluidCapacity = 0;
            Map<Fluid, Long> fluids = new LinkedHashMap<>();
            for (ItemStack cell : accessor.getTarget().getCells()) {
                if (cell.getItem() instanceof StarCellItem item) {
                    capacity += item.tier.capacity;
                    used += StarCellItem.getContents(cell).totalCount();
                } else if (cell.getItem() instanceof FluidCellItem item) {
                    fluidCapacity = Math.min(Long.MAX_VALUE / 2, fluidCapacity + item.tier.fluidCapacity);
                    for (FluidContents.Entry entry : FluidCellItem.getContents(cell).entries()) fluids.merge(entry.fluid(), entry.amount(), Long::sum);
                }
            }
            long itemsUsed = used, itemsCapacity = capacity, liquidsCapacity = fluidCapacity;
            // How full the Star Cells are
            data.add(EnergyData.TYPE, result -> {
                if (itemsCapacity > 0) result.add(EnergyData.of(itemsUsed, itemsCapacity));
            });
            // The liquids
            data.add(FluidData.TYPE, result -> {
                if (fluids.isEmpty()) return;
                List<Map.Entry<Fluid, Long>> sorted = new ArrayList<>(fluids.entrySet());
                sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
                FluidData fluidData = FluidData.of(FluidData.Unit.MILLIBUCKETS, Math.min(MAX_FLUIDS, sorted.size()));
                for (int i = 0; i < Math.min(MAX_FLUIDS, sorted.size()); i++) {
                    fluidData.add(sorted.get(i).getKey(), DataComponentPatch.EMPTY, sorted.get(i).getValue(), liquidsCapacity);
                }
                result.add(fluidData);
            });
        }
    }

    /// Sends the items of the core (and the craft progress) from the server to the tooltip.
    public enum GravityCoreDataProvider implements IDataProvider<GravityCoreBlockEntity> {
        INSTANCE;

        @Override
        public void appendData(IDataWriter data, IServerAccessor<GravityCoreBlockEntity> accessor, IPluginConfig config) {
            GravityCoreBlockEntity core = accessor.getTarget();
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack stack : core.getItems()) {
                if (!stack.isEmpty()) items.add(stack.copy());
            }

            // The items orbiting the core
            data.add(ItemData.TYPE, result -> {
                if (!items.isEmpty()) result.add(ItemData.of(config).add(items));
            });

            // While crafting: progress bar, ingredients -> result
            data.add(ProgressData.TYPE, result -> {
                if (core.getCraftTotal() > 0 && core.getCraftTime() > 0) {
                    ProgressData progress = ProgressData.tick(core.getCraftTime(), core.getCraftTotal()).input(items);
                    ItemStack output = core.getCraftingResult();
                    if (!output.isEmpty()) progress.output(output);
                    result.add(progress);
                }
            });
        }
    }
}
