package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Progress for every {@link OathTask}, in {@code OathTask.values()} order. */
public record OathProgressPayload(List<Integer> values) implements CustomPacketPayload {
    public static final Type<OathProgressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "oath_progress"));

    public static final StreamCodec<ByteBuf, OathProgressPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), OathProgressPayload::values,
            OathProgressPayload::new
    );

    @Override
    public Type<OathProgressPayload> type() { return TYPE; }
}
