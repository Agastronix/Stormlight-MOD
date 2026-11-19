package com.agastronix.stormlight.shardblade;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.InteractionHand;

public record BondDeadeyePayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<BondDeadeyePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "bond_deadeye"));

    // Out-of-range ids fall back to the main hand instead of failing to decode.
    public static final StreamCodec<ByteBuf, InteractionHand> HAND_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(InteractionHand::ordinal, InteractionHand.values(), ByIdMap.OutOfBoundsStrategy.ZERO),
            InteractionHand::ordinal);

    public static final StreamCodec<ByteBuf, BondDeadeyePayload> STREAM_CODEC = StreamCodec.composite(
            HAND_CODEC, BondDeadeyePayload::hand,
            BondDeadeyePayload::new
    );

    @Override
    public Type<BondDeadeyePayload> type() { return TYPE; }
}
