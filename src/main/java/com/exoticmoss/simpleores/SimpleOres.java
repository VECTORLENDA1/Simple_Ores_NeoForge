package com.exoticmoss.simpleores;

import com.mojang.logging.LogUtils;
import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreRenderer;
import com.exoticmoss.simpleores.gravitycraft.MoveItemsToCore;
import com.exoticmoss.simpleores.gravitycraft.SelectCoreRecipe;
import com.exoticmoss.simpleores.recipe.ModRecipes;
import com.exoticmoss.simpleores.starstorage.render.StarBlockRenderer;
import com.exoticmoss.simpleores.starstorage.render.StarLook;
import com.exoticmoss.simpleores.starstorage.drive.StellarDriveRenderer;
import com.exoticmoss.simpleores.starstorage.ModDataComponents;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalAction;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalContents;
import com.exoticmoss.simpleores.starstorage.binary.BinaryAction;
import com.exoticmoss.simpleores.starstorage.binary.BinaryView;
import com.exoticmoss.simpleores.starstorage.terminal.CraftRequest;
import com.exoticmoss.simpleores.starstorage.ModMenus;
import com.exoticmoss.simpleores.starstorage.terminal.TerminalMenu;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarFill;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarPatterns;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarCore;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarJobs;
import com.exoticmoss.simpleores.starstorage.quasar.QuasarSlot;
import com.exoticmoss.simpleores.starstorage.client.BinaryScreen;
import com.exoticmoss.simpleores.starstorage.client.QuasarScreen;
import com.exoticmoss.simpleores.starstorage.client.TerminalScreen;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import com.exoticmoss.simpleores.worldgen.ModFeatures;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.exoticmoss.simpleores.block.ModBlocks;
import com.exoticmoss.simpleores.item.ModCreativeModTabs;
import com.exoticmoss.simpleores.item.ModItems;
import org.slf4j.Logger;

@Mod(SimpleOres.MODID)
public class SimpleOres {
    public static final String MODID = "simpleores";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleOres(IEventBus modEventBus, ModContainer modContainer) {


        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);

        ModItems.Register(modEventBus);
        ModBlocks.Register(modEventBus);
        ModRecipes.register(modEventBus);
        ModFeatures.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenus.register(modEventBus);



        ModCreativeModTabs.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(com.exoticmoss.simpleores.starstorage.network.StarChunks::register);
        modEventBus.addListener(com.exoticmoss.simpleores.starstorage.binary.BinaryStarBlockEntity::registerCapabilities);
        // Milk is a liquid too (it can be kept in the Fluid Cells)
        net.neoforged.neoforge.common.NeoForgeMod.enableMilkFluid();
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // Network messages sent between client and server
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        // JEI "Move Items" button for Gravity Core recipes (client -> server)
        event.registrar("1").playToServer(MoveItemsToCore.TYPE, MoveItemsToCore.STREAM_CODEC, MoveItemsToCore::handle)
        // Selection bar above the Gravity Core: the player picked a recipe (client -> server)
                .playToServer(SelectCoreRecipe.TYPE, SelectCoreRecipe.STREAM_CODEC, SelectCoreRecipe::handle)
        // Stellar Terminal: the player took or stored items (client -> server)
                .playToServer(TerminalAction.TYPE, TerminalAction.STREAM_CODEC, TerminalAction::handle)
        // Stellar Terminal: the items of the Magnetar network (server -> client)
                .playToClient(TerminalContents.TYPE, TerminalContents.STREAM_CODEC, TerminalContents::handle)
        // Binary Star screen: the player changed the rules of the slots (client -> server)
                .playToServer(BinaryAction.TYPE, BinaryAction.STREAM_CODEC, BinaryAction::handle)
                .playToServer(BinaryAction.Ghost.TYPE, BinaryAction.Ghost.STREAM_CODEC, BinaryAction.Ghost::handle)
        // Binary Star screen: the blocks around it (server -> client)
                .playToClient(BinaryView.TYPE, BinaryView.STREAM_CODEC, BinaryView::handle)
        // Quasar: the recipes saved in it (server -> client), JEI filled its grid (client -> server)
                .playToClient(QuasarPatterns.TYPE, QuasarPatterns.STREAM_CODEC, QuasarPatterns::handle)
                .playToServer(QuasarFill.TYPE, QuasarFill.STREAM_CODEC, QuasarFill::handle)
                .playToClient(com.exoticmoss.simpleores.starstorage.network.StarTabs.TYPE, com.exoticmoss.simpleores.starstorage.network.StarTabs.STREAM_CODEC,
                        com.exoticmoss.simpleores.starstorage.network.StarTabs::handle)
                .playToServer(com.exoticmoss.simpleores.starstorage.network.StarTabs.Open.TYPE, com.exoticmoss.simpleores.starstorage.network.StarTabs.Open.STREAM_CODEC,
                        com.exoticmoss.simpleores.starstorage.network.StarTabs.Open::handle)
                .playToServer(QuasarSlot.TYPE, QuasarSlot.STREAM_CODEC, QuasarSlot::handle)
                .playToServer(QuasarCore.TYPE, QuasarCore.STREAM_CODEC, QuasarCore::handle)
                .playToClient(QuasarJobs.TYPE, QuasarJobs.STREAM_CODEC, QuasarJobs::handle)
        // Auto-craft: the player asked to craft (client -> server) and the answer (server -> client)
                .playToServer(CraftRequest.TYPE, CraftRequest.STREAM_CODEC, CraftRequest::handle)
                .playToClient(CraftRequest.Reply.TYPE, CraftRequest.Reply.STREAM_CODEC, CraftRequest.Reply::handle)
        // Magnetar screen: how the network is (server -> client)
                .playToClient(com.exoticmoss.simpleores.starstorage.magnetar.MagnetarInfo.TYPE, com.exoticmoss.simpleores.starstorage.magnetar.MagnetarInfo.STREAM_CODEC,
                        com.exoticmoss.simpleores.starstorage.magnetar.MagnetarInfo::handle);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.GRAVITY_CORE_BE.get(), GravityCoreRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.MAGNETAR_BE.get(), context -> new StarBlockRenderer<>(context, StarLook.MAGNETAR));
            event.registerBlockEntityRenderer(ModBlockEntities.STELLAR_TERMINAL_BE.get(), context -> new StarBlockRenderer<>(context, StarLook.TERMINAL));
            event.registerBlockEntityRenderer(ModBlockEntities.STELLAR_DRIVE_BE.get(), StellarDriveRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.ADVANCED_TERMINAL_BE.get(), context -> new StarBlockRenderer<>(context, StarLook.TERMINAL_2));
            event.registerBlockEntityRenderer(ModBlockEntities.BINARY_STAR_BE.get(), context -> new StarBlockRenderer<>(context, StarLook.BINARY));
            event.registerBlockEntityRenderer(ModBlockEntities.QUASAR_BE.get(), context -> new StarBlockRenderer<>(context, StarLook.QUASAR));
        }

        // Screen of the Stellar Terminal
        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.TERMINAL.get(), (TerminalMenu menu, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) -> new TerminalScreen<>(menu, inventory, title));
            event.register(ModMenus.QUASAR.get(), QuasarScreen::new);
            event.register(ModMenus.BINARY.get(), BinaryScreen::new);
            event.register(ModMenus.MAGNETAR.get(), com.exoticmoss.simpleores.starstorage.client.MagnetarScreen::new);
        }

        // 3D items: the Gravity Cores, the stars of the star storage and the cells (their item models are "builtin/entity")
        @SubscribeEvent
        public static void registerItemRenderers(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
            net.neoforged.neoforge.client.extensions.common.IClientItemExtensions extensions = new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                private com.exoticmoss.simpleores.starstorage.render.SpecialItemRenderer renderer;

                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) renderer = new com.exoticmoss.simpleores.starstorage.render.SpecialItemRenderer();
                    return renderer;
                }
            };
            event.registerItem(extensions,
                    ModBlocks.GRAVITY_CORE_SUN.get().asItem(), ModBlocks.GRAVITY_CORE_RED_GIANT.get().asItem(),
                    ModBlocks.GRAVITY_CORE_PULSAR.get().asItem(), ModBlocks.GRAVITY_CORE_BLACK_HOLE.get().asItem(),
                    ModBlocks.MAGNETAR_CORE.get().asItem(), ModBlocks.STELLAR_TERMINAL.get().asItem(), ModBlocks.ADVANCED_TERMINAL.get().asItem(),
                    ModBlocks.BINARY_STAR.get().asItem(), ModBlocks.QUASAR.get().asItem(),
                    ModItems.RED_DWARF_CELL.get(), ModItems.WHITE_DWARF_CELL.get(), ModItems.BLUE_DWARF_CELL.get(), ModItems.PULSAR_CELL.get(),
                    ModItems.NEUTRON_CELL.get(), ModItems.QUARK_CELL.get(), ModItems.PREON_CELL.get(), ModItems.SINGULARITY_CELL.get(),
                    ModItems.RED_DWARF_FLUID_CELL.get(), ModItems.WHITE_DWARF_FLUID_CELL.get(), ModItems.BLUE_DWARF_FLUID_CELL.get(),
                    ModItems.PULSAR_FLUID_CELL.get(), ModItems.NEUTRON_FLUID_CELL.get(), ModItems.QUARK_FLUID_CELL.get(),
                    ModItems.PREON_FLUID_CELL.get(), ModItems.SINGULARITY_FLUID_CELL.get());
        }


    }
}
