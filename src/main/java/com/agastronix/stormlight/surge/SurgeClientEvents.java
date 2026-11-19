package com.agastronix.stormlight.surge;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.radiant.ClientRadiantData;
import com.agastronix.stormlight.radiant.RadiantOrder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, value = Dist.CLIENT)
public class SurgeClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (SurgeKeybinds.SURGE_PRIMARY.consumeClick()) {
            if (ClientRadiantData.order == RadiantOrder.EDGEDANCER.ordinal()) {
                AbrasionClient.toggle();               // Edgedancer primary = Abrasion (client-side)
            } else {
                PacketDistributor.sendToServer(new UseSurgePayload(0));
            }
        }
        while (SurgeKeybinds.SURGE_SECONDARY.consumeClick()) {
            PacketDistributor.sendToServer(new UseSurgePayload(1));
        }
    }
}