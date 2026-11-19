package com.agastronix.stormlight.client;

import com.agastronix.stormlight.entity.CultivationSprenEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class CultivationSprenRenderer extends MobRenderer<CultivationSprenEntity, CultivationSprenModel> {
    // The green frog. In 1.21.1 the variants are temperate (orange) / warm (white) / cold (green).
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/frog/cold_frog.png");

    public CultivationSprenRenderer(EntityRendererProvider.Context context) {
        super(context, new CultivationSprenModel(context.bakeLayer(ModClientSetup.CULTIVATION_SPREN_LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(CultivationSprenEntity entity) {
        return TEXTURE;
    }

    // Always render fully lit — the self-lit "glow" that marks it as a spren.
    @Override
    protected int getBlockLightLevel(CultivationSprenEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected RenderType getRenderType(CultivationSprenEntity entity, boolean isVisible,
                                       boolean isVisibleToPlayer, boolean isGlowing) {
        return RenderType.entityTranslucent(getTextureLocation(entity));
    }
}
