package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Geometry for the Sacred Gear visuals.
 *  - ARMOR_BB / ARMOR_JD: the full-body Balance Breaker / Juggernaut Drive armor shell (64x32 humanoid layout).
 *  - EXTRAS: horns, crest, halo, wings, pauldrons, claws, tail and back core that sit on top of the shell.
 *  - WORN: what a gear looks like while simply worn in a Curios slot (gauntlet, bracer, belt, pendant, rings, folded wings).
 */
public final class GearModels {
    public static final ModelLayerLocation ARMOR_BB = new ModelLayerLocation(DxDCraft.id("balance_breaker_armor"), "main");
    public static final ModelLayerLocation ARMOR_JD = new ModelLayerLocation(DxDCraft.id("juggernaut_armor"), "main");
    public static final ModelLayerLocation EXTRAS = new ModelLayerLocation(DxDCraft.id("gear_extras"), "main");
    public static final ModelLayerLocation WORN = new ModelLayerLocation(DxDCraft.id("worn_gear"), "main");

    private GearModels() {}

    public static LayerDefinition armorBB() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.1F), 0.0F), 64, 32);
    }

    public static LayerDefinition armorJD() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.45F), 0.0F), 64, 32);
    }

    // ------------------------------------------------------------------ extras

    /** Texture map (64x64): metal everywhere except wing membranes (0,32) and the halo ring (24,48). */
    public static LayerDefinition extras() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        // Empty the base cubes: only the add-ons below are drawn by this model.
        for (String part : new String[]{"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            PartPose pose = switch (part) {
                case "right_arm" -> PartPose.offset(-5.0F, 2.0F, 0.0F);
                case "left_arm" -> PartPose.offset(5.0F, 2.0F, 0.0F);
                case "right_leg" -> PartPose.offset(-1.9F, 12.0F, 0.0F);
                case "left_leg" -> PartPose.offset(1.9F, 12.0F, 0.0F);
                default -> PartPose.ZERO;
            };
            root.addOrReplaceChild(part, CubeListBuilder.create(), pose);
        }
        PartDefinition head = root.getChild("head");
        PartDefinition body = root.getChild("body");
        PartDefinition rArm = root.getChild("right_arm");
        PartDefinition lArm = root.getChild("left_arm");

        // Horns: swept back from the temples.
        head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2)
                .texOffs(8, 0).addBox(-0.5F, -10.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(3.2F, -8.4F, -1.5F, -0.75F, 0.0F, 0.35F));
        head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2)
                .texOffs(8, 0).mirror().addBox(-0.5F, -10.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-3.2F, -8.4F, -1.5F, -0.75F, 0.0F, -0.35F));
        // Crest: a fin running over the helmet.
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(12, 0).addBox(-0.5F, -3.5F, -6.0F, 1, 4, 11),
                PartPose.offsetAndRotation(0.0F, -9.0F, 0.5F, 0.15F, 0.0F, 0.0F));
        // Halo: a flat ring floating above the head (zero-thickness box).
        head.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(24, 48).addBox(-5.0F, 0.0F, -5.0F, 10, 0, 10),
                PartPose.offset(0.0F, -13.5F, 0.0F));

        // Back core: a jewel housing between the shoulder blades.
        body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(36, 0).addBox(-2.5F, 2.0F, 3.0F, 5, 5, 2),
                PartPose.ZERO);
        // Wings: a bone with a membrane hanging from it. Animated (spread + flap) in the renderer.
        body.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, -1.0F, -0.5F, 15, 2, 1)
                .texOffs(0, 32).addBox(0.0F, 1.0F, 0.0F, 15, 13, 0), PartPose.offset(1.5F, 2.0F, 2.6F));
        body.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(0, 26).mirror().addBox(-15.0F, -1.0F, -0.5F, 15, 2, 1)
                .texOffs(0, 32).mirror().addBox(-15.0F, 1.0F, 0.0F, 15, 13, 0), PartPose.offset(-1.5F, 2.0F, 2.6F));
        // Tail: hangs from the lower back.
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(40, 8).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 8)
                .texOffs(40, 20).addBox(-1.0F, -1.0F, 8.0F, 2, 2, 6), PartPose.offsetAndRotation(0.0F, 10.5F, 2.5F, -0.55F, 0.0F, 0.0F));

        // Pauldrons on both shoulders.
        rArm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(0, 10).addBox(-5.0F, -4.0F, -3.5F, 7, 4, 7)
                .texOffs(28, 10).addBox(-5.5F, -5.5F, -0.5F, 3, 2, 1), PartPose.rotation(0.0F, 0.0F, 0.18F));
        lArm.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(0, 10).mirror().addBox(-2.0F, -4.0F, -3.5F, 7, 4, 7)
                .texOffs(28, 10).mirror().addBox(2.5F, -5.5F, -0.5F, 3, 2, 1), PartPose.rotation(0.0F, 0.0F, -0.18F));
        // Claws at the fingertips.
        CubeListBuilder claws = CubeListBuilder.create().texOffs(28, 14)
                .addBox(-2.6F, 10.5F, -2.8F, 1, 4, 1).addBox(-1.0F, 10.5F, -2.8F, 1, 4, 1).addBox(0.6F, 10.5F, -2.8F, 1, 4, 1);
        rArm.addOrReplaceChild("claws", claws, PartPose.ZERO);
        lArm.addOrReplaceChild("claws", CubeListBuilder.create().texOffs(28, 14)
                .addBox(-1.6F, 10.5F, -2.8F, 1, 4, 1).addBox(0.0F, 10.5F, -2.8F, 1, 4, 1).addBox(1.6F, 10.5F, -2.8F, 1, 4, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Humanoid model whose base boxes are empty: the renderer toggles the add-on parts. */
    public static final class ExtrasModel extends HumanoidModel<LivingEntity> {
        public final ModelPart hornL, hornR, crest, halo, core, wingL, wingR, tail, pauldronL, pauldronR, clawsL, clawsR;

        public ExtrasModel(ModelPart root) {
            super(root);
            hornL = head.getChild("horn_l");
            hornR = head.getChild("horn_r");
            crest = head.getChild("crest");
            halo = head.getChild("halo");
            core = body.getChild("core");
            wingL = body.getChild("wing_l");
            wingR = body.getChild("wing_r");
            tail = body.getChild("tail");
            pauldronL = leftArm.getChild("pauldron");
            pauldronR = rightArm.getChild("pauldron");
            clawsL = leftArm.getChild("claws");
            clawsR = rightArm.getChild("claws");
        }

        public void hideAllExtras() {
            for (ModelPart p : new ModelPart[]{hornL, hornR, crest, halo, core, wingL, wingR, tail, pauldronL, pauldronR, clawsL, clawsR}) {
                p.visible = false;
            }
        }
    }

    // ------------------------------------------------------------------ worn (Curios)

    /** Texture map (64x32): metal for everything, gem cubes at (48,0). Tinted per gear. */
    public static LayerDefinition worn() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        for (String part : new String[]{"head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            PartPose pose = switch (part) {
                case "right_arm" -> PartPose.offset(-5.0F, 2.0F, 0.0F);
                case "left_arm" -> PartPose.offset(5.0F, 2.0F, 0.0F);
                case "right_leg" -> PartPose.offset(-1.9F, 12.0F, 0.0F);
                case "left_leg" -> PartPose.offset(1.9F, 12.0F, 0.0F);
                default -> PartPose.ZERO;
            };
            root.addOrReplaceChild(part, CubeListBuilder.create(), pose);
        }
        PartDefinition body = root.getChild("body");
        PartDefinition rArm = root.getChild("right_arm");
        PartDefinition lArm = root.getChild("left_arm");
        CubeDeformation snug = new CubeDeformation(0.35F);

        // Gauntlet on the left forearm and hand, with knuckle plates and a jewel on the back of the hand.
        lArm.addOrReplaceChild("gauntlet", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, 3.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.45F))
                .texOffs(16, 0).addBox(-1.5F, 9.5F, -3.0F, 5, 2, 1)
                .texOffs(16, 4).addBox(3.2F, 2.5F, -1.5F, 1, 5, 3), PartPose.ZERO);
        lArm.addOrReplaceChild("gauntlet_gem", CubeListBuilder.create().texOffs(48, 0).addBox(3.3F, 7.5F, -1.0F, 1, 2, 2),
                PartPose.ZERO);
        // Bracer on the right wrist.
        rArm.addOrReplaceChild("bracer", CubeListBuilder.create().texOffs(0, 14).addBox(-3.0F, 6.5F, -2.0F, 4, 3, 4, snug),
                PartPose.ZERO);
        rArm.addOrReplaceChild("bracer_gem", CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, 7.0F, -2.9F, 1, 2, 1),
                PartPose.ZERO);
        // Belt with a buckle.
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(16, 14).addBox(-4.0F, 9.5F, -2.0F, 8, 2, 4, snug)
                .texOffs(40, 14).addBox(-1.5F, 9.0F, -2.8F, 3, 3, 1), PartPose.ZERO);
        body.addOrReplaceChild("belt_gem", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 9.8F, -3.1F, 1, 1, 1),
                PartPose.ZERO);
        // Pendant on a chain.
        body.addOrReplaceChild("pendant", CubeListBuilder.create().texOffs(16, 22).addBox(-3.0F, 0.2F, -2.5F, 6, 1, 1)
                .texOffs(32, 22).addBox(-1.5F, 1.0F, -2.6F, 3, 3, 1), PartPose.ZERO);
        body.addOrReplaceChild("pendant_gem", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 1.8F, -2.9F, 1, 1, 1),
                PartPose.ZERO);
        // Rings on both hands.
        rArm.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 22).addBox(-3.0F, 10.0F, -2.0F, 4, 1, 4, new CubeDeformation(0.2F)),
                PartPose.ZERO);
        lArm.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, 10.0F, -2.0F, 4, 1, 4, new CubeDeformation(0.2F)),
                PartPose.ZERO);
        // Folded wings on the back.
        body.addOrReplaceChild("folded_l", CubeListBuilder.create().texOffs(56, 4).addBox(0.0F, 0.0F, 0.0F, 2, 9, 1),
                PartPose.offsetAndRotation(0.5F, 1.0F, 2.2F, 0.2F, 0.0F, -0.25F));
        body.addOrReplaceChild("folded_r", CubeListBuilder.create().texOffs(56, 4).mirror().addBox(-2.0F, 0.0F, 0.0F, 2, 9, 1),
                PartPose.offsetAndRotation(-0.5F, 1.0F, 2.2F, 0.2F, 0.0F, 0.25F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static final class WornModel extends HumanoidModel<LivingEntity> {
        public final ModelPart gauntlet, gauntletGem, bracer, bracerGem, belt, beltGem, pendant, pendantGem, ringL, ringR, foldedL, foldedR;

        public WornModel(ModelPart root) {
            super(root);
            gauntlet = leftArm.getChild("gauntlet");
            gauntletGem = leftArm.getChild("gauntlet_gem");
            bracer = rightArm.getChild("bracer");
            bracerGem = rightArm.getChild("bracer_gem");
            belt = body.getChild("belt");
            beltGem = body.getChild("belt_gem");
            pendant = body.getChild("pendant");
            pendantGem = body.getChild("pendant_gem");
            ringL = leftArm.getChild("ring");
            ringR = rightArm.getChild("ring");
            foldedL = body.getChild("folded_l");
            foldedR = body.getChild("folded_r");
        }

        public ModelPart[] all() {
            return new ModelPart[]{gauntlet, gauntletGem, bracer, bracerGem, belt, beltGem, pendant, pendantGem, ringL, ringR, foldedL, foldedR};
        }
    }
}
