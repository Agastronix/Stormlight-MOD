package com.agastronix.stormlight.client;

import com.agastronix.stormlight.entity.CultivationSprenEntity;
import net.minecraft.client.animation.definitions.FrogAnimation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Vanilla frog geometry. Part names come straight from {@code FrogModel.createBodyLayer()}:
 * root -> body -> {head -> eyes, croaking_body, tongue, left_arm, right_arm},
 * root -> {left_leg, right_leg}.
 */
public class CultivationSprenModel extends HierarchicalModel<CultivationSprenEntity> {
    private final ModelPart root;          // baked mesh root (what root() must return)
    private final ModelPart croakingBody;

    public CultivationSprenModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("root").getChild("body");
        this.croakingBody = body.getChild("croaking_body");
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(CultivationSprenEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);

        // Vanilla's own frog walk cycle drives the limbs — no entity animation state needed.
        // No head look rotation: the frog's "head" part is only the upper jaw, so rotating it
        // pries the mouth open. Vanilla doesn't rotate it either; the whole body turns instead.
        this.animateWalk(FrogAnimation.FROG_WALK, limbSwing, limbSwingAmount, 1.5F, 2.5F);

        // Vanilla only shows the puffed throat mid-croak; this spren never croaks.
        this.croakingBody.visible = false;
    }
}
