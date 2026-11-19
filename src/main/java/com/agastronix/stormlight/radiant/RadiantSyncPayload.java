package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RadiantSyncPayload(int order, int oath, boolean active) implements CustomPacketPayload {
    public static final Type<RadiantSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "radiant_sync"));

    public static final StreamCodec<ByteBuf, RadiantSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RadiantSyncPayload::order,
            ByteBufCodecs.INT, RadiantSyncPayload::oath,
            ByteBufCodecs.BOOL, RadiantSyncPayload::active,
            RadiantSyncPayload::new
    );

    @Override
    public Type<RadiantSyncPayload> type() { return TYPE; }
}