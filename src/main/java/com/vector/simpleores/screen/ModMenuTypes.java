package com.vector.simpleores.screen;

import com.vector.simpleores.screen.custom.AtomicCraftingTableMenu;
import com.vector.simpleores.screen.custom.SimpleCraftingTableMenu;
import com.vector.simpleores.screen.custom.UltraCraftingTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;



public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, "simpleores");

    public static final DeferredHolder<MenuType<?>, MenuType<SimpleCraftingTableMenu>> SIMPLE_CRAFTING_TABLE_MENU =
            registerMenuType("simple_crafting_table_menu", SimpleCraftingTableMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<UltraCraftingTableMenu>> ULTRA_CRAFTING_TABLE_MENU =
            registerMenuType("ultra_crafting_table_menu", UltraCraftingTableMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<AtomicCraftingTableMenu>> ATOMIC_CRAFTING_TABLE_MENU =
            registerMenuType("atomic_crafting_table_menu", AtomicCraftingTableMenu::new);


    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name,
                                                                                                              IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
