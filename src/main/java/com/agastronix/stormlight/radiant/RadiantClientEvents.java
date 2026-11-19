package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, value = Dist.CLIENT)
public class RadiantClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (RadiantKeybinds.TOGGLE_RADIANT.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleRadiantPayload());
        }
    }
}