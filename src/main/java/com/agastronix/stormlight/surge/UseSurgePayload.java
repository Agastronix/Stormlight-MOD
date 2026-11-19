package com.agastronix.stormlight.surge;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UseSurgePayload(int slot) implements CustomPacketPayload {
    public static final Type<UseSurgePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "use_surge"));

    public static final StreamCodec<ByteBuf, UseSurgePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, UseSurgePayload::slot,
            UseSurgePayload::new
    );

    @Override
    public Type<UseSurgePayload> type() { return TYPE; }
}