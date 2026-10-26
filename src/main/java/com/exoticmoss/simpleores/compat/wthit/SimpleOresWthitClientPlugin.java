package com.exoticmoss.simpleores.compat.wthit;

import com.exoticmoss.simpleores.SimpleOres;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.data.EnergyData;
import net.minecraft.network.chat.Component;

/// WTHIT, client side: the bar of the Stellar Drive (sent as "energy" data) is named "Items", in purple.
public class SimpleOresWthitClientPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        EnergyData.describe(SimpleOres.MODID)
                .name(Component.translatable("screen.simpleores.magnetar.items"))
                .unit("")
                .color(0x8060C0);
    }
}
