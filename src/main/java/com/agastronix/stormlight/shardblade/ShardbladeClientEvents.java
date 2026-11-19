package com.agastronix.stormlight.shardblade;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, value = Dist.CLIENT)
public class ShardbladeClientEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ShardbladeKeybinds.TOGGLE_SHARDBLADE.consumeClick()) {
            PacketDistributor.sendToServer(new ToggleShardbladePayload());
        }
    }
}