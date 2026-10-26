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
import com.exoticmoss.simpleores.starstorage.CellContents;
import com.exoticmoss.simpleores.starstorage.FluidCellItem;
import com.exoticmoss.simpleores.starstorage.FluidContents;
import com.exoticmoss.simpleores.starstorage.StarCellItem;
import com.exoticmoss.simpleores.starstorage.StellarDriveBlockEntity;
import mcp.mobius.waila.api.data.EnergyData;
import mcp.mobius.waila.api.data.FluidData;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.HashMap;
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
    }

    /// Stellar Drive: the items of its Star Cells (a bar with how full they are, and the items) and the liquids of its
    /// Fluid Cells (one bar per liquid). The bar of the items is named in SimpleOresWthitClientPlugin.
    public enum DriveDataProvider implements IDataProvider<StellarDriveBlockEntity> {
        INSTANCE;

        /// At most this many kinds of items are shown (the most first).
        private static final int MAX_ITEMS = 36;

        @Override
        public void appendData(IDataWriter data, IServerAccessor<StellarDriveBlockEntity> accessor, IPluginConfig config) {
            long used = 0, capacity = 0, fluidCapacity = 0;
            Map<ItemStackTemplate, Long> items = new HashMap<>();
            Map<Fluid, Long> fluids = new LinkedHashMap<>();
            for (ItemStack cell : accessor.getTarget().getCells()) {
                if (cell.getItem() instanceof StarCellItem item) {
                    capacity += item.tier.capacity;
                    for (CellContents.Entry entry : StarCellItem.getContents(cell).entries()) {
                        used += entry.count();
                        items.merge(entry.item(), (long) entry.count(), Long::sum);
                    }
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
                FluidData fluidData = FluidData.of(FluidData.Unit.MILLIBUCKETS, fluids.size());
                fluids.forEach((fluid, amount) -> fluidData.add(fluid, DataComponentPatch.EMPTY, amount, liquidsCapacity));
                result.add(fluidData);
            });
            // The items (the most first)
            List<Map.Entry<ItemStackTemplate, Long>> sorted = new ArrayList<>(items.entrySet());
            sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
            List<ItemStack> stacks = new ArrayList<>();
            for (int i = 0; i < Math.min(MAX_ITEMS, sorted.size()); i++) {
                ItemStack stack = sorted.get(i).getKey().create();
                stack.setCount((int) Math.min(Integer.MAX_VALUE, sorted.get(i).getValue()));
                stacks.add(stack);
            }
            data.add(ItemData.TYPE, result -> {
                if (!stacks.isEmpty()) result.add(ItemData.of(config).add(stacks));
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
