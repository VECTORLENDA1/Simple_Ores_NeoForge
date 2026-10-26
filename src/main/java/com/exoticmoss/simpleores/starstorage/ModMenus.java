package com.exoticmoss.simpleores.starstorage;

import com.exoticmoss.simpleores.starstorage.client.BinaryClient;
import com.exoticmoss.simpleores.starstorage.client.TerminalClient;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.exoticmoss.simpleores.SimpleOres.MODID;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    /// The Stellar Terminal. The client reads the position of the terminal and picks how many rows of
    /// items fit in its window (TerminalClient.rows).
    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL = MENUS.register("stellar_terminal",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new TerminalMenu(id, inventory, buf.readBlockPos(), TerminalClient.rows())));

    /// The Quasar (like the terminal: the client reads its position and picks how many rows fit).
    public static final DeferredHolder<MenuType<?>, MenuType<QuasarMenu>> QUASAR = MENUS.register("quasar",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new QuasarMenu(id, inventory, buf.readBlockPos(), TerminalClient.rows())));

    /// The Binary Star (the client reads its position and picks the height of the panel).
    public static final DeferredHolder<MenuType<?>, MenuType<BinaryMenu>> BINARY = MENUS.register("binary_star",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new BinaryMenu(id, inventory, buf.readBlockPos(), BinaryClient.height())));

    /// The Magnetar Core (its upgrades).
    public static final DeferredHolder<MenuType<?>, MenuType<MagnetarMenu>> MAGNETAR = MENUS.register("magnetar_core",
            () -> IMenuTypeExtension.create((id, inventory, buf) -> new MagnetarMenu(id, inventory, buf.readBlockPos())));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
