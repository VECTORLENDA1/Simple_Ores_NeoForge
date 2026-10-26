package com.exoticmoss.simpleores.starstorage;

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

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
