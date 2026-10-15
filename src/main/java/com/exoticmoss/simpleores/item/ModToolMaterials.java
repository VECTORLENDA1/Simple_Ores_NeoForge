package com.exoticmoss.simpleores.item;

import com.exoticmoss.simpleores.SimpleOres;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/// Materials of the Simple Ores weapons/tools. Change the numbers here to change the weapon.
public class ModToolMaterials {
    /// Items that repair the Wither Reaper in an anvil (data/simpleores/tags/item/wither_reaper_repair.json)
    public static final TagKey<Item> WITHER_REAPER_REPAIR =
            ItemTags.create(Identifier.fromNamespaceAndPath(SimpleOres.MODID, "wither_reaper_repair"));

    public static final ToolMaterial WITHER_REAPER = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // blocks this weapon can't mine/drop
            1561,                                 // durability (uses)
            8.0F,                                 // mining speed
            3.0F,                                 // extra attack damage (added to the damage in ModItems)
            10,                                   // enchantability
            WITHER_REAPER_REPAIR);                // repair items
}
