package com.exoticmoss.simpleores;

import com.exoticmoss.simpleores.block.entity.ModBlockEntities;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreItemRenderer;
import com.exoticmoss.simpleores.gravitycraft.GravityCoreRenderer;
import com.exoticmoss.simpleores.screen.CoreSelectionBar;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;

import static com.exoticmoss.simpleores.SimpleOres.MODID;

/// Client side setup (only loaded on the client).
public class SimpleOresClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ModBlockEntities.GRAVITY_CORE_BE.get(), GravityCoreRenderer::new);

        // 3D item model of the Gravity Cores ("type": "simpleores:gravity_core" in the item model files)
        SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(MODID, "gravity_core"), GravityCoreItemRenderer.Unbaked.MAP_CODEC);

        // Selection bar above the Gravity Core (looking, right-click, mouse wheel)
        CoreSelectionBar.register();
    }
}
