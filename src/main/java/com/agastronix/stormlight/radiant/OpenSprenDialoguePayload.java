package com.agastronix.stormlight.radiant;

import com.agastronix.stormlight.StormlightArchiveMOD;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenSprenDialoguePayload(int entityId, int currentOath, int maxOath, boolean manage)
        implements CustomPacketPayload {
    public static final Type<OpenSprenDialoguePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "open_spren_dialogue"));

    public static final StreamCodec<ByteBuf, OpenSprenDialoguePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OpenSprenDialoguePayload::entityId,
            ByteBufCodecs.INT, OpenSprenDialoguePayload::currentOath,
            ByteBufCodecs.INT, OpenSprenDialoguePayload::maxOath,
            ByteBufCodecs.BOOL, OpenSprenDialoguePayload::manage,
            OpenSprenDialoguePayload::new
    );

    @Override
    public Type<OpenSprenDialoguePayload> type() { return TYPE; }
}