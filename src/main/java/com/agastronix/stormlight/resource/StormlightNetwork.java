package com.agastronix.stormlight.resource;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.highstorm.ClientHighstormData;
import com.agastronix.stormlight.highstorm.HighstormPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD)
public class StormlightNetwork {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                StormlightSyncPayload.TYPE,
                StormlightSyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientStormlightData.set(payload.stormlight(), payload.max()))
        );
        registrar.playToClient(
                HighstormPayload.TYPE,
                HighstormPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ClientHighstormData.set(payload.phase(), payload.ticks(), payload.length()))
        );
    }

    public static void sendToClient(ServerPlayer player, float stormlight, float max) {
        PacketDistributor.sendToPlayer(player, new StormlightSyncPayload(stormlight, max));
    }
}