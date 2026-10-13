package com.exoticmoss.simpleores.item.custom;

import com.exoticmoss.simpleores.SimpleOres;
import com.exoticmoss.simpleores.item.ModItems;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/// Wither Reaper: a Wither Skeleton killed by a player holding it has a 33% chance to drop its skull.
@EventBusSubscriber(modid = SimpleOres.MODID)
public class WitherReaperEvents {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.is(EntityTypes.WITHER_SKELETON)) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (!player.getMainHandItem().is(ModItems.WITHER_REAPER.get())) return;

        // The normal (vanilla) drop already gave a skull: don't give a second one
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(Items.WITHER_SKELETON_SKULL)) return;
        }

        if (entity.getRandom().nextFloat() < WitherReaperItem.SKULL_CHANCE) {
            ItemEntity skull = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(),
                    new ItemStack(Items.WITHER_SKELETON_SKULL));
            skull.setDefaultPickUpDelay();
            event.getDrops().add(skull);
        }
    }
}
