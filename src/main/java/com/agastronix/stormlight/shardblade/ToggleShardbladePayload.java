package com.agastronix.stormlight.shardblade;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleShardbladePayload() implements CustomPacketPayload {
    public static final Type<ToggleShardbladePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "toggle_shardblade"));

    public static final StreamCodec<ByteBuf, ToggleShardbladePayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleShardbladePayload());

    @Override
    public Type<ToggleShardbladePayload> type() { return TYPE; }
}