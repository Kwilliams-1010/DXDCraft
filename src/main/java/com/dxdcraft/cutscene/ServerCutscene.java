package com.dxdcraft.cutscene;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.data.DxDData;
import com.dxdcraft.logic.Destiny;
import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.logic.GearGrowth;
import com.dxdcraft.network.CutscenePayload;
import com.dxdcraft.network.FxPayload;
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
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new FxPayload(p.getId(), gearId, FxPayload.FX_CUTSCENE, d.getGearStage(gearId)));
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

        d.setCutsceneTick(t + 1);
    }

    public static void finish(ServerPlayer p, DxDData d) {
        boolean awaken = d.isCutsceneAwaken();
        String gear = d.getCutsceneGear();
        d.endCutscene();
        freeze(p, false);
        PacketDistributor.sendToPlayer(p, new CutscenePayload(gear, false));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new FxPayload(p.getId(), gear, FxPayload.FX_STOP, 0));
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

    // ------------------------------------------------------------------ server half of the set-pieces
    // Particles are drawn client-side by ClientFx (one FxPayload to all trackers at start).
    // The server only plays the sounds on cue and spawns the harmless lightning for Zenith Tempest.

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

    private static void buildUp(ServerLevel l, ServerPlayer p, String gear, int t) {
        float prog = (t - 20) / 100F;
        switch (gear) {
            case "boosted_gear" -> { if (t % 20 == 0) sound(l, p, SoundEvents.WARDEN_HEARTBEAT, 1.5F, 0.8F + prog * 0.6F); }
            case "divine_dividing" -> { if (t % 25 == 0) sound(l, p, SoundEvents.PHANTOM_FLAP, 1.2F, 0.6F); }
            case "twilight_healing" -> { if (t % 15 == 0) sound(l, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F + prog); }
            case "sword_birth" -> { if (t % 8 == 0) sound(l, p, SoundEvents.ANVIL_PLACE, 0.4F, 1.6F); }
            case "absorption_line" -> { if (t % 20 == 0) sound(l, p, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0F, 0.6F); }
            case "annihilation_maker" -> { if (t % 30 == 0) sound(l, p, SoundEvents.WARDEN_EMERGE, 0.6F, 1.4F); }
            case "dimension_lost" -> { if (t % 20 == 0) sound(l, p, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.7F); }
            case "regulus_nemea" -> { if (t % 30 == 0) sound(l, p, SoundEvents.RAVAGER_ROAR, 0.6F, 1.0F + prog * 0.3F); }
            case "zenith_tempest" -> {
                if (t % 25 == 0 && t > 40) {
                    double a = l.random.nextDouble() * Math.PI * 2;
                    visualBolt(l, p.getX() + Math.cos(a) * 6, p.getY(), p.getZ() + Math.sin(a) * 6);
                }
            }
            case "forbidden_balor_view" -> { if (t % 20 == 0) sound(l, p, SoundEvents.BELL_RESONATE, 0.8F, 1.6F - prog * 0.8F); }
            default -> {}
        }
    }

    private static void climax(ServerLevel l, ServerPlayer p, String gear) {
        sound(l, p, SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        switch (gear) {
            case "boosted_gear" -> sound(l, p, SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.0F);
            case "divine_dividing" -> sound(l, p, SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.5F);
            case "twilight_healing" -> sound(l, p, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.4F);
            case "sword_birth" -> sound(l, p, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
            case "absorption_line" -> sound(l, p, SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.6F);
            case "annihilation_maker" -> sound(l, p, SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.6F);
            case "dimension_lost" -> sound(l, p, SoundEvents.ENDERMAN_TELEPORT, 1.5F, 0.5F);
            case "regulus_nemea" -> sound(l, p, SoundEvents.RAVAGER_ROAR, 2.0F, 0.7F);
            case "zenith_tempest" -> {
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    visualBolt(l, p.getX() + Math.cos(a) * 4, p.getY(), p.getZ() + Math.sin(a) * 4);
                }
            }
            case "forbidden_balor_view" -> sound(l, p, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.5F);
            default -> {}
        }
    }
}
