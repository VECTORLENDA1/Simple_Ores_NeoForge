package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreBlock;
import com.exoticmoss.simpleores.starstorage.binary.BinaryStarBlock;
import com.exoticmoss.simpleores.starstorage.magnetar.MagnetarBlock;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarBlock;
import com.exoticmoss.simpleores.starstorage.terminal.StellarTerminalBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

/// The stars (Magnetar Core, Stellar Terminal and the Gravity Cores) don't show the black box outline when
/// the player looks at them: the star itself shines instead (see StarBlockRenderer and GravityCoreRenderer).
@EventBusSubscriber(modid = SimpleOres.MODID, value = Dist.CLIENT)
public class StarOutline {
    @SubscribeEvent
    public static void onBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        var block = event.getBlockState().getBlock();
        if (block instanceof MagnetarBlock || block instanceof StellarTerminalBlock || block instanceof GravityCoreBlock
                || block instanceof BinaryStarBlock || block instanceof QuasarBlock) {
            event.setCanceled(true);
        }
    }
}
