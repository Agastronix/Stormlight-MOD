package com.agastronix.stormlight.loot;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, StormlightArchiveMOD.MODID);

    public static final Supplier<MapCodec<AddDeadeyeModifier>> ADD_DEADEYE =
            LOOT_MODIFIERS.register("add_deadeye", () -> AddDeadeyeModifier.CODEC);

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIERS.register(modEventBus);
    }
}
