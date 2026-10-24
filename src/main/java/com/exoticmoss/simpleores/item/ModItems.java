package com.exoticmoss.simpleores.item;

import com.exoticmoss.simpleores.Registered;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import com.exoticmoss.simpleores.item.custom.*;
import net.fabricmc.fabric.api.registry.FuelValueEvents;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.exoticmoss.simpleores.SimpleOres.MODID;


public class ModItems {

    //Items//
    public static final Registered<Item, Item> CELESTINE = registerSimpleItem("celestine");
    public static final Registered<Item, Item> ZENITHRA = registerSimpleItem("zenithra");
    public static final Registered<Item, Item> ASTRALITE = registerSimpleItem("astralite");
    public static final Registered<Item, Item> RAW_ASTRALITE = registerSimpleItem("raw_astralite");
    public static final Registered<Item, Item> RAW_NEXALITE = registerSimpleItem("raw_nexalite");
    public static final Registered<Item, Item> NEXALITE = registerSimpleItem("nexalite");
    public static final Registered<Item, Item> IGNITHRA = registerSimpleItem("ignithra");
    public static final Registered<Item, Item> RAW_IGNITHRA = registerSimpleItem("raw_ignithra");
    public static final Registered<Item, Item> ANTRACITE = registerItem("antracite", p -> new FuelItem(p, 3200), p -> p);
    public static final Registered<Item, Item> RAW_OBSCURIDIUM = registerSimpleItem("raw_obscuridium");
    public static final Registered<Item, Item> OBSCURIDIUM = registerSimpleItem("obscuridium");
    public static final Registered<Item, Item> OBSCURITE = registerSimpleItem("obscurite");

    //Weapons//
    public static final Registered<Item, Item> WITHER_REAPER = registerItem("wither_reaper", WitherReaperItem::new,
            p -> p.sword(ModToolMaterials.WITHER_REAPER, 0, -2.0F)); /// base damage = 1, base attack speed = -4.0


    public static Registered<Item, Item> registerSimpleItem(String name) {
        return registerItem(name, Item::new, p -> p);
    }

    // The Properties must contain the item ID (setId), like vanilla does
    public static Registered<Item, Item> registerItem(String name, Function<Item.Properties, ? extends Item> factory,
                                                      UnaryOperator<Item.Properties> properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, name));
        Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.apply(new Item.Properties().setId(key))));
        return new Registered<>(key, item);
    }

    /// The item of a block (same ID as the block, name from the block's lang key).
    public static Registered<Item, Item> registerSimpleBlockItem(String name, Block block, UnaryOperator<Item.Properties> properties) {
        return registerItem(name, p -> new BlockItem(block, p), p -> properties.apply(p.useBlockDescriptionPrefix()));
    }

    public static void Register() {
        // The furnace burn time of the fuel items (NeoForge's Item#getBurnTime does not exist on Fabric)
        FuelValueEvents.BUILD.register((builder, context) -> {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof FuelItem fuel) builder.add(fuel, fuel.getBurnTime());
            }
        });
    }
}
