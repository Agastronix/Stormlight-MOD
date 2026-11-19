package com.agastronix.stormlight.resource;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StormlightSyncPayload(float stormlight, float max) implements CustomPacketPayload {
    public static final Type<StormlightSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "stormlight_sync"));

    public static final StreamCodec<ByteBuf, StormlightSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StormlightSyncPayload::stormlight,
            ByteBufCodecs.FLOAT, StormlightSyncPayload::max,
            StormlightSyncPayload::new
    );

    @Override
    public Type<StormlightSyncPayload> type() {
        return TYPE;
    }
}