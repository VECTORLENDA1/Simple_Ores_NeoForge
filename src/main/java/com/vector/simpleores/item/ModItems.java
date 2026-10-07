package com.vector.simpleores.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.vector.simpleores.item.custom.*;

import static com.vector.simpleores.SimpleOres.MODID;


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



    public static void Register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
