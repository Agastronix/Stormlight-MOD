package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BreakBondPayload(int entityId) implements CustomPacketPayload {
    public static final Type<BreakBondPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "break_bond"));

    public static final StreamCodec<ByteBuf, BreakBondPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, BreakBondPayload::entityId,
            BreakBondPayload::new
    );

    @Override
    public Type<BreakBondPayload> type() { return TYPE; }
}