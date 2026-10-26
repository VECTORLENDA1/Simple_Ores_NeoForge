package com.exoticmoss.simpleores;

import com.mojang.logging.LogUtils;
import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreRenderer;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreItemRenderer;
import com.exoticmoss.simpleores.gravitycraft.MoveItemsToCore;
import com.exoticmoss.simpleores.gravitycraft.SelectCoreRecipe;
import com.exoticmoss.simpleores.recipe.ModRecipes;
import com.exoticmoss.simpleores.starstorage.MagnetarItemRenderer;
import com.exoticmoss.simpleores.starstorage.MagnetarRenderer;
import com.exoticmoss.simpleores.starstorage.ModDataComponents;
import com.exoticmoss.simpleores.starstorage.TerminalAction;
import com.exoticmoss.simpleores.starstorage.TerminalContents;
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
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.minecraft.resources.Identifier;
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



        ModCreativeModTabs.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerPayloads);
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
                .playToClient(TerminalContents.TYPE, TerminalContents.STREAM_CODEC, TerminalContents::handle);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
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
            event.registerBlockEntityRenderer(ModBlockEntities.MAGNETAR_BE.get(), MagnetarRenderer::new);
        }

        // 3D item model of the Gravity Cores ("type": "simpleores:gravity_core" in the item model files)
        @SubscribeEvent
        public static void registerItemRenderers(RegisterSpecialModelRendererEvent event) {
            event.register(Identifier.fromNamespaceAndPath(MODID, "gravity_core"), GravityCoreItemRenderer.Unbaked.MAP_CODEC);
            // 3D item model of the Magnetar Core ("type": "simpleores:magnetar")
            event.register(Identifier.fromNamespaceAndPath(MODID, "magnetar"), MagnetarItemRenderer.Unbaked.MAP_CODEC);
        }

    }
}
