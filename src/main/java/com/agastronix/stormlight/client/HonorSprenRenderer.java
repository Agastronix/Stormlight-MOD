package com.agastronix.stormlight.client;

import com.agastronix.stormlight.StormlightArchiveMOD;
import com.agastronix.stormlight.entity.HonorSprenEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class HonorSprenRenderer extends MobRenderer<HonorSprenEntity, HonorSprenModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/allay/allay.png");

    public HonorSprenRenderer(EntityRendererProvider.Context context) {
        super(context, new HonorSprenModel(context.bakeLayer(ModClientSetup.HONOR_SPREN_LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(HonorSprenEntity entity) {
        return TEXTURE;
    }

    // Always render fully lit — the self-lit "glow".
    @Override
    protected int getBlockLightLevel(HonorSprenEntity entity, BlockPos pos) {
        return 15;
    }

    // Alpha-blended render type so a semi-transparent texture actually shows through.
    @Override
    protected RenderType getRenderType(HonorSprenEntity entity, boolean isVisible,
                                       boolean isVisibleToPlayer, boolean isGlowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }
}