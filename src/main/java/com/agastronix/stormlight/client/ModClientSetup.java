package com.agastronix.stormlight.client;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.entity.ModEntities;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.model.FrogModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = StormlightArchiveMOD.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientSetup {
    public static final ModelLayerLocation HONOR_SPREN_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "honor_spren"), "main");

    public static final ModelLayerLocation CULTIVATION_SPREN_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(StormlightArchiveMOD.MODID, "cultivation_spren"), "main");

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HONOR_SPREN_LAYER, AllayModel::createBodyLayer);
        event.registerLayerDefinition(CULTIVATION_SPREN_LAYER, FrogModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HONOR_SPREN.get(), HonorSprenRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIVATION_SPREN.get(), CultivationSprenRenderer::new);
    }
}