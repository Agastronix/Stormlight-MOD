package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleRadiantPayload() implements CustomPacketPayload {
    public static final Type<ToggleRadiantPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "toggle_radiant"));

    public static final StreamCodec<ByteBuf, ToggleRadiantPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleRadiantPayload());

    @Override
    public Type<ToggleRadiantPayload> type() { return TYPE; }
}