package com.exoticmoss.simpleores.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.exoticmoss.simpleores.block.ModBlocks;

import static com.exoticmoss.simpleores.SimpleOres.MODID;


//Creates the mod's tab in the creative inventory, with all its items
public class ModCreativeModTabs {
    public static final net.neoforged.neoforge.registries.DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            net.neoforged.neoforge.registries.DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SIMPLE_ORES = CREATIVE_MODE_TABS.register("simpleores",
            () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab.simpleores"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.CELESTINE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {

                    //ITEMS//
                    output.accept(ModItems.CELESTINE.get());
                    output.accept(ModItems.ZENITHRA.get());
                    output.accept(ModItems.ASTRALITE.get());
                    output.accept(ModItems.RAW_ASTRALITE.get());
                    output.accept(ModItems.RAW_NEXALITE.get());
                    output.accept(ModItems.NEXALITE.get());
                    output.accept(ModItems.IGNITHRA.get());
                    output.accept(ModItems.RAW_IGNITHRA.get());
                    output.accept(ModItems.ANTRACITE.get());
                    output.accept(ModItems.RAW_OBSCURIDIUM.get());
                    output.accept(ModItems.OBSCURIDIUM.get());
                    output.accept(ModItems.OBSCURITE.get());
                    output.accept(ModItems.WITHER_REAPER.get());


                    //BLOCKS//
                    output.accept(ModBlocks.END_STONE_CELESTINE_ORE.get());
                    output.accept(ModBlocks.NEXALITE_BLOCK.get());
                    output.accept(ModBlocks.IGNITHRA_BLOCK.get());
                    output.accept(ModBlocks.RAW_IGNITHRA_BLOCK.get());
                    output.accept(ModBlocks.DEEPSLATE_IGNITHRA_ORE.get());
                    output.accept(ModBlocks.IGNITHRA_ORE.get());
                    output.accept(ModBlocks.ASTRALITE_BLOCK.get());
                    output.accept(ModBlocks.DEEPSLATE_ASTRALITE_ORE.get());
                    output.accept(ModBlocks.END_STONE_ZENITHRA_ORE.get());
                    output.accept(ModBlocks.RAW_NEXALITE_BLOCK.get());
                    output.accept(ModBlocks.RAW_ASTRALITE_BLOCK.get());
                    output.accept(ModBlocks.ZENITHRA_BLOCK.get());
                    output.accept(ModBlocks.CELESTINE_BLOCK.get());
                    output.accept(ModBlocks.DEEPSLATE_NEXALITE_ORE.get());
                    output.accept(ModBlocks.NETHER_ANTRACITE_ORE.get());
                    output.accept(ModBlocks.ANTRACITE_BLOCK.get());
                    output.accept(ModBlocks.BEDROCK_OBSCURIDIUM_ORE.get());
                    output.accept(ModBlocks.RAW_OBSCURIDIUM_BLOCK.get());
                    output.accept(ModBlocks.OBSCURIDIUM_BLOCK.get());
                    output.accept(ModBlocks.OBSCURITE_BLOCK.get());

                    //GRAVITY CORES//
                    output.accept(ModBlocks.GRAVITY_CORE_SUN.get());
                    output.accept(ModBlocks.GRAVITY_CORE_RED_GIANT.get());
                    output.accept(ModBlocks.GRAVITY_CORE_PULSAR.get());
                    output.accept(ModBlocks.GRAVITY_CORE_BLACK_HOLE.get());

                    //STAR STORAGE//
                    output.accept(ModBlocks.MAGNETAR_CORE.get());
                    output.accept(ModBlocks.STELLAR_DRIVE.get());
                    output.accept(ModBlocks.STELLAR_TERMINAL.get());
                    output.accept(ModBlocks.ADVANCED_TERMINAL.get());
                    output.accept(ModBlocks.BINARY_STAR.get());
                    output.accept(ModBlocks.QUASAR.get());
                    output.accept(ModItems.WHITE_DWARF_CELL.get());
                    output.accept(ModItems.NEUTRON_CELL.get());
                    output.accept(ModItems.SINGULARITY_CELL.get());


            })
                .build());

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
