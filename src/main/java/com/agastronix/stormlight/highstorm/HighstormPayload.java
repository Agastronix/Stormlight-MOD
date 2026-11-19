package com.agastronix.stormlight.highstorm;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record HighstormPayload(int phase, int ticks, int length) implements CustomPacketPayload {
    public static final Type<HighstormPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "highstorm"));

    public static final StreamCodec<ByteBuf, HighstormPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HighstormPayload::phase,
            ByteBufCodecs.VAR_INT, HighstormPayload::ticks,
            ByteBufCodecs.VAR_INT, HighstormPayload::length,
            HighstormPayload::new
    );

    @Override
    public Type<HighstormPayload> type() { return TYPE; }
}
