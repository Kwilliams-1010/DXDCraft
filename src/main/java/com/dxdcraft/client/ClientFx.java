package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.cutscene.CutsceneScripts;
import com.dxdcraft.network.FxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side particle effects for Sacred Gears: summon/draw/stage-up animations and the
 * cutscene set-pieces. Triggered by a single {@link FxPayload}; drawn locally by every client
 * that can see the player, so the server sends almost nothing.
 */
@EventBusSubscriber(modid = DxDCraft.MODID, value = Dist.CLIENT)
public final class ClientFx {
    private static final class Fx {
        final int entityId;
        final String gear;
        final int kind;
        final int stage;
        final int duration;
        int t;

        Fx(FxPayload p) {
            this.entityId = p.entityId();
            this.gear = p.gear();
            this.kind = p.kind();
            this.stage = Math.max(1, p.stage());
            this.duration = switch (p.kind()) {
                case FxPayload.FX_SUMMON -> 40;
                case FxPayload.FX_STAGE_UP -> 60;
                case FxPayload.FX_CUTSCENE -> CutsceneScripts.DURATION;
                default -> 16;
            };
        }
    }

    private static final List<Fx> ACTIVE = new ArrayList<>();
    private static final int MAX_ACTIVE = 32; // hard cap so a crowded server can't flood a client

    private ClientFx() {}

    public static void play(FxPayload p) {
        if (p.kind() == FxPayload.FX_STOP) {
            ACTIVE.removeIf(f -> f.entityId == p.entityId() && f.kind == FxPayload.FX_CUTSCENE);
            return;
        }
        if (CutsceneScripts.get(p.gear()) == null) return;
        // one animation per player at a time (a cutscene always wins)
        ACTIVE.removeIf(f -> f.entityId == p.entityId() && (f.kind != FxPayload.FX_CUTSCENE || p.kind() == FxPayload.FX_CUTSCENE));
        if (ACTIVE.size() >= MAX_ACTIVE) ACTIVE.remove(0);
        ACTIVE.add(new Fx(p));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            if (mc.level == null) ACTIVE.clear();
            return;
        }
        Iterator<Fx> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Fx fx = it.next();
            Entity e = mc.level.getEntity(fx.entityId);
            if (e == null || fx.t >= fx.duration) {
                it.remove();
                continue;
            }
            if (fx.kind == FxPayload.FX_CUTSCENE) cutsceneFrame(mc.level, e, fx);
            else animFrame(mc.level, e, fx);
            fx.t++;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), size);
    }

    /** Same spread/speed semantics as ServerLevel#sendParticles with count > 0. */
    private static void emit(ClientLevel l, ParticleOptions p, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        RandomSource r = l.random;
        for (int i = 0; i < count; i++) {
            l.addParticle(p, x + r.nextGaussian() * dx, y + r.nextGaussian() * dy, z + r.nextGaussian() * dz,
                    r.nextGaussian() * speed, r.nextGaussian() * speed, r.nextGaussian() * speed);
        }
    }

    private static void dot(ClientLevel l, ParticleOptions p, double x, double y, double z) {
        l.addParticle(p, x, y, z, 0, 0, 0);
    }

    private static void ring(ClientLevel l, double x, double y, double z, double r, ParticleOptions p, int n, double spin) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + spin;
            dot(l, p, x + Math.cos(a) * r, y, z + Math.sin(a) * r);
        }
    }

    private static void column(ClientLevel l, double x, double y, double z, double h, ParticleOptions p) {
        for (double dy = 0; dy < h; dy += 0.25) emit(l, p, x, y + dy, z, 1, 0.02, 0, 0.02, 0);
    }

    private static void line(ClientLevel l, Vec3 a, Vec3 b, ParticleOptions p) {
        int steps = (int) (a.distanceTo(b) * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 v = a.lerp(b, i / (double) Math.max(1, steps));
            dot(l, p, v.x, v.y, v.z);
        }
    }

    private static int color(String gear) {
        CutsceneScripts.Script s = CutsceneScripts.get(gear);
        return s == null ? 0xFFFFFF : s.color();
    }

    // ------------------------------------------------------------------ summon / draw / stage-up

    private static void animFrame(ClientLevel l, Entity e, Fx fx) {
        int t = fx.t;
        double progress = t / (double) Math.max(1, fx.duration - 1);
        boolean draw = fx.kind == FxPayload.FX_DRAW;
        DustParticleOptions d = dust(color(fx.gear), draw ? 0.9F : 1.4F);
        double x = e.getX(), y = e.getY(), z = e.getZ();

        // magic circle drawing itself in
        double radius = draw ? 0.9 : 1.4 + 0.2 * fx.stage;
        int ringPoints = draw ? 10 : 24;
        for (int i = 0; i < ringPoints; i++) {
            double a = (i / (double) ringPoints) * Math.PI * 2 * Math.min(1.0, progress * 1.5) + t * 0.05;
            dot(l, d, x + Math.cos(a) * radius, y + 0.05, z + Math.sin(a) * radius);
            if (!draw && i % 3 == 0) dot(l, d, x + Math.cos(-a) * radius * 0.6, y + 0.05, z + Math.sin(-a) * radius * 0.6);
        }
        // rising spiral (more arms at Balance Breaker+)
        int arms = fx.stage >= 3 ? 4 : 3;
        double height = progress * (fx.kind == FxPayload.FX_STAGE_UP ? 3.0 : 2.2);
        for (int arm = 0; arm < arms; arm++) {
            double a = t * 0.55 + arm * (Math.PI * 2 / arms);
            double r = 0.8 * (1.0 - progress * 0.5);
            emit(l, d, x + Math.cos(a) * r, y + height, z + Math.sin(a) * r, 2, 0.02, 0.02, 0.02, 0);
        }
        if (!draw && t % 2 == 0) emit(l, ParticleTypes.ENCHANT, x, y + 1.2, z, 6, 1.0, 1.0, 1.0, 0.5);
        // finale
        if (t == fx.duration - 1) {
            int count = draw ? 15 : fx.kind == FxPayload.FX_SUMMON ? 60 : 120;
            emit(l, d, x, y + 1.0, z, count, 0.5, 0.8, 0.5, 0.2);
            emit(l, ParticleTypes.END_ROD, x, y + 1.0, z, count / 3, 0.3, 0.6, 0.3, 0.15);
            if (!draw) dot(l, ParticleTypes.FLASH, x, y + 1.0, z);
        }
    }

    // ------------------------------------------------------------------ cutscene set-pieces

    private static void cutsceneFrame(ClientLevel l, Entity e, Fx fx) {
        int t = fx.t;
        if (t >= 20 && t < CutsceneScripts.CLIMAX) buildUp(l, e, fx.gear, t);
        else if (t == CutsceneScripts.CLIMAX) climax(l, e, fx.gear);
        else if (t > CutsceneScripts.CLIMAX && t < 185 && t % 4 == 0) {
            DustParticleOptions d = dust(color(fx.gear), 1.0F);
            double a = t * 0.2;
            emit(l, d, e.getX() + Math.cos(a) * 0.8, e.getY() + 1.0, e.getZ() + Math.sin(a) * 0.8, 3, 0.05, 0.3, 0.05, 0);
        }
    }

    private static void buildUp(ClientLevel l, Entity e, String gear, int t) {
        double x = e.getX(), y = e.getY(), z = e.getZ();
        double prog = (t - 20) / 100.0;
        DustParticleOptions d = dust(color(gear), 1.5F);
        RandomSource r = l.random;
        ring(l, x, y + 0.05, z, 2.0, d, (int) (8 + 24 * prog), t * 0.03);

        switch (gear) {
            case "boosted_gear" -> {
                for (int arm = 0; arm < 2; arm++) {
                    double a = t * 0.3 + arm * Math.PI;
                    emit(l, ParticleTypes.FLAME, x + Math.cos(a) * 1.2, y + prog * 2.5, z + Math.sin(a) * 1.2, 2, 0.05, 0.05, 0.05, 0.01);
                }
                if (t > 80 && t % 10 == 0) column(l, x + (r.nextDouble() - 0.5) * 4, y, z + (r.nextDouble() - 0.5) * 4, 3, ParticleTypes.FLAME);
            }
            case "divine_dividing" -> {
                double yaw = Math.toRadians(e.getYRot());
                Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
                Vec3 side = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
                for (int s = -1; s <= 1; s += 2) {
                    for (int i = 0; i < 6; i++) {
                        double f = i / 5.0 * prog;
                        Vec3 pt = e.position().add(back.scale(0.5)).add(side.scale(s * (0.4 + f * 2.2))).add(0, 1.4 + Math.sin(f * Math.PI) * 0.9, 0);
                        dot(l, d, pt.x, pt.y, pt.z);
                    }
                }
                if (t % 7 == 0) emit(l, ParticleTypes.ELECTRIC_SPARK, x, y + 1, z, 8, 1.2, 1.0, 1.2, 0.1);
            }
            case "twilight_healing" -> {
                ring(l, x, y + 1.0, z, 0.6, dust(0xFFE38C, 0.8F), 10, t * 0.2);
                ring(l, x, y + 1.0, z, 0.9, d, 12, -t * 0.15);
                if (t % 6 == 0) emit(l, ParticleTypes.HAPPY_VILLAGER, x, y + 1, z, 6, 2.0, 1.0, 2.0, 0);
            }
            case "sword_birth" -> {
                if (t % 8 == 0) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rad = 1.5 + r.nextDouble() * 2;
                    column(l, x + Math.cos(a) * rad, y, z + Math.sin(a) * rad, 1 + prog * 2.5, d);
                }
                if (t % 4 == 0) emit(l, ParticleTypes.CRIT, x, y + 1, z, 6, 1.5, 0.8, 1.5, 0.3);
            }
            case "absorption_line" -> {
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2 + t * 0.05;
                    double reach = 1.0 + 3.5 * Math.abs(Math.sin(t * 0.08));
                    Vec3 from = e.position().add(0, 1.0, 0);
                    line(l, from, from.add(Math.cos(a) * reach, -0.6, Math.sin(a) * reach), d);
                }
            }
            case "annihilation_maker" -> {
                if (t % 3 == 0) emit(l, ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, 6, 2.5, 0.1, 2.5, 0.01);
                if (t % 10 == 0) {
                    double a = r.nextDouble() * Math.PI * 2;
                    column(l, x + Math.cos(a) * 2.5, y, z + Math.sin(a) * 2.5, 1.5 + prog * 1.5, ParticleTypes.SQUID_INK);
                }
            }
            case "dimension_lost" -> emit(l, ParticleTypes.CLOUD, x, y + 0.5, z, (int) (4 + 10 * prog), 2.5 * prog + 0.5, 0.6, 2.5 * prog + 0.5, 0.005);
            case "regulus_nemea" -> {
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6 + t * 0.02;
                    double rad = 0.4 + prog * 0.9;
                    dot(l, d, x + Math.cos(a) * rad, y + 1.7 + Math.sin(a * 2) * 0.2, z + Math.sin(a) * rad);
                }
            }
            case "zenith_tempest" -> {
                emit(l, ParticleTypes.CLOUD, x, y + 4.5, z, 6, 3.0, 0.3, 3.0, 0.01);
                emit(l, ParticleTypes.SPLASH, x, y + 0.2, z, 10, 3.0, 0.1, 3.0, 0.1);
            }
            case "forbidden_balor_view" -> {
                ring(l, x, y + 1.6, z, 1.2, d, 16, 0);
                if (t % 5 == 0) emit(l, ParticleTypes.REVERSE_PORTAL, x, y + 1.6, z, 10, 1.5, 1.0, 1.5, 0.02);
            }
            default -> {}
        }
    }

    private static void climax(ClientLevel l, Entity e, String gear) {
        double x = e.getX(), y = e.getY(), z = e.getZ();
        DustParticleOptions d = dust(color(gear), 2.0F);
        emit(l, d, x, y + 1, z, 200, 1.5, 1.5, 1.5, 0.3);
        dot(l, ParticleTypes.FLASH, x, y + 1, z);
        for (double r = 1; r <= 5; r += 1) ring(l, x, y + 0.1, z, r, d, (int) (r * 10), 0);
        switch (gear) {
            case "boosted_gear" -> {
                dot(l, ParticleTypes.EXPLOSION_EMITTER, x, y + 1, z);
                emit(l, ParticleTypes.FLAME, x, y + 1, z, 150, 2, 1, 2, 0.2);
            }
            case "divine_dividing" -> emit(l, ParticleTypes.END_ROD, x, y + 1.5, z, 120, 2.5, 1.0, 2.5, 0.15);
            case "twilight_healing" -> emit(l, ParticleTypes.HEART, x, y + 2, z, 30, 2, 1, 2, 0);
            case "sword_birth" -> {
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    column(l, x + Math.cos(a) * 3, y, z + Math.sin(a) * 3, 3.5, d);
                }
            }
            case "absorption_line" -> emit(l, ParticleTypes.HAPPY_VILLAGER, x, y + 1, z, 80, 3, 1, 3, 0);
            case "annihilation_maker" -> emit(l, ParticleTypes.SCULK_SOUL, x, y + 1, z, 60, 3, 1, 3, 0.05);
            case "dimension_lost" -> emit(l, ParticleTypes.CLOUD, x, y + 1, z, 300, 5, 1.5, 5, 0.05);
            case "regulus_nemea" -> emit(l, ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.5, z, 150, 1, 1, 1, 0.6);
            case "forbidden_balor_view" -> emit(l, ParticleTypes.REVERSE_PORTAL, x, y + 1, z, 200, 3, 2, 3, 0.1);
            default -> {}
        }
    }
}
