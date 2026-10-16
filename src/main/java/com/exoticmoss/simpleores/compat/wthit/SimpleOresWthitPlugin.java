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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/// WTHIT support (optional, only loaded when WTHIT is installed): when looking at a Gravity Core,
/// the tooltip shows the items orbiting it and, while it is crafting, a progress bar with
/// the ingredients -> the result. Registered in wthit_plugins.json.
public class SimpleOresWthitPlugin implements IWailaCommonPlugin {

    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(GravityCoreDataProvider.INSTANCE, GravityCoreBlockEntity.class);
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
