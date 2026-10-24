package com.exoticmoss.simpleores.item.custom;

import com.exoticmoss.simpleores.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/// Wither Reaper: a Wither Skeleton killed by a player holding it has a 33% chance to drop its skull.
/// On Fabric this runs when the death loot table of the Wither Skeleton is rolled (LootTableEvents.MODIFY_DROPS),
/// instead of NeoForge's LivingDropsEvent.
public class WitherReaperEvents {
    public static void register() {
        LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
            Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
            if (entity == null || entity.getType() != EntityTypes.WITHER_SKELETON) return;
            // Only the death loot of the Wither Skeleton (not other tables rolled for it)
            if (table.unwrapKey().isEmpty() || !table.unwrapKey().equals(entity.getLootTable())) return;
            if (!(context.getOptionalParameter(LootContextParams.ATTACKING_ENTITY) instanceof Player player)) return;
            if (!player.getMainHandItem().is(ModItems.WITHER_REAPER.get())) return;

            // The normal (vanilla) drop already gave a skull: don't give a second one
            for (ItemStack drop : drops) {
                if (drop.is(Items.WITHER_SKELETON_SKULL)) return;
            }

            if (entity.getRandom().nextFloat() < WitherReaperItem.SKULL_CHANCE) {
                drops.add(new ItemStack(Items.WITHER_SKELETON_SKULL));
            }
        });
    }
}
