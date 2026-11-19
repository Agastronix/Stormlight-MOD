package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SwearWordsPayload(int entityId) implements CustomPacketPayload {
    public static final Type<SwearWordsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "swear_words"));

    public static final StreamCodec<ByteBuf, SwearWordsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SwearWordsPayload::entityId,
            SwearWordsPayload::new
    );

    @Override
    public Type<SwearWordsPayload> type() { return TYPE; }
}