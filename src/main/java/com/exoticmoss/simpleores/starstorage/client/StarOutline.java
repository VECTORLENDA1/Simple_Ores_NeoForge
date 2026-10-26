package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.MagnetarBlock;
import com.exoticmoss.simpleores.starstorage.StellarTerminalBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/// The stars of the star storage (Magnetar Core, Stellar Terminal) don't show the black box outline when
/// the player looks at them: the star itself shines instead (see StarBlockRenderer).
@EventBusSubscriber(modid = SimpleOres.MODID, value = Dist.CLIENT)
public class StarOutline {
    @SubscribeEvent
    public static void onBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        if (event.getBlockState().getBlock() instanceof MagnetarBlock || event.getBlockState().getBlock() instanceof StellarTerminalBlock) {
            event.setCanceled(true);
        }
    }
}
