package com.exoticmoss.simpleores.starstorage.client;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.starstorage.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Set;

/// A short line under the name of every star (what it does): "tooltip.simpleores.<id>" in the lang files.
@EventBusSubscriber(modid = SimpleOres.MODID, value = Dist.CLIENT)
public class StarTooltips {
    private static final Set<String> STARS = Set.of("magnetar_core", "stellar_terminal", "advanced_stellar_terminal",
            "binary_star", "quasar", "stellar_drive", "gravity_core_sun", "gravity_core_red_giant", "gravity_core_pulsar",
            "gravity_core_black_hole", "solar_wind_upgrade", "magnetic_field_upgrade", "wormhole_link");

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals(SimpleOres.MODID) || !STARS.contains(id.getPath())) return;
        event.getToolTip().add(1, Component.translatable("tooltip.simpleores." + id.getPath()).withStyle(ChatFormatting.GRAY));
        // Wormhole Link: the Magnetar it points to
        GlobalPos link = event.getItemStack().get(ModDataComponents.LINK.get());
        if (link != null) {
            event.getToolTip().add(2, Component.translatable("tooltip.simpleores.wormhole_link.target", link.pos().getX(), link.pos().getY(),
                    link.pos().getZ(), link.dimension().identifier().toString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
