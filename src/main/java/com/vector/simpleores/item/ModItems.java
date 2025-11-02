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
    public static final DeferredItem<Item> CELESTINE = ITEMS.register("celestine",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZENITHRA = ITEMS.register("zenithra",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ASTRALITE = ITEMS.register("astralite",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_ASTRALITE = ITEMS.register("raw_astralite",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_NEXALITE = ITEMS.register("raw_nexalite",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NEXALITE = ITEMS.register("nexalite",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IGNITHRA = ITEMS.register("ignithra",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_IGNITHRA = ITEMS.register("raw_ignithra",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ANTRACITE = ITEMS.register("antracite",
            () -> new FuelItem(new Item.Properties(), 3200));
    public static final DeferredItem<Item> RAW_OBSCURIDIUM = ITEMS.register("raw_obscuridium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSCURIDIUM = ITEMS.register("obscuridium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OBSCURITE = ITEMS.register("obscurite",
            () -> new Item(new Item.Properties()));



    public static void Register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
