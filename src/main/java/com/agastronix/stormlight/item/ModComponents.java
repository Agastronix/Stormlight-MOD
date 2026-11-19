package com.agastronix.stormlight.item;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Supplier;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, StormlightArchiveMOD.MODID);

    public static final Supplier<DataComponentType<Boolean>> RADIANT =
            COMPONENTS.register("radiant", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // Marks a Deadeye blade bonded to a player: the stored blade and its summoned copy.
    public static final Supplier<DataComponentType<UUID>> DEADEYE_OWNER =
            COMPONENTS.register("deadeye_owner", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}