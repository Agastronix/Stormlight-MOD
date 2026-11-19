package com.agastronix.stormlight.shardblade;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public record OpenDeadeyeBondPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<OpenDeadeyeBondPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "open_deadeye_bond"));

    public static final StreamCodec<ByteBuf, OpenDeadeyeBondPayload> STREAM_CODEC = StreamCodec.composite(
            BondDeadeyePayload.HAND_CODEC, OpenDeadeyeBondPayload::hand,
            OpenDeadeyeBondPayload::new
    );

    @Override
    public Type<OpenDeadeyeBondPayload> type() { return TYPE; }
}
