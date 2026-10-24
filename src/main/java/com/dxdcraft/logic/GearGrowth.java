package com.dxdcraft.logic;

import com.dxdcraft.compat.CuriosCompat;
import com.dxdcraft.data.DxDData;
import com.dxdcraft.item.SacredGearItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sacred Gears grow with their wielder.
 *
 * Stage I   Awakened         (start)
 * Stage II  Enhanced         150 gear XP + DxD Lv 5
 * Stage III Balance Breaker  500 gear XP + DxD Lv 15   -> unlocks the gear's ULTIMATE (and Incomplete Juggernaut Drive)
 * Stage IV  Sublimation      1500 gear XP + DxD Lv 30  -> needed for the COMPLETE Juggernaut Drive
 * (Rare gears need 1.25x the XP, Longinus gears 1.5x.)
 *
 * Gear XP: +5 per successful active skill, +1 per melee hit with the gear (held or worn),
 * +25 per ultimate, +1 per second of Juggernaut Drive.
 *
 * Each stage above I makes the gear's skills hit 25% harder and cost 10% less mana.
 * Growth is stored on the player (per gear), so a lost or re-summoned gear keeps its stage.
 */
public final class GearGrowth {
    public static final String[] STAGE_NAMES = {"", "Awakened", "Enhanced", "Balance Breaker", "Sublimation"};
    public static final String[] ROMAN = {"", "I", "II", "III", "IV"};
    private static final int[] BASE_XP = {0, 0, 150, 500, 1500};
    private static final int[] LEVEL_REQ = {0, 0, 5, 15, 30};
    public static final int BALANCE_BREAKER = 3;
    public static final int SUBLIMATION = 4;

    // animation kinds
    public static final int ANIM_DRAW = 0, ANIM_SUMMON = 1, ANIM_STAGE_UP = 2;

    private GearGrowth() {}

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static double tierMultiplier(String gearId) {
        return switch (Destiny.tierOf(gearId)) {
            case COMMON -> 1.0;
            case RARE -> 1.25;
            case LONGINUS -> 1.5;
        };
    }

    public static int xpForStage(String gearId, int stage) {
        if (stage <= 1 || stage > 4) return 0;
        return (int) Math.round(BASE_XP[stage] * tierMultiplier(gearId));
    }

    public static int levelForStage(int stage) {
        return stage <= 1 || stage > 4 ? 0 : LEVEL_REQ[stage];
    }

    public static float power(int stage) {
        return 1.0F + 0.25F * (Math.max(1, stage) - 1);
    }

    public static float manaDiscount(int stage) {
        return 1.0F - 0.10F * (Math.max(1, stage) - 1);
    }

    public static int stage(Player p, Item gear) {
        return DxDLogic.data(p).getGearStage(id(gear));
    }

    public static String label(int stage) {
        return "Stage " + ROMAN[stage] + ": " + STAGE_NAMES[stage];
    }

    // ------------------------------------------------------------------ growth

    public static void addXp(ServerPlayer p, Item gear, int amount) {
        if (!(gear instanceof SacredGearItem) || amount <= 0) return;
        DxDData d = DxDLogic.data(p);
        String gid = id(gear);
        d.setGearXp(gid, d.getGearXp(gid) + amount);
        checkStageUp(p, d, gid);
    }

    public static void checkStageUp(ServerPlayer p, DxDData d, String gid) {
        int stage = d.getGearStage(gid);
        while (stage < 4 && d.getGearXp(gid) >= xpForStage(gid, stage + 1) && d.getLevel() >= levelForStage(stage + 1)) {
            stage++;
            d.setGearStage(gid, stage);
            announce(p, gid, stage);
        }
    }

    private static void announce(ServerPlayer p, String gid, int stage) {
        String name = Destiny.gearName(gid);
        ChatFormatting color = stage >= SUBLIMATION ? ChatFormatting.LIGHT_PURPLE : stage >= BALANCE_BREAKER ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        Abilities.title(p, stage == BALANCE_BREAKER ? "BALANCE BREAKER!" : stage == SUBLIMATION ? "SUBLIMATION!" : "GEAR EVOLVED",
                color, name + " - " + label(stage));
        String extra = switch (stage) {
            case BALANCE_BREAKER -> " Its ULTIMATE is now unlocked.";
            case SUBLIMATION -> " It has reached its true form.";
            default -> "";
        };
        p.sendSystemMessage(Component.literal("Your " + name + " grew to " + label(stage) + "." + extra).withStyle(color));
        if (stage >= BALANCE_BREAKER) {
            p.server.getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString() + "'s " + name
                    + " reached " + STAGE_NAMES[stage] + "!").withStyle(color, ChatFormatting.BOLD), false);
        }
        startAnim(p, gid, ANIM_STAGE_UP);
    }

    /** Re-checks every gear the player has grown (level-ups can unlock a stage that was waiting on level). */
    public static void secondTick(ServerPlayer p, DxDData d) {
        for (String gid : new ArrayList<>(d.grownGears())) checkStageUp(p, d, gid);
        syncStacks(p, d);

        // Worn gear changed (Curios) -> summon animation
        SacredGearItem worn = CuriosCompat.wornGear(p);
        String wornId = worn == null ? "" : id(worn);
        if (!wornId.equals(d.getLastWornGear())) {
            d.setLastWornGear(wornId);
            if (!wornId.isEmpty()) startAnim(p, wornId, ANIM_DRAW);
        }
    }

    /** Writes stage/xp onto every gear stack the player carries, so tooltips (client side) can show it. */
    private static void syncStacks(ServerPlayer p, DxDData d) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) stacks.add(p.getInventory().getItem(i));
        stacks.addAll(CuriosCompat.equipped(p));
        for (ItemStack s : stacks) {
            if (!(s.getItem() instanceof SacredGearItem)) continue;
            String gid = id(s.getItem());
            int stage = d.getGearStage(gid);
            int xp = d.getGearXp(gid);
            CompoundTag cur = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (cur.getInt("dxd_stage") != stage || cur.getInt("dxd_xp") != xp) {
                CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> {
                    tag.putInt("dxd_stage", stage);
                    tag.putInt("dxd_xp", xp);
                });
                if (stage >= BALANCE_BREAKER) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                else s.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            }
        }
    }

    /** Stage stored on a stack (client-safe, for tooltips). 1 if never synced. */
    public static int stackStage(ItemStack s) {
        int st = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("dxd_stage");
        return st <= 0 ? 1 : st;
    }

    public static int stackXp(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("dxd_xp");
    }

    // ------------------------------------------------------------------ summon animation

    private static final Map<String, Vector3f> COLORS = Map.of(
            "boosted_gear", new Vector3f(0.90F, 0.10F, 0.15F),
            "divine_dividing", new Vector3f(0.80F, 0.92F, 1.00F),
            "twilight_healing", new Vector3f(0.55F, 1.00F, 0.60F),
            "sword_birth", new Vector3f(0.50F, 0.20F, 0.75F),
            "absorption_line", new Vector3f(0.40F, 0.80F, 0.20F),
            "annihilation_maker", new Vector3f(0.18F, 0.12F, 0.25F),
            "dimension_lost", new Vector3f(0.88F, 0.88F, 0.93F),
            "regulus_nemea", new Vector3f(1.00F, 0.80F, 0.20F),
            "zenith_tempest", new Vector3f(0.50F, 0.80F, 1.00F),
            "forbidden_balor_view", new Vector3f(0.62F, 0.30F, 0.90F));

    public static void startAnim(ServerPlayer p, String gearId, int kind) {
        int total = switch (kind) {
            case ANIM_SUMMON -> 40;
            case ANIM_STAGE_UP -> 60;
            default -> 16;
        };
        DxDLogic.data(p).startAnim(gearId, total, kind);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                kind == ANIM_DRAW ? 0.4F : 1.0F, kind == ANIM_DRAW ? 1.8F : 1.2F);
        if (kind == ANIM_DRAW) {
            int stage = DxDLogic.data(p).getGearStage(gearId);
            p.displayClientMessage(Component.literal("✦ " + Destiny.gearName(gearId) + " ✦ ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(Component.literal(label(stage)).withStyle(ChatFormatting.GRAY)), true);
        }
    }

    /** Called every tick. Detects drawing a gear and plays any running animation. */
    public static void tick(ServerPlayer p) {
        DxDData d = DxDLogic.data(p);
        Item main = p.getMainHandItem().getItem();
        String held = main instanceof SacredGearItem ? id(main) : "";
        if (!held.equals(d.getLastHeldGear())) {
            d.setLastHeldGear(held);
            if (!held.isEmpty() && !d.inCutscene()) {
                if (!d.hasSeenCutscene(held) && !p.getAbilities().instabuild) {
                    // First time this player ever draws this gear: its awakening cutscene.
                    com.dxdcraft.cutscene.ServerCutscene.start(p, held, false);
                } else if (d.getAnimTicks() <= 0) {
                    startAnim(p, held, ANIM_DRAW);
                }
            }
        }
        com.dxdcraft.cutscene.ServerCutscene.tick(p);
        if (d.getAnimTicks() > 0) {
            playFrame(p, d);
            d.tickAnim();
        }
    }

    private static void playFrame(ServerPlayer p, DxDData d) {
        ServerLevel level = p.serverLevel();
        int total = d.getAnimTotal();
        int t = total - d.getAnimTicks();          // 0 .. total-1
        double progress = t / (double) Math.max(1, total - 1);
        int kind = d.getAnimKind();
        int stage = d.getGearStage(d.getAnimGear());
        Vector3f color = COLORS.getOrDefault(d.getAnimGear(), new Vector3f(1F, 1F, 1F));
        DustParticleOptions dust = new DustParticleOptions(color, kind == ANIM_DRAW ? 0.9F : 1.4F);
        double x = p.getX(), y = p.getY(), z = p.getZ();

        // 1) magic circle on the ground, drawn in as the animation progresses
        double radius = kind == ANIM_DRAW ? 0.9 : 1.4 + 0.2 * stage;
        int ringPoints = kind == ANIM_DRAW ? 10 : 24;
        for (int i = 0; i < ringPoints; i++) {
            double a = (i / (double) ringPoints) * Math.PI * 2 * Math.min(1.0, progress * 1.5) + t * 0.05;
            level.sendParticles(dust, x + Math.cos(a) * radius, y + 0.05, z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
            if (kind != ANIM_DRAW && i % 3 == 0) {
                level.sendParticles(dust, x + Math.cos(-a) * radius * 0.6, y + 0.05, z + Math.sin(-a) * radius * 0.6, 1, 0, 0, 0, 0);
            }
        }

        // 2) rising spiral around the wielder (3 arms; 4 at Balance Breaker+)
        int arms = stage >= BALANCE_BREAKER ? 4 : 3;
        double height = progress * (kind == ANIM_STAGE_UP ? 3.0 : 2.2);
        for (int arm = 0; arm < arms; arm++) {
            double a = t * 0.55 + arm * (Math.PI * 2 / arms);
            double r = 0.8 * (1.0 - progress * 0.5);
            level.sendParticles(dust, x + Math.cos(a) * r, y + height, z + Math.sin(a) * r, 2, 0.02, 0.02, 0.02, 0);
        }

        // 3) gathering sparks into the hand / body
        if (kind != ANIM_DRAW && t % 2 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 1.2, z, 6, 1.0, 1.0, 1.0, 0.5);
        }

        // 4) finale burst
        if (d.getAnimTicks() == 1) {
            int count = kind == ANIM_DRAW ? 15 : kind == ANIM_SUMMON ? 60 : 120;
            level.sendParticles(dust, x, y + 1.0, z, count, 0.5, 0.8, 0.5, 0.2);
            level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, count / 3, 0.3, 0.6, 0.3, 0.15);
            if (kind != ANIM_DRAW) {
                level.sendParticles(ParticleTypes.FLASH, x, y + 1.0, z, 1, 0, 0, 0, 0);
                level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.8F);
            }
            if (kind == ANIM_STAGE_UP) {
                level.playSound(null, p.blockPosition(), stage >= BALANCE_BREAKER ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 1.2F, 1.1F);
            }
        }
    }
}
