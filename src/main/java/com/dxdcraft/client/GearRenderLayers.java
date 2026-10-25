package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.compat.CuriosCompat;
import com.dxdcraft.item.SacredGearItem;
import com.dxdcraft.logic.BalanceBreaker;
import com.dxdcraft.logic.GearGrowth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Draws Balance Breaker / Juggernaut Drive armor and worn Sacred Gears on players. */
@EventBusSubscriber(modid = DxDCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearRenderLayers {
    private GearRenderLayers() {}

    @SubscribeEvent
    public static void onLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GearModels.ARMOR_BB, GearModels::armorBB);
        event.registerLayerDefinition(GearModels.ARMOR_JD, GearModels::armorJD);
        event.registerLayerDefinition(GearModels.EXTRAS, GearModels::extras);
        event.registerLayerDefinition(GearModels.WORN, GearModels::worn);
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer renderer) {
                var models = event.getEntityModels();
                renderer.addLayer(new TransformLayer(renderer,
                        new HumanoidModel<>(models.bakeLayer(GearModels.ARMOR_BB)),
                        new HumanoidModel<>(models.bakeLayer(GearModels.ARMOR_JD)),
                        new GearModels.ExtrasModel(models.bakeLayer(GearModels.EXTRAS))));
                renderer.addLayer(new WornLayer(renderer, new GearModels.WornModel(models.bakeLayer(GearModels.WORN))));
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static final Map<String, ResourceLocation> TEX = new HashMap<>();

    static ResourceLocation tex(String path) {
        return TEX.computeIfAbsent(path, p -> DxDCraft.id("textures/" + p + ".png"));
    }

    static int argb(float alpha, int rgb) {
        return (Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** rgb scaled by brightness, alpha 255 (for additive "eyes" glow). */
    static int glow(float brightness, int rgb) {
        float b = Mth.clamp(brightness, 0.0F, 1.0F);
        int r = (int) (((rgb >> 16) & 255) * b), g = (int) (((rgb >> 8) & 255) * b), bl = (int) ((rgb & 255) * b);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void copyPose(PlayerModel<AbstractClientPlayer> parent, HumanoidModel<?> target) {
        ((HumanoidModel) parent).copyPropertiesTo(target);
    }

    static void scale(ModelPart part, float s) {
        part.xScale = s;
        part.yScale = s;
        part.zScale = s;
    }

    // ------------------------------------------------------------------ transformation armor

    static final class TransformLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private static final int WINDOW = BalanceBreaker.ASSEMBLE_TICKS / 5;
        private final HumanoidModel<LivingEntity> bb, jd;
        private final GearModels.ExtrasModel extras;

        TransformLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                       HumanoidModel<LivingEntity> bb, HumanoidModel<LivingEntity> jd, GearModels.ExtrasModel extras) {
            super(parent);
            this.bb = bb;
            this.jd = jd;
            this.extras = extras;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            TransformState.Entry form = TransformState.get(player.getId());
            if (form == null || player.isInvisible()) return;
            long now = player.level().getGameTime();
            float t = (now - form.startTick) + partialTick;
            float dissolve = form.endTick < 0 ? 1.0F
                    : Mth.clamp(1.0F - ((now - form.endTick) + partialTick) / TransformState.DISSOLVE_TICKS, 0.0F, 1.0F);
            if (dissolve <= 0.0F) return;

            boolean jug = form.juggernaut();
            boolean red = "boosted_gear".equals(form.gear);
            GearStyles.Style style = jug ? GearStyles.juggernaut(red) : GearStyles.of(form.gear);
            String base = jug ? "armor/juggernaut_" + (red ? "red" : "white") : "armor/bb_" + form.gear;
            ResourceLocation armorTex = tex(base), glowTex = tex(base + "_glow");
            HumanoidModel<LivingEntity> shell = jug ? jd : bb;
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
            copyPose(getParentModel(), shell);

            // Playing the Blockbench transformation scene: every piece follows its animation bone.
            com.dxdcraft.client.anim.BedrockAnimation anim = com.dxdcraft.client.anim.TransformScenes.playing(form, partialTick);
            if (anim != null) {
                float st = com.dxdcraft.client.anim.TransformScenes.time(form, partialTick);
                float alpha = com.dxdcraft.client.anim.TransformAnimations.scalar(anim, "fx_alpha", st, 1.0F) * dissolve;
                float glowLevel = com.dxdcraft.client.anim.TransformAnimations.scalar(anim, "fx_glow", st, 1.0F);
                String[] bones = {"armor_head", "armor_body", "armor_right_arm", "armor_left_arm", "armor_right_leg", "armor_left_leg"};
                for (int i = 0; i < bones.length; i++) {
                    copyPose(getParentModel(), shell);
                    ModelPart part = new ModelPart[]{shell.head, shell.body, shell.rightArm, shell.leftArm, shell.rightLeg, shell.leftLeg}[i];
                    shell.setAllVisible(false);
                    part.visible = true;
                    com.dxdcraft.client.anim.TransformAnimations.apply(anim, bones[i], st, part);
                    if (Math.abs(part.xScale) < 0.01F && Math.abs(part.yScale) < 0.01F) {
                        scale(part, 1.0F);
                        continue;
                    }
                    shell.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(armorTex)), light, overlay, argb(alpha, 0xFFFFFF));
                    shell.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(glowTex)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                            glow(alpha * glowLevel, 0xFFFFFF));
                    scale(part, 1.0F);
                }
                shell.setAllVisible(true);
                renderExtras(pose, buffers, light, overlay, player, style, alpha, ageInTicks, jug, anim, st);
                return;
            }

            // No scene (disabled, or joined mid-way): shell pieces assemble in order: legs, body, arms, head (then the extras).
            ModelPart[][] groups = {{shell.rightLeg, shell.leftLeg}, {shell.body}, {shell.rightArm, shell.leftArm}, {shell.head}};
            for (int i = 0; i < groups.length; i++) {
                float q = Mth.clamp((t - i * WINDOW) / WINDOW, 0.0F, 1.0F) * dissolve;
                if (q <= 0.0F) continue;
                shell.setAllVisible(false);
                for (ModelPart p : groups[i]) {
                    p.visible = true;
                    scale(p, 1.0F + 0.45F * (1.0F - q));
                }
                shell.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(armorTex)), light, overlay, argb(q, 0xFFFFFF));
                // Glow: jewels and lines, plus a bright flash while the piece is forming.
                float flash = q < 1.0F ? 1.0F : 0.85F + 0.15F * Mth.sin(ageInTicks * 0.15F);
                shell.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(glowTex)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        glow(q * flash, 0xFFFFFF));
                for (ModelPart p : groups[i]) scale(p, 1.0F);
            }
            shell.setAllVisible(true);

            float qx = Mth.clamp((t - 4 * WINDOW) / WINDOW, 0.0F, 1.0F) * dissolve;
            if (qx > 0.0F) renderExtras(pose, buffers, light, overlay, player, style, qx, ageInTicks, jug, null, 0.0F);
        }

        private void renderExtras(PoseStack pose, MultiBufferSource buffers, int light, int overlay, AbstractClientPlayer player,
                                  GearStyles.Style s, float q, float age, boolean jug,
                                  com.dxdcraft.client.anim.BedrockAnimation anim, float st) {
            GearModels.ExtrasModel m = extras;
            copyPose(getParentModel(), m);
            float grow = 0.6F + 0.4F * q;
            float size = jug ? 1.15F : 1.0F;

            // Wings: spread open as they form, flap faster in flight.
            boolean flying = player.getAbilities().flying || !player.onGround();
            float flap = Mth.sin(age * (flying ? 0.45F : 0.12F)) * (flying ? 0.35F : 0.08F);
            float spread = (0.25F + 0.5F * q) * (flying ? 1.0F : 0.75F);
            m.wingL.yRot = -spread;
            m.wingR.yRot = spread;
            m.wingL.zRot = -0.6F + flap;   // raised up and out
            m.wingR.zRot = 0.6F - flap;
            m.wingL.xRot = m.wingR.xRot = 0.15F;
            m.tail.yRot = Mth.sin(age * 0.08F) * 0.25F;
            m.halo.yRot = age * 0.05F;
            for (ModelPart p : new ModelPart[]{m.hornL, m.hornR, m.crest, m.halo, m.core, m.wingL, m.wingR, m.tail, m.pauldronL, m.pauldronR, m.clawsL, m.clawsR}) {
                scale(p, grow * size);
            }

            if (anim != null) {
                if (anim.bone("wing_left") != null || anim.bone("wing_right") != null) {
                    // the scene drives the wings fully, starting from folded against the back
                    // (wing_left rotation y -60 / wing_right +60 opens them; z raises: left negative, right positive)
                    m.wingL.yRot = -1.35F; m.wingR.yRot = 1.35F; m.wingL.zRot = -0.15F; m.wingR.zRot = 0.15F;
                    scale(m.wingL, size); scale(m.wingR, size);
                }
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "wing_left", st, m.wingL);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "wing_right", st, m.wingR);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "horns", st, m.hornL);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "horns", st, m.hornR);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "horn_left", st, m.hornL);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "horn_right", st, m.hornR);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "crest", st, m.crest);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "halo", st, m.halo);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "tail", st, m.tail);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "pauldrons", st, m.pauldronL);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "pauldrons", st, m.pauldronR);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "pauldron_left", st, m.pauldronL);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "pauldron_right", st, m.pauldronR);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "claws", st, m.clawsL);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "claws", st, m.clawsR);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "claws_left", st, m.clawsL);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "claws_right", st, m.clawsR);
                com.dxdcraft.client.anim.TransformAnimations.apply(anim, "core", st, m.core);
            }

            ResourceLocation tex = tex("armor/gear_extras");
            // primary-tinted metal
            m.hideAllExtras();
            m.pauldronL.visible = m.pauldronR.visible = s.pauldrons();
            m.tail.visible = s.tail();
            m.core.visible = true;
            if (s.wings() == 1) m.wingL.visible = m.wingR.visible = true;
            m.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(tex)), light, overlay, argb(q, s.primary()));
            // secondary-tinted metal
            m.hideAllExtras();
            m.hornL.visible = m.hornR.visible = s.horns();
            m.crest.visible = s.crest();
            m.clawsL.visible = m.clawsR.visible = s.claws();
            m.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(tex)), light, overlay, argb(q, s.secondary()));
            // glowing parts: energy wings, halo, the back core's light
            m.hideAllExtras();
            m.halo.visible = s.halo();
            if (s.wings() == 2) m.wingL.visible = m.wingR.visible = true;
            m.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucentEmissive(tex)), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, argb(0.8F * q, s.glow()));
            m.hideAllExtras();
            m.core.visible = true;
            m.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(tex("armor/gear_extras_glow"))), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, glow(q, s.glow()));
            for (ModelPart p : new ModelPart[]{m.hornL, m.hornR, m.crest, m.halo, m.core, m.wingL, m.wingR, m.tail, m.pauldronL, m.pauldronR, m.clawsL, m.clawsR}) {
                scale(p, 1.0F);
                p.visible = true;
            }
        }
    }

    // ------------------------------------------------------------------ worn gears (Curios)

    static final class WornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final GearModels.WornModel model;

        WornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, GearModels.WornModel model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (player.isInvisible() || !CuriosCompat.isLoaded()) return;
            TransformState.Entry form = TransformState.get(player.getId());
            if (form != null && form.endTick < 0) return; // covered by the armor
            List<ItemStack> worn = new ArrayList<>();
            for (ItemStack s : CuriosCompat.equipped(player)) if (s.getItem() instanceof SacredGearItem) worn.add(s);
            if (worn.isEmpty()) return;

            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
            ResourceLocation tex = tex("worn/worn_gear");
            int floating = 0;
            for (ItemStack stack : worn) {
                String gear = GearGrowth.id(stack.getItem());
                GearStyles.Style s = GearStyles.of(gear);
                boolean awakened = GearGrowth.stackStage(stack) >= GearGrowth.BALANCE_BREAKER;
                if (s.worn() == GearStyles.Worn.FLOATING) {
                    renderFloating(pose, buffers, player, stack, floating++, ageInTicks);
                    continue;
                }
                copyPose(getParentModel(), model);
                ModelPart[] metal, gems;
                switch (s.worn()) {
                    case GAUNTLET -> { metal = new ModelPart[]{model.gauntlet}; gems = new ModelPart[]{model.gauntletGem}; }
                    case BRACER -> { metal = new ModelPart[]{model.bracer}; gems = new ModelPart[]{model.bracerGem}; }
                    case BELT -> { metal = new ModelPart[]{model.belt}; gems = new ModelPart[]{model.beltGem}; }
                    case PENDANT -> { metal = new ModelPart[]{model.pendant}; gems = new ModelPart[]{model.pendantGem}; }
                    case RINGS -> { metal = new ModelPart[]{model.ringL, model.ringR}; gems = new ModelPart[0]; }
                    case FOLDED_WINGS -> { metal = new ModelPart[0]; gems = new ModelPart[]{model.foldedL, model.foldedR}; }
                    default -> { metal = new ModelPart[0]; gems = new ModelPart[0]; }
                }
                if (metal.length > 0) {
                    show(metal);
                    model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay, 0xFF000000 | s.primary());
                }
                if (gems.length > 0) {
                    show(gems);
                    float pulse = (awakened ? 0.85F : 0.6F) + 0.15F * Mth.sin(ageInTicks * 0.12F);
                    RenderType type = s.worn() == GearStyles.Worn.FOLDED_WINGS ? RenderType.entityTranslucentEmissive(tex) : RenderType.eyes(tex);
                    int color = s.worn() == GearStyles.Worn.FOLDED_WINGS ? argb(0.75F, s.glow()) : glow(pulse, s.glow());
                    model.renderToBuffer(pose, buffers.getBuffer(type), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
                }
            }
            for (ModelPart p : model.all()) p.visible = true;
        }

        private void show(ModelPart[] parts) {
            for (ModelPart p : model.all()) p.visible = false;
            for (ModelPart p : parts) p.visible = true;
        }

        /** Charm-type gears hover over the shoulders, slowly turning. */
        private void renderFloating(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer player, ItemStack stack, int index, float age) {
            float side = index % 2 == 0 ? -1.0F : 1.0F; // right shoulder first, then left
            float bob = Mth.sin(age * 0.1F + index) * 0.05F;
            pose.pushPose();
            pose.translate(side * 0.62F, -0.28F + bob - (index / 2) * 0.35F, 0.15F);
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            pose.mulPose(Axis.YP.rotationDegrees(age * 3.0F));
            pose.scale(0.42F, 0.42F, 0.42F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, player.level(), player.getId() + index);
            pose.popPose();
        }
    }
}
