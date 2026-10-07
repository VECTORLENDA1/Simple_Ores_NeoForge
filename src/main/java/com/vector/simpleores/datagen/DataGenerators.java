package com.vector.simpleores.datagen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import com.vector.simpleores.SimpleOres;

@EventBusSubscriber(modid = SimpleOres.MODID)
public class DataGenerators {
    // The "data" run uses clientData(), so we listen to GatherDataEvent.Client (it also generates the server data).
    // ExistingFileHelper and includeServer() were removed; createProvider passes the output and lookup by itself.
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(ModBlockTagProvider::new);
        event.createProvider(ModWorldGenProvider::new);
    }
}
