package com.agastronix.stormlight.entity;

import com.agastronix.stormlight.StormlightArchiveMOD;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, StormlightArchiveMOD.MODID);

    public static final Supplier<EntityType<HonorSprenEntity>> HONOR_SPREN =
            ENTITY_TYPES.register("honor_spren", () -> EntityType.Builder
                    .of(HonorSprenEntity::new, MobCategory.CREATURE)
                    .sized(0.35F, 0.6F)          // same footprint as an allay
                    .clientTrackingRange(8)
                    .build("honor_spren"));

    public static final Supplier<EntityType<CultivationSprenEntity>> CULTIVATION_SPREN =
            ENTITY_TYPES.register("cultivation_spren", () -> EntityType.Builder
                    .of(CultivationSprenEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.5F)          // same footprint as a frog
                    .clientTrackingRange(8)
                    .build("cultivation_spren"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HONOR_SPREN.get(), HonorSprenEntity.createAttributes().build());
        event.put(CULTIVATION_SPREN.get(), CultivationSprenEntity.createAttributes().build());
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}