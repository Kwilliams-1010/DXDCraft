package com.dxdcraft.client.anim;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.client.GearStyles;
import com.dxdcraft.client.TransformState;
import com.mojang.math.Axis;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Plays the Balance Breaker / Juggernaut Drive transformation scenes from the Blockbench animations:
 * player pose and lift, timeline sounds and particles (for everyone who can see the player), and for the
 * transforming player a sweeping camera, letterbox bars, a flash, and locked movement.
 */
public final class TransformScenes {
    public static final ResourceLocation LAYER = DxDCraft.id("transform_scene");
    private static final Vector3f V = new Vector3f();
    private static float sceneBodyYaw;
    private static int localSceneEntity = Integer.MIN_VALUE;
    private static float flash;

    private TransformScenes() {}

    // ------------------------------------------------------------------ queries

    /** The animation currently playing for this form, or null when the scene is over / disabled. */
    public static BedrockAnimation playing(TransformState.Entry e, float partialTick) {
        if (e == null || !e.animated || e.endTick >= 0) return null;
        BedrockAnimation anim = TransformAnimations.get(e.animationName());
        if (anim == null) return null;
        return time(e, partialTick) < anim.length() ? anim : null;
    }

    public static float time(TransformState.Entry e, float partialTick) {
        var level = Minecraft.getInstance().level;
        long now = level == null ? e.startTick : level.getGameTime();
        return Math.max(0.0F, (now - e.startTick + partialTick) / 20.0F);
    }

    private static TransformState.Entry localEntry() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? null : TransformState.get(p.getId());
    }

    /** True while the local player is in their own transformation scene. */
    public static boolean localInScene() {
        return playing(localEntry(), 0.0F) != null;
    }

    // ------------------------------------------------------------------ pose (called from the model mixin)

    public static void poseModel(HumanoidModel<?> model, LivingEntity entity, float ageInTicks) {
        if (!(model instanceof PlayerModel<?>) || !(entity instanceof AbstractClientPlayer)) return;
        TransformState.Entry e = TransformState.get(entity.getId());
        float partial = ageInTicks - entity.tickCount;
        BedrockAnimation anim = playing(e, partial);
        if (anim == null) return;
        float t = time(e, partial);
        TransformAnimations.apply(anim, "player_head", t, model.head);
        TransformAnimations.apply(anim, "player_body", t, model.body);
        TransformAnimations.apply(anim, "player_right_arm", t, model.rightArm);
        TransformAnimations.apply(anim, "player_left_arm", t, model.leftArm);
        TransformAnimations.apply(anim, "player_right_leg", t, model.rightLeg);
        TransformAnimations.apply(anim, "player_left_leg", t, model.leftLeg);
        model.hat.copyFrom(model.head);
    }

    // ------------------------------------------------------------------ camera (called from the camera mixin)

    /** Camera placement for the local player's scene: {x, y, z, yaw, pitch}, or null for the normal camera. */
    public static double[] camera(Entity viewer, float partialTick) {
        if (viewer != Minecraft.getInstance().player) return null;
        TransformState.Entry e = TransformState.get(viewer.getId());
        BedrockAnimation anim = playing(e, partialTick);
        if (anim == null) return null;
        float t = time(e, partialTick), p = Mth.clamp(t / anim.length(), 0.0F, 1.0F);
        float ease = p * p * (3 - 2 * p);
        TransformAnimations.position(anim, "player_root", t, V);
        Vec3 feet = viewer.getPosition(partialTick);
        Vec3 target = feet.add(0, 1.15 + V.y / 16.0, 0);
        // Sweep from front-left to front-right of the player, starting low and rising to eye level, pushing in mid-scene.
        float angle = sceneBodyYaw + Mth.lerp(ease, -55.0F, 50.0F);
        double dist = 4.3 - 1.0 * Mth.sin(p * Mth.PI);
        double height = 0.35 + 1.35 * ease + V.y / 16.0;
        double rad = angle * Mth.DEG_TO_RAD;
        Vec3 wanted = new Vec3(feet.x - Math.sin(rad) * dist, feet.y + height, feet.z + Math.cos(rad) * dist);
        var hit = viewer.level().clip(new ClipContext(target, wanted, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer));
        Vec3 cam = hit.getType() == HitResult.Type.MISS ? wanted : hit.getLocation().add(target.subtract(hit.getLocation()).normalize().scale(0.3));
        double dx = target.x - cam.x, dy = target.y - cam.y, dz = target.z - cam.z;
        float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        return new double[]{cam.x, cam.y, cam.z, yaw, pitch};
    }

    // ------------------------------------------------------------------ timeline effects

    private static void runTimeline(ClientLevel level) {
        for (Map.Entry<Integer, TransformState.Entry> me : TransformState.all().entrySet()) {
            TransformState.Entry e = me.getValue();
            BedrockAnimation anim = playing(e, 0.0F);
            if (anim == null) continue;
            Entity entity = level.getEntity(me.getKey());
            if (!(entity instanceof AbstractClientPlayer player)) continue;
            float t = time(e, 0.0F);
            for (BedrockAnimation.Event ev : anim.sounds()) {
                if (ev.time() > e.fxDoneUntil && ev.time() <= t) playSound(level, player, ev.effect());
            }
            for (BedrockAnimation.Event ev : anim.particles()) {
                if (ev.time() > e.fxDoneUntil && ev.time() <= t) particles(level, player, e, ev.effect());
            }
            e.fxDoneUntil = t;
        }
    }

    /** "namespace:sound.id|pitch|volume" (pitch and volume optional). */
    private static void playSound(ClientLevel level, Entity at, String spec) {
        String[] parts = spec.split("\\|");
        ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
        if (id == null) return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(SoundEvent.createVariableRangeEvent(id));
        float pitch = parts.length > 1 ? parse(parts[1], 1.0F) : 1.0F;
        float volume = parts.length > 2 ? parse(parts[2], 1.0F) : 1.0F;
        level.playLocalSound(at.getX(), at.getY() + 1, at.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false);
    }

    private static float parse(String s, float fallback) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** "shape" or "shape:particle". Shapes: implode, spiral, burst, ring, shockwave, pillar, sparks, rain, flash, lock_head/body/arms/legs. */
    private static void particles(ClientLevel level, AbstractClientPlayer p, TransformState.Entry e, String spec) {
        String[] parts = spec.split(":");
        String shape = parts[0];
        int glow = e.juggernaut() ? GearStyles.juggernaut("boosted_gear".equals(e.gear)).glow() : GearStyles.of(e.gear).glow();
        ParticleOptions type = particleType(parts.length > 1 ? parts[1] : "dust", glow);
        RandomSource r = level.random;
        double cx = p.getX(), cy = p.getY() + 1.0, cz = p.getZ();
        float yaw = p.yBodyRot * Mth.DEG_TO_RAD;
        switch (shape) {
            case "implode" -> {
                for (int i = 0; i < 36; i++) {
                    Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.7, r.nextGaussian()).normalize().scale(2.6 + r.nextDouble());
                    level.addParticle(type, cx + d.x, cy + d.y, cz + d.z, -d.x * 0.11, -d.y * 0.11, -d.z * 0.11);
                }
            }
            case "spiral" -> {
                for (int i = 0; i < 48; i++) {
                    double a = i * 0.65 + (i % 3) * 2.094, h = i / 48.0 * 2.4, rr = 1.1 - h * 0.25;
                    level.addParticle(type, cx + Math.cos(a) * rr, p.getY() + h, cz + Math.sin(a) * rr, 0, 0.04, 0);
                }
            }
            case "burst" -> {
                for (int i = 0; i < 45; i++) {
                    Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().scale(0.25 + r.nextDouble() * 0.3);
                    level.addParticle(type, cx, cy, cz, d.x, d.y, d.z);
                }
            }
            case "ring", "shockwave" -> {
                boolean big = shape.equals("shockwave");
                for (int i = 0; i < (big ? 60 : 36); i++) {
                    double a = i * Mth.TWO_PI / (big ? 60 : 36);
                    double s = big ? 0.55 : 0.22;
                    level.addParticle(type, cx + Math.cos(a) * 0.6, p.getY() + 0.1, cz + Math.sin(a) * 0.6, Math.cos(a) * s, 0.01, Math.sin(a) * s);
                }
                if (big) level.addParticle(ParticleTypes.EXPLOSION, cx, p.getY() + 0.5, cz, 0, 0, 0);
            }
            case "pillar" -> {
                for (int i = 0; i < 35; i++) {
                    level.addParticle(type, cx + r.nextGaussian() * 0.25, p.getY() + r.nextDouble() * 7, cz + r.nextGaussian() * 0.25, 0, 0.15, 0);
                }
            }
            case "sparks" -> {
                for (int i = 0; i < 16; i++) {
                    level.addParticle(type, cx + r.nextGaussian() * 0.5, cy + r.nextGaussian() * 0.7, cz + r.nextGaussian() * 0.5,
                            r.nextGaussian() * 0.1, r.nextGaussian() * 0.1, r.nextGaussian() * 0.1);
                }
            }
            case "rain" -> {
                for (int i = 0; i < 24; i++) {
                    level.addParticle(type, cx + r.nextGaussian() * 1.6, p.getY() + 3 + r.nextDouble() * 2, cz + r.nextGaussian() * 1.6, 0, -0.08, 0);
                }
            }
            case "flash" -> {
                for (int i = 0; i < 30; i++) {
                    Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().scale(0.45);
                    level.addParticle(type, cx, cy, cz, d.x, d.y, d.z);
                }
                if (p == Minecraft.getInstance().player) flash = 1.0F;
            }
            default -> {
                if (!shape.startsWith("lock_")) return;
                // A small flare where an armor piece locks into place.
                double ox = 0, oy = 0;
                switch (shape) {
                    case "lock_head" -> oy = 0.75;
                    case "lock_body" -> oy = 0.1;
                    case "lock_arms" -> { ox = 0.38; oy = 0.15; }
                    case "lock_legs" -> oy = -0.6;
                }
                for (int side = -1; side <= 1; side += 2) {
                    if (ox == 0 && side == 1) continue;
                    double x = cx + Math.cos(yaw) * ox * side, z = cz + Math.sin(yaw) * ox * side;
                    for (int i = 0; i < 8; i++) {
                        level.addParticle(type, x, cy + oy, z, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08);
                    }
                }
            }
        }
    }

    private static ParticleOptions particleType(String name, int rgb) {
        return switch (name) {
            case "flame" -> ParticleTypes.FLAME;
            case "soul" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "witch" -> ParticleTypes.WITCH;
            case "ink" -> ParticleTypes.SQUID_INK;
            case "smoke" -> ParticleTypes.LARGE_SMOKE;
            case "cloud" -> ParticleTypes.CLOUD;
            case "glow" -> ParticleTypes.GLOW;
            case "snow" -> ParticleTypes.SNOWFLAKE;
            case "cherry" -> ParticleTypes.CHERRY_LEAVES;
            case "enchant" -> ParticleTypes.ENCHANT;
            case "crit" -> ParticleTypes.CRIT;
            case "portal" -> ParticleTypes.PORTAL;
            default -> new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), 0.9F);
        };
    }

    // ------------------------------------------------------------------ events

    private static final Set<Integer> PUSHED = new HashSet<>();

    @EventBusSubscriber(modid = DxDCraft.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            runTimeline(mc.level);
            flash = Math.max(0.0F, flash - 0.25F);
            TransformState.Entry local = localEntry();
            boolean inScene = playing(local, 0.0F) != null;
            if (inScene && localSceneEntity != mc.player.getId()) {
                localSceneEntity = mc.player.getId();
                sceneBodyYaw = mc.player.yBodyRot;
            } else if (!inScene) {
                localSceneEntity = Integer.MIN_VALUE;
            }
        }

        @SubscribeEvent
        public static void onInput(MovementInputUpdateEvent event) {
            if (!localInScene()) return;
            var in = event.getInput();
            in.forwardImpulse = 0;
            in.leftImpulse = 0;
            in.up = in.down = in.left = in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
        }

        @SubscribeEvent
        public static void onHand(RenderHandEvent event) {
            if (localInScene()) event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
            if (localInScene() && !LAYER.equals(event.getName())) event.setCanceled(true);
        }

        /** player_root: lift / sway the whole player. */
        @SubscribeEvent
        public static void onRenderPre(RenderPlayerEvent.Pre event) {
            TransformState.Entry e = TransformState.get(event.getEntity().getId());
            BedrockAnimation anim = playing(e, event.getPartialTick());
            if (anim == null || anim.bone("player_root") == null) return;
            float t = time(e, event.getPartialTick());
            var pose = event.getPoseStack();
            pose.pushPose();
            PUSHED.add(event.getEntity().getId());
            TransformAnimations.position(anim, "player_root", t, V);
            float yaw = event.getEntity().yBodyRot * Mth.DEG_TO_RAD;
            // Blockbench axes relative to the player: +x = the player's right, +z = behind them.
            double wx = -V.x * Math.cos(yaw) + V.z * Math.sin(yaw), wz = -V.x * Math.sin(yaw) - V.z * Math.cos(yaw);
            pose.translate(wx / 16.0, V.y / 16.0, wz / 16.0);
            TransformAnimations.rotation(anim, "player_root", t, V);
            if (V.y != 0) pose.mulPose(Axis.YP.rotationDegrees(-V.y));
        }

        @SubscribeEvent
        public static void onRenderPost(RenderPlayerEvent.Post event) {
            if (PUSHED.remove(event.getEntity().getId())) event.getPoseStack().popPose();
        }
    }

    @EventBusSubscriber(modid = DxDCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(LAYER, TransformScenes::renderOverlay);
        }
    }

    /** Letterbox bars for the transforming player, plus the white flash. */
    private static void renderOverlay(GuiGraphics g, DeltaTracker delta) {
        TransformState.Entry e = localEntry();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        BedrockAnimation anim = playing(e, partial);
        int w = g.guiWidth(), h = g.guiHeight();
        if (anim != null) {
            float t = time(e, partial), len = anim.length();
            float k = Math.min(Mth.clamp(t / 0.35F, 0, 1), Mth.clamp((len - t) / 0.35F, 0, 1));
            int bar = (int) (h * 0.11F * k);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
        if (flash > 0.01F) g.fill(0, 0, w, h, ((int) (flash * 140) << 24) | 0xFFFFFF);
    }
}
