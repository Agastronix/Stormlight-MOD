package com.agastronix.stormlight.client;

import com.agastronix.stormlight.entity.HonorSprenEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

public class HonorSprenModel extends HierarchicalModel<HonorSprenEntity> {
    private final ModelPart root;       // baked mesh root (what root() must return)
    private final ModelPart allayRoot;  // the allay "root" bone — bob this
    private final ModelPart head;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public HonorSprenModel(ModelPart root) {
        this.root = root;
        this.allayRoot = root.getChild("root");
        this.head = this.allayRoot.getChild("head");
        ModelPart body = this.allayRoot.getChild("body"); // wings live under body
        this.rightWing = body.getChild("right_wing");
        this.leftWing = body.getChild("left_wing");
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(HonorSprenEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);

        // Head follows the look direction.
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Fast wing flap, like a hovering allay.
        float flap = Mth.cos(ageInTicks * 1.3F) * 0.6F;
        this.rightWing.zRot = -0.4F - flap;
        this.leftWing.zRot = 0.4F + flap;
        this.rightWing.yRot = -0.2F;
        this.leftWing.yRot = 0.2F;

        // Gentle vertical bob of the whole spren.
        this.allayRoot.y += Mth.cos(ageInTicks * 0.18F) * 0.8F;
    }
}