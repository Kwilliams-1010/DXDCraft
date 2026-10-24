package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.entity.HeavenlyDragon;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Simple original blocky dragon: body, neck, horned head, two wings, two-part tail, four legs. */
public class DragonBossModel extends HierarchicalModel<HeavenlyDragon> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DxDCraft.id("heavenly_dragon"), "main");

    private final ModelPart root;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWingTip;
    private final ModelPart tail;
    private final ModelPart tailEnd;

    public DragonBossModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.leftWingTip = leftWing.getChild("left_wing_tip");
        this.rightWingTip = rightWing.getChild("right_wing_tip");
        this.tail = body.getChild("tail");
        this.tailEnd = tail.getChild("tail_end");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -5.0F, -12.0F, 12, 10, 24), PartPose.offset(0.0F, 10.0F, 0.0F));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-3.0F, -3.0F, -10.0F, 6, 6, 10), PartPose.offsetAndRotation(0.0F, -2.0F, -12.0F, -0.3F, 0.0F, 0.0F));

        neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(32, 34).addBox(-5.0F, -4.0F, -10.0F, 10, 8, 10)
                .texOffs(72, 0).addBox(-4.0F, -9.0F, -3.0F, 2, 5, 2)
                .texOffs(72, 0).addBox(2.0F, -9.0F, -3.0F, 2, 5, 2)
                .texOffs(80, 0).addBox(-3.0F, 0.0F, -15.0F, 6, 4, 5), PartPose.offsetAndRotation(0.0F, 0.0F, -10.0F, 0.3F, 0.0F, 0.0F));

        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 52).addBox(0.0F, -1.0F, -6.0F, 20, 2, 14), PartPose.offset(6.0F, -4.0F, -4.0F));
        leftWing.addOrReplaceChild("left_wing_tip", CubeListBuilder.create()
                .texOffs(0, 70).addBox(0.0F, -0.5F, -6.0F, 18, 1, 12), PartPose.offset(20.0F, 0.0F, 0.0F));

        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 52).mirror().addBox(-20.0F, -1.0F, -6.0F, 20, 2, 14), PartPose.offset(-6.0F, -4.0F, -4.0F));
        rightWing.addOrReplaceChild("right_wing_tip", CubeListBuilder.create()
                .texOffs(0, 70).mirror().addBox(-18.0F, -0.5F, -6.0F, 18, 1, 12), PartPose.offset(-20.0F, 0.0F, 0.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(72, 34).addBox(-3.0F, -3.0F, 0.0F, 6, 6, 16), PartPose.offset(0.0F, -1.0F, 12.0F));
        tail.addOrReplaceChild("tail_end", CubeListBuilder.create()
                .texOffs(72, 60).addBox(-2.0F, -2.0F, 0.0F, 4, 4, 14), PartPose.offset(0.0F, 0.0F, 16.0F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(100, 0).addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4);
        body.addOrReplaceChild("front_left_leg", leg, PartPose.offset(5.0F, 4.0F, -8.0F));
        body.addOrReplaceChild("front_right_leg", leg, PartPose.offset(-5.0F, 4.0F, -8.0F));
        body.addOrReplaceChild("back_left_leg", leg, PartPose.offset(5.0F, 4.0F, 8.0F));
        body.addOrReplaceChild("back_right_leg", leg, PartPose.offset(-5.0F, 4.0F, 8.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(HeavenlyDragon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float flap = Mth.sin(ageInTicks * 0.18F);
        leftWing.zRot = flap * 0.55F;
        rightWing.zRot = -flap * 0.55F;
        leftWingTip.zRot = flap * 0.35F;
        rightWingTip.zRot = -flap * 0.35F;

        tail.yRot = Mth.sin(ageInTicks * 0.08F) * 0.2F;
        tail.xRot = -0.1F;
        tailEnd.yRot = Mth.sin(ageInTicks * 0.08F - 0.8F) * 0.3F;

        neck.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        head.xRot = 0.3F + headPitch * Mth.DEG_TO_RAD * 0.5F;
    }
}
