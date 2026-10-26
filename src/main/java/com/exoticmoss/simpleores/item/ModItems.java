package com.exoticmoss.simpleores.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.exoticmoss.simpleores.item.custom.*;
import com.exoticmoss.simpleores.starstorage.CellTier;
import com.exoticmoss.simpleores.starstorage.StarCellItem;
import com.exoticmoss.simpleores.starstorage.FluidCellItem;

import static com.exoticmoss.simpleores.SimpleOres.MODID;


public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    //Items//
    public static final DeferredItem<Item> CELESTINE = ITEMS.registerSimpleItem("celestine");
    public static final DeferredItem<Item> ZENITHRA = ITEMS.registerSimpleItem("zenithra");
    public static final DeferredItem<Item> ASTRALITE = ITEMS.registerSimpleItem("astralite");
    public static final DeferredItem<Item> RAW_ASTRALITE = ITEMS.registerSimpleItem("raw_astralite");
    public static final DeferredItem<Item> RAW_NEXALITE = ITEMS.registerSimpleItem("raw_nexalite");
    public static final DeferredItem<Item> NEXALITE = ITEMS.registerSimpleItem("nexalite");
    public static final DeferredItem<Item> IGNITHRA = ITEMS.registerSimpleItem("ignithra");
    public static final DeferredItem<Item> RAW_IGNITHRA = ITEMS.registerSimpleItem("raw_ignithra");
    public static final DeferredItem<Item> ANTRACITE = ITEMS.registerItem("antracite", p -> new FuelItem(p, 3200));
    public static final DeferredItem<Item> RAW_OBSCURIDIUM = ITEMS.registerSimpleItem("raw_obscuridium");
    public static final DeferredItem<Item> OBSCURIDIUM = ITEMS.registerSimpleItem("obscuridium");
    public static final DeferredItem<Item> OBSCURITE = ITEMS.registerSimpleItem("obscurite");

    //Weapons//
    public static final DeferredItem<Item> WITHER_REAPER = ITEMS.registerItem("wither_reaper", WitherReaperItem::new,
            p -> p.sword(ModToolMaterials.WITHER_REAPER, 0, -2.0F)); /// base damage = 1, base attack speed = -4.0


    //Star Cells (they keep the items of the Magnetar network)//
    public static final DeferredItem<Item> WHITE_DWARF_CELL = ITEMS.registerItem("white_dwarf_cell",
            p -> new StarCellItem(CellTier.WHITE_DWARF, p), p -> p.stacksTo(1));
    public static final DeferredItem<Item> NEUTRON_CELL = ITEMS.registerItem("neutron_cell",
            p -> new StarCellItem(CellTier.NEUTRON, p), p -> p.stacksTo(1));
    public static final DeferredItem<Item> SINGULARITY_CELL = ITEMS.registerItem("singularity_cell",
            p -> new StarCellItem(CellTier.SINGULARITY, p), p -> p.stacksTo(1).fireResistant());

    //Fluid Cells (they keep the liquids of the Magnetar network)//
    public static final DeferredItem<Item> WHITE_DWARF_FLUID_CELL = ITEMS.registerItem("white_dwarf_fluid_cell",
            p -> new FluidCellItem(CellTier.WHITE_DWARF, p), p -> p.stacksTo(1));
    public static final DeferredItem<Item> NEUTRON_FLUID_CELL = ITEMS.registerItem("neutron_fluid_cell",
            p -> new FluidCellItem(CellTier.NEUTRON, p), p -> p.stacksTo(1));
    public static final DeferredItem<Item> SINGULARITY_FLUID_CELL = ITEMS.registerItem("singularity_fluid_cell",
            p -> new FluidCellItem(CellTier.SINGULARITY, p), p -> p.stacksTo(1).fireResistant());

    //Magnetar upgrades (they go in the screen of the Magnetar Core)//
    public static final DeferredItem<Item> SOLAR_WIND_UPGRADE = ITEMS.registerItem("solar_wind_upgrade", Item::new, p -> p.stacksTo(16));
    public static final DeferredItem<Item> MAGNETIC_FIELD_UPGRADE = ITEMS.registerItem("magnetic_field_upgrade", Item::new, p -> p.stacksTo(16));
    public static final DeferredItem<Item> WORMHOLE_LINK = ITEMS.registerItem("wormhole_link", Item::new, p -> p.stacksTo(1));


    public static void Register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
