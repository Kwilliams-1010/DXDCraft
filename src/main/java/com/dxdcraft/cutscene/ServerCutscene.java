package com.dxdcraft.cutscene;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.data.DxDData;
import com.dxdcraft.logic.Destiny;
import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.logic.GearGrowth;
import com.dxdcraft.network.CutscenePayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Server side of the awakening cutscene: freezes and protects the player, plays the gear's
 * unique particle/sound set-piece on a fixed timeline, and (for innate awakenings) grants the
 * gear at the end. The client shows letterbox, dialogue and the title card.
 */
public final class ServerCutscene {
    private static final ResourceLocation FREEZE_SPEED = DxDCraft.id("cutscene_freeze_speed");
    private static final ResourceLocation FREEZE_JUMP = DxDCraft.id("cutscene_freeze_jump");

    private ServerCutscene() {}

    public static boolean active(ServerPlayer p) {
        return DxDLogic.data(p).inCutscene();
    }

    /** @param awaken true when this is an innate gear awakening (the gear is granted at the end). */
    public static void start(ServerPlayer p, String gearId, boolean awaken) {
        if (CutsceneScripts.get(gearId) == null) {
            if (awaken) Destiny.finishAwaken(p, DxDLogic.data(p));
            return;
        }
        DxDData d = DxDLogic.data(p);
        if (d.inCutscene()) return;
        d.beginCutscene(gearId, awaken);
        d.markCutsceneSeen(gearId);
        freeze(p, true);
        calmMobs(p);
        PacketDistributor.sendToPlayer(p, new CutscenePayload(gearId, true));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    public static void tick(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        if (!d.inCutscene()) return;
        int t = d.getCutsceneTick();
        String gear = d.getCutsceneGear();

        boolean skip = t > CutsceneScripts.SKIPPABLE_AFTER && p.isShiftKeyDown();
        if (t >= CutsceneScripts.DURATION || skip || !p.isAlive()) {
            finish(p, d);
            return;
        }
        p.setDeltaMovement(Vec3.ZERO);
        if (t % 20 == 0) calmMobs(p);

        ServerLevel level = p.serverLevel();
        if (t >= 20 && t < CutsceneScripts.CLIMAX) buildUp(level, p, gear, t);
        else if (t == CutsceneScripts.CLIMAX) climax(level, p, gear);
        else if (t > CutsceneScripts.CLIMAX && t < 185) aftermath(level, p, gear, t);

        d.setCutsceneTick(t + 1);
    }

    public static void finish(ServerPlayer p, DxDData d) {
        boolean awaken = d.isCutsceneAwaken();
        String gear = d.getCutsceneGear();
        d.endCutscene();
        freeze(p, false);
        PacketDistributor.sendToPlayer(p, new CutscenePayload(gear, false));
        if (awaken) Destiny.finishAwaken(p, d);
        else GearGrowth.startAnim(p, gear, GearGrowth.ANIM_SUMMON);
    }

    /** Called on logout/death so nobody stays frozen. */
    public static void abort(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        if (!d.inCutscene()) return;
        d.endCutscene();
        freeze(p, false);
    }

    private static void freeze(ServerPlayer p, boolean on) {
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = p.getAttribute(Attributes.JUMP_STRENGTH);
        if (on) {
            if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(FREEZE_SPEED, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (jump != null) jump.addOrUpdateTransientModifier(new AttributeModifier(FREEZE_JUMP, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            if (speed != null) speed.removeModifier(FREEZE_SPEED);
            if (jump != null) jump.removeModifier(FREEZE_JUMP);
        }
    }

    private static void calmMobs(ServerPlayer p) {
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24.0))) {
            if (m.getTarget() == p) m.setTarget(null);
        }
    }

    // ------------------------------------------------------------------ particle helpers

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), size);
    }

    private static void ring(ServerLevel l, double x, double y, double z, double r, ParticleOptions p, int n, double spin) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + spin;
            l.sendParticles(p, x + Math.cos(a) * r, y, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private static void column(ServerLevel l, double x, double y, double z, double h, ParticleOptions p) {
        for (double dy = 0; dy < h; dy += 0.25) l.sendParticles(p, x, y + dy, z, 1, 0.02, 0, 0.02, 0);
    }

    private static void line(ServerLevel l, Vec3 a, Vec3 b, ParticleOptions p) {
        int steps = (int) (a.distanceTo(b) * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 v = a.lerp(b, i / (double) Math.max(1, steps));
            l.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static void sound(ServerLevel l, ServerPlayer p, SoundEvent s, float vol, float pitch) {
        l.playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, vol, pitch);
    }

    private static void visualBolt(ServerLevel l, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
        if (bolt == null) return;
        bolt.moveTo(x, y, z);
        bolt.setVisualOnly(true);
        l.addFreshEntity(bolt);
    }

    // ------------------------------------------------------------------ the set-pieces

    private static void buildUp(ServerLevel l, ServerPlayer p, String gear, int t) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double prog = (t - 20) / 100.0; // 0..1
        int color = CutsceneScripts.get(gear).color();
        DustParticleOptions d = dust(color, 1.5F);
        // shared: the ground circle draws itself in
        ring(l, x, y + 0.05, z, 2.0, d, (int) (8 + 24 * prog), t * 0.03);

        switch (gear) {
            case "boosted_gear" -> {
                for (int arm = 0; arm < 2; arm++) {
                    double a = t * 0.3 + arm * Math.PI;
                    l.sendParticles(ParticleTypes.FLAME, x + Math.cos(a) * 1.2, y + prog * 2.5, z + Math.sin(a) * 1.2, 2, 0.05, 0.05, 0.05, 0.01);
                }
                if (t % 20 == 0) sound(l, p, SoundEvents.WARDEN_HEARTBEAT, 1.5F, 0.8F + (float) prog * 0.6F);
                if (t > 80 && t % 10 == 0) column(l, x + (l.random.nextDouble() - 0.5) * 4, y, z + (l.random.nextDouble() - 0.5) * 4, 3, ParticleTypes.FLAME);
            }
            case "divine_dividing" -> {
                // two wing arcs unfolding behind the player
                double yaw = Math.toRadians(p.getYRot());
                Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
                Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
                for (int s = -1; s <= 1; s += 2) {
                    for (int i = 0; i < 6; i++) {
                        double f = i / 5.0 * prog;
                        Vec3 pt = p.position().add(back.scale(0.5)).add(side.scale(s * (0.4 + f * 2.2))).add(0, 1.4 + Math.sin(f * Math.PI) * 0.9, 0);
                        l.sendParticles(d, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                    }
                }
                if (t % 7 == 0) l.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1, z, 8, 1.2, 1.0, 1.2, 0.1);
                if (t % 25 == 0) sound(l, p, SoundEvents.PHANTOM_FLAP, 1.2F, 0.6F);
            }
            case "twilight_healing" -> {
                ring(l, x, y + 1.0, z, 0.6, dust(0xFFE38C, 0.8F), 10, t * 0.2);
                ring(l, x, y + 1.0, z, 0.9, d, 12, -t * 0.15);
                if (t % 6 == 0) l.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1, z, 6, 2.0, 1.0, 2.0, 0);
                if (t % 15 == 0) sound(l, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F + (float) prog);
            }
            case "sword_birth" -> {
                if (t % 8 == 0) {
                    double a = l.random.nextDouble() * Math.PI * 2;
                    double r = 1.5 + l.random.nextDouble() * 2;
                    column(l, x + Math.cos(a) * r, y, z + Math.sin(a) * r, 1 + prog * 2.5, d);
                    sound(l, p, SoundEvents.ANVIL_PLACE, 0.4F, 1.6F);
                }
                if (t % 4 == 0) l.sendParticles(ParticleTypes.CRIT, x, y + 1, z, 6, 1.5, 0.8, 1.5, 0.3);
            }
            case "absorption_line" -> {
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2 + t * 0.05;
                    double reach = 1.0 + 3.5 * Math.abs(Math.sin(t * 0.08));
                    Vec3 from = p.position().add(0, 1.0, 0);
                    line(l, from, from.add(Math.cos(a) * reach, -0.6, Math.sin(a) * reach), d);
                }
                if (t % 20 == 0) sound(l, p, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F, 0.6F);
            }
            case "annihilation_maker" -> {
                if (t % 3 == 0) l.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 6, 2.5, 0.1, 2.5, 0.01);
                if (t % 10 == 0) {
                    double a = l.random.nextDouble() * Math.PI * 2;
                    column(l, x + Math.cos(a) * 2.5, y, z + Math.sin(a) * 2.5, 1.5 + prog * 1.5, ParticleTypes.SQUID_INK);
                }
                if (t % 30 == 0) sound(l, p, SoundEvents.WARDEN_EMERGE, 0.6F, 1.4F);
            }
            case "dimension_lost" -> {
                l.sendParticles(ParticleTypes.CLOUD, x, y + 0.5, z, (int) (4 + 10 * prog), 2.5 * prog + 0.5, 0.6, 2.5 * prog + 0.5, 0.005);
                if (t % 20 == 0) sound(l, p, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.7F);
            }
            case "regulus_nemea" -> {
                // a golden mane flaring outward around the head
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6 + t * 0.02;
                    double r = 0.4 + prog * 0.9;
                    l.sendParticles(d, x + Math.cos(a) * r, y + 1.7 + Math.sin(a * 2) * 0.2, z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
                if (t % 30 == 0) sound(l, p, SoundEvents.RAVAGER_ROAR, 0.6F, 1.0F + (float) prog * 0.3F);
            }
            case "zenith_tempest" -> {
                l.sendParticles(ParticleTypes.CLOUD, x, y + 4.5, z, 6, 3.0, 0.3, 3.0, 0.01);
                l.sendParticles(ParticleTypes.SPLASH, x, y + 0.2, z, 10, 3.0, 0.1, 3.0, 0.1);
                if (t % 25 == 0 && t > 40) {
                    double a = l.random.nextDouble() * Math.PI * 2;
                    visualBolt(l, x + Math.cos(a) * 6, y, z + Math.sin(a) * 6);
                }
            }
            case "forbidden_balor_view" -> {
                ring(l, x, y + 1.6, z, 1.2, d, 16, 0); // a still, unmoving ring: time stops
                if (t % 5 == 0) l.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.6, z, 10, 1.5, 1.0, 1.5, 0.02);
                if (t % 20 == 0) sound(l, p, SoundEvents.BELL_RESONATE, 0.8F, 1.6F - (float) prog * 0.8F);
            }
            default -> {}
        }
    }

    private static void climax(ServerLevel l, ServerPlayer p, String gear) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        DustParticleOptions d = dust(CutsceneScripts.get(gear).color(), 2.0F);
        l.sendParticles(d, x, y + 1, z, 200, 1.5, 1.5, 1.5, 0.3);
        l.sendParticles(ParticleTypes.FLASH, x, y + 1, z, 1, 0, 0, 0, 0);
        for (double r = 1; r <= 5; r += 1) ring(l, x, y + 0.1, z, r, d, (int) (r * 10), 0);
        sound(l, p, SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        switch (gear) {
            case "boosted_gear" -> {
                l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1, z, 1, 0, 0, 0, 0);
                l.sendParticles(ParticleTypes.FLAME, x, y + 1, z, 150, 2, 1, 2, 0.2);
                sound(l, p, SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.0F);
            }
            case "divine_dividing" -> {
                l.sendParticles(ParticleTypes.END_ROD, x, y + 1.5, z, 120, 2.5, 1.0, 2.5, 0.15);
                sound(l, p, SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.5F);
            }
            case "twilight_healing" -> {
                l.sendParticles(ParticleTypes.HEART, x, y + 2, z, 30, 2, 1, 2, 0);
                sound(l, p, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.4F);
            }
            case "sword_birth" -> {
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    column(l, x + Math.cos(a) * 3, y, z + Math.sin(a) * 3, 3.5, d);
                }
                sound(l, p, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
            }
            case "absorption_line" -> {
                l.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1, z, 80, 3, 1, 3, 0);
                sound(l, p, SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.6F);
            }
            case "annihilation_maker" -> {
                l.sendParticles(ParticleTypes.SCULK_SOUL, x, y + 1, z, 60, 3, 1, 3, 0.05);
                sound(l, p, SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.6F);
            }
            case "dimension_lost" -> {
                l.sendParticles(ParticleTypes.CLOUD, x, y + 1, z, 300, 5, 1.5, 5, 0.05);
                sound(l, p, SoundEvents.ENDERMAN_TELEPORT, 1.5F, 0.5F);
            }
            case "regulus_nemea" -> {
                l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.5, z, 150, 1, 1, 1, 0.6);
                sound(l, p, SoundEvents.RAVAGER_ROAR, 2.0F, 0.7F);
            }
            case "zenith_tempest" -> {
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    visualBolt(l, x + Math.cos(a) * 4, y, z + Math.sin(a) * 4);
                }
            }
            case "forbidden_balor_view" -> {
                l.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1, z, 200, 3, 2, 3, 0.1);
                sound(l, p, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.5F);
            }
            default -> {}
        }
    }

    private static void aftermath(ServerLevel l, ServerPlayer p, String gear, int t) {
        if (t % 4 != 0) return;
        DustParticleOptions d = dust(CutsceneScripts.get(gear).color(), 1.0F);
        double a = t * 0.2;
        l.sendParticles(d, p.getX() + Math.cos(a) * 0.8, p.getY() + 1.0, p.getZ() + Math.sin(a) * 0.8, 3, 0.05, 0.3, 0.05, 0);
    }
}
