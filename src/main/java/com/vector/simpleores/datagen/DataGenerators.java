package com.vector.simpleores.datagen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import com.vector.simpleores.SimpleOres;

@EventBusSubscriber(modid = SimpleOres.MODID)
public class DataGenerators {
    // O run "data" usa clientData(), por isso ouvimos o GatherDataEvent.Client (gera tambem os dados de servidor).
    // O ExistingFileHelper e o includeServer() foram removidos; o createProvider passa o output e o lookup sozinho.
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ModBlockTagProvider::new);
        event.createProvider(ModWorldGenProvider::new);
    }
}
