package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.entity.HeavenlyDragon;
import com.dxdcraft.entity.RogueFallenAngel;
import com.dxdcraft.entity.StrayDevil;
import com.dxdcraft.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = DxDCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(DragonBossModel.LAYER, DragonBossModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LIGHT_SPEAR.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.25F, true));
        event.registerEntityRenderer(ModEntities.DESTRUCTION_ORB.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.5F, true));
        event.registerEntityRenderer(ModEntities.DRAGON_SHOT.get(), ctx -> new ThrownItemRenderer<>(ctx, 2.0F, true));
        event.registerEntityRenderer(ModEntities.HOLY_WATER.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.STRAY_DEVIL.get(), StrayDevilRenderer::new);
        event.registerEntityRenderer(ModEntities.ROGUE_FALLEN_ANGEL.get(), FallenAngelRenderer::new);
        event.registerEntityRenderer(ModEntities.HEAVENLY_DRAGON.get(), DragonRenderer::new);
        event.registerEntityRenderer(ModEntities.SERAPH_GUARDIAN.get(), ctx -> new Humanoid<>(ctx, "seraph_guardian", 1.6F));
        event.registerEntityRenderer(ModEntities.UNDERWORLD_OVERLORD.get(), ctx -> new Humanoid<>(ctx, "underworld_overlord", 1.85F));
        event.registerEntityRenderer(ModEntities.WANDERING_EXORCIST.get(), ctx -> new Humanoid<>(ctx, "wandering_exorcist", 1.0F));
        event.registerEntityRenderer(ModEntities.ANGEL_MERCHANT.get(), ctx -> new Humanoid<>(ctx, "angel_merchant", 1.0F));
        event.registerEntityRenderer(ModEntities.DEVIL_MERCHANT.get(), ctx -> new Humanoid<>(ctx, "devil_merchant", 1.0F));
    }

    public static class StrayDevilRenderer extends HumanoidMobRenderer<StrayDevil, HumanoidModel<StrayDevil>> {
        private static final ResourceLocation TEXTURE = DxDCraft.id("textures/entity/stray_devil.png");

        public StrayDevilRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(StrayDevil entity) {
            return TEXTURE;
        }
    }

    public static class FallenAngelRenderer extends HumanoidMobRenderer<RogueFallenAngel, HumanoidModel<RogueFallenAngel>> {
        private static final ResourceLocation TEXTURE = DxDCraft.id("textures/entity/rogue_fallen_angel.png");

        public FallenAngelRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        }

        @Override
        public ResourceLocation getTextureLocation(RogueFallenAngel entity) {
            return TEXTURE;
        }
    }

    public static class DragonRenderer extends MobRenderer<HeavenlyDragon, DragonBossModel> {
        private static final ResourceLocation TEXTURE = DxDCraft.id("textures/entity/heavenly_dragon.png");

        public DragonRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DragonBossModel(ctx.bakeLayer(DragonBossModel.LAYER)), 2.0F);
        }

        @Override
        protected void scale(HeavenlyDragon entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(2.0F, 2.0F, 2.0F);
        }

        @Override
        public ResourceLocation getTextureLocation(HeavenlyDragon entity) {
            return TEXTURE;
        }
    }

    /** Generic scaled humanoid renderer for the new bosses and NPCs. */
    public static class Humanoid<T extends net.minecraft.world.entity.Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
        private final ResourceLocation texture;
        private final float scale;

        public Humanoid(EntityRendererProvider.Context ctx, String name, float scale) {
            super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F * scale);
            this.texture = DxDCraft.id("textures/entity/" + name + ".png");
            this.scale = scale;
        }

        @Override
        protected void scale(T entity, PoseStack poseStack, float partialTick) {
            poseStack.scale(scale, scale, scale);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }
    }
}
