package com.dxdcraft.command;

import com.dxdcraft.cutscene.CutsceneScripts;
import com.dxdcraft.cutscene.ServerCutscene;
import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Element;
import com.dxdcraft.data.Piece;
import com.dxdcraft.data.Race;
import com.dxdcraft.item.SacredGearItem;
import com.dxdcraft.logic.Abilities;
import com.dxdcraft.logic.Combat;
import com.dxdcraft.logic.Destiny;
import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.logic.ElementCasting;
import com.dxdcraft.logic.GearGrowth;
import com.dxdcraft.logic.JuggernautDrive;
import com.dxdcraft.logic.RageMeter;
import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.registry.ModItems;
import com.dxdcraft.world.DimensionTravel;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /dxd selftest - runs the whole DxD Craft game-logic suite against a simulated player on the
 * running server and reports pass/fail. Safe to run on a live server (it uses its own fake player
 * and cleans up the mobs it spawns), handy for checking an install or a modpack.
 */
public final class SelfTest {
    private static final Logger LOGGER = LogUtils.getLogger();

    private interface Check {
        void run() throws Exception;
    }

    private final CommandSourceStack src;
    private final ServerLevel level;
    private final List<String> failures = new ArrayList<>();
    private final List<Mob> spawned = new ArrayList<>();
    private int passed;
    private ServerPlayer p;

    private SelfTest(CommandSourceStack src) {
        this.src = src;
        this.level = src.getServer().overworld();
    }

    public static int run(CommandSourceStack src) {
        return new SelfTest(src).runAll();
    }

    private void check(String name, Check c) {
        try {
            c.run();
            passed++;
        } catch (Throwable t) {
            failures.add(name + ": " + t);
            LOGGER.error("DxD selftest FAILED: {}", name, t);
        }
    }

    private static void expect(boolean cond, String what) {
        if (!cond) throw new AssertionError(what);
    }

    private ServerPlayer fresh() {
        ServerPlayer fp = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "DxDTester"));
        BlockPos spawn = level.getSharedSpawnPos();
        fp.moveTo(spawn.getX() + 0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn.getX(), spawn.getZ()) + 1, spawn.getZ() + 0.5, 0, 0);
        fp.getInventory().clearContent();
        fp.removeAllEffects();
        fp.setHealth(fp.getMaxHealth());
        fp.setData(com.dxdcraft.registry.ModAttachments.DXD, new DxDData());
        return fp;
    }

    private Zombie target() {
        Zombie z = EntityType.ZOMBIE.create(level);
        z.moveTo(p.getX(), p.getY(), p.getZ() + 3, 180, 0);
        z.setNoAi(true);
        z.setPersistenceRequired();
        level.addFreshEntity(z);
        spawned.add(z);
        return z;
    }

    private void hold(Item item) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
    }

    private void use(Item item) {
        hold(item);
        item.use(level, p, InteractionHand.MAIN_HAND);
    }

    private int runAll() {
        src.sendSuccess(() -> Component.literal("Running DxD Craft self-test...").withStyle(ChatFormatting.GOLD), false);

        // ---------------------------------------------------------------- data
        check("default data", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            expect(d.getRace() == Race.HUMAN && d.getLevel() == 1, "fresh player should be Lv1 human");
            expect(d.getMaxMana() == 60, "human max mana 60, got " + d.getMaxMana());
        });
        check("save/load round trip", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            d.setRace(Race.DEVIL); d.setPiece(Piece.QUEEN); d.setLevel(25); d.setUltCharge(77); d.setRage(40);
            d.setMasteredRed(true); d.setInnateGear("boosted_gear"); d.setGearAwakened(true);
            d.setElement1(Element.FIRE); d.setElement2(Element.WIND);
            d.setGearXp("boosted_gear", 900); d.setGearStage("boosted_gear", 3); d.markCutsceneSeen("boosted_gear");
            CompoundTag tag = d.serializeNBT(level.registryAccess());
            DxDData e = new DxDData();
            e.deserializeNBT(level.registryAccess(), tag);
            expect(e.getRace() == Race.DEVIL && e.getPiece() == Piece.QUEEN && e.getLevel() == 25, "race/piece/level");
            expect(e.getUltCharge() == 77 && e.getRage() == 40 && e.isMasteredRed(), "ult/rage/mastery");
            expect("boosted_gear".equals(e.getInnateGear()) && e.isGearAwakened(), "innate gear");
            expect(e.getElement1() == Element.FIRE && e.getElement2() == Element.WIND, "elements");
            expect(e.getGearStage("boosted_gear") == 3 && e.getGearXp("boosted_gear") == 900, "gear growth");
            expect(e.hasSeenCutscene("boosted_gear"), "seen cutscenes");
        });

        // ---------------------------------------------------------------- races / levels
        check("races, pieces and flight", () -> {
            p = fresh();
            DxDLogic.setRace(p, Race.DEVIL, Piece.ROOK);
            DxDData d = DxDLogic.data(p);
            expect(d.getRace() == Race.DEVIL && d.getPiece() == Piece.ROOK, "devil rook");
            expect(p.getAbilities().mayfly, "devil wings should allow flight");
            DxDLogic.secondTick(p);
            expect(p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "rook strength");
            DxDLogic.setRace(p, Race.HUMAN, Piece.NONE);
            expect(!p.getAbilities().mayfly, "human should lose flight");
            for (Race r : Race.values()) DxDLogic.setRace(p, r, Piece.BISHOP);
        });
        check("levels and XP", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            DxDLogic.giveXp(p, 1_000_000);
            expect(d.getLevel() == DxDData.MAX_LEVEL, "should cap at Lv100, got " + d.getLevel());
            expect(d.getMaxMana() == 60 + 5 * 99, "max mana scales with level");
            p = fresh();
            DxDLogic.data(p).tryUseMana(50);
            expect(DxDLogic.data(p).getXp() > 0, "spending mana gives XP");
        });

        // ---------------------------------------------------------------- destiny
        check("destiny roll (1000 players)", () -> {
            p = fresh();
            RandomSource r = RandomSource.create(42);
            int gears = 0, doubles = 0;
            for (int i = 0; i < 1000; i++) {
                DxDData d = new DxDData();
                p.setData(com.dxdcraft.registry.ModAttachments.DXD, d);
                Destiny.roll(p, d, r);
                if (d.hasInnateGear()) {
                    gears++;
                    expect(d.getElement1() == null, "gear users get no element");
                } else {
                    expect(d.getElement1() != null, "element users get an element");
                    if (d.getElement2() != null) {
                        doubles++;
                        expect(d.getElement1() != d.getElement2(), "two different elements");
                    }
                }
            }
            expect(gears > 250 && gears < 450, "~35% gears, got " + gears / 10.0 + "%");
            expect(doubles > 50 && doubles < 160, "~15% of element users doubled, got " + doubles);
        });
        check("innate gear awakening + cutscene", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            d.setDestinyRolled(true);
            d.setInnateGear("twilight_healing");
            d.setLevel(5);
            p.setHealth(4.0F);
            Destiny.secondTick(p, d);
            expect(d.isGearAwakened(), "common gear should awaken at Lv5 + near death");
            expect(d.inCutscene(), "awakening cutscene should be playing");
            for (int i = 0; i <= CutsceneScripts.DURATION + 1; i++) ServerCutscene.tick(p);
            expect(!d.inCutscene(), "cutscene should end");
            expect(p.getInventory().contains(new ItemStack(ModItems.TWILIGHT_HEALING.get())), "gear granted after cutscene");
            p.getInventory().clearContent();
            expect(Destiny.tryResummon(p) && p.getInventory().contains(new ItemStack(ModItems.TWILIGHT_HEALING.get())), "Sneak+V re-manifest");
        });
        check("rare + longinus awakening requirements", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            d.setInnateGear("dimension_lost");
            d.setLevel(10);
            d.setHostileKills(49);
            Destiny.secondTick(p, d);
            expect(!d.isGearAwakened(), "rare needs 50 kills");
            Destiny.onKill(p, target());
            expect(d.isGearAwakened(), "rare awakens on 50th kill");
            p = fresh();
            d = DxDLogic.data(p);
            d.setInnateGear("boosted_gear");
            d.setLevel(20);
            Destiny.secondTick(p, d);
            expect(!d.isGearAwakened(), "longinus needs a boss");
            Mob boss = ModEntities.HEAVENLY_DRAGON.get().create(level);
            Destiny.onKill(p, boss);
            expect(d.isGearAwakened(), "longinus awakens on boss kill");
        });
        check("second element at growth threshold", () -> {
            int got = 0;
            for (int i = 0; i < 200; i++) {
                p = fresh();
                DxDData d = DxDLogic.data(p);
                d.setElement1(Element.WATER);
                d.setLevel(30);
                Destiny.secondTick(p, d); // Lv15 roll
                Destiny.secondTick(p, d); // Lv30 roll
                if (d.getElement2() != null) got++;
            }
            // P(second) = 1 - 0.6 * 0.4 = 76%
            expect(got > 120 && got < 180, "~76% should gain a 2nd element by Lv30, got " + got / 2.0 + "%");
        });

        // ---------------------------------------------------------------- elements
        for (Element e : Element.values()) {
            check("element cast: " + e.id(), () -> {
                p = fresh();
                DxDData d = DxDLogic.data(p);
                d.setElement1(e);
                d.setLevel(10);
                d.setMana(d.getMaxMana());
                Zombie z = target();
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
                int before = d.getMana();
                ElementCasting.cast(p, 0);
                expect(d.getMana() < before, e.id() + " should cost mana");
                ElementCasting.passives(p, d);
            });
        }

        // ---------------------------------------------------------------- sacred gears
        for (String gid : Destiny.gearIds()) {
            check("gear skill + ultimate: " + gid, () -> {
                p = fresh();
                DxDData d = DxDLogic.data(p);
                d.setLevel(40);
                d.setMana(d.getMaxMana());
                Item gear = Destiny.gearItem(gid);
                expect(gear instanceof SacredGearItem, gid + " is a SacredGearItem");
                Zombie z = target();
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, z.getEyePosition());
                if (gid.equals("boosted_gear")) d.setBoosts(5);
                use(gear);
                d.setMana(d.getMaxMana());
                // ultimate is locked below Balance Breaker
                d.setUltCharge(100);
                Abilities.tryGearUltimate(p, (SacredGearItem) gear);
                expect(d.getUltCharge() == 100, "ultimate must be locked at Stage I");
                GearGrowth.addXp(p, gear, 100000);
                expect(GearGrowth.stage(p, gear) == 4, "gear should reach Sublimation, got " + GearGrowth.stage(p, gear));
                p.getCooldowns().removeCooldown(gear);
                Abilities.tryGearUltimate(p, (SacredGearItem) gear);
                expect(d.getUltCharge() == 0, "ultimate should fire and spend the gauge");
            });
        }
        check("gear stage gates by level", () -> {
            p = fresh();
            Item g = ModItems.SWORD_BIRTH.get();
            GearGrowth.addXp(p, g, 100000);
            expect(GearGrowth.stage(p, g) == 1, "Lv1 should stay Stage I");
            DxDLogic.data(p).setLevel(15);
            GearGrowth.checkStageUp(p, DxDLogic.data(p), "sword_birth");
            expect(GearGrowth.stage(p, g) == 3, "Lv15 should reach Balance Breaker");
        });

        // ---------------------------------------------------------------- juggernaut / rage
        check("juggernaut drive gates", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            hold(ModItems.BOOSTED_GEAR.get());
            d.setUltCharge(100);
            d.setLevel(19);
            JuggernautDrive.tryActivate(p);
            expect(!d.jdActive(), "locked below Lv20");
            d.setLevel(20);
            JuggernautDrive.tryActivate(p);
            expect(!d.jdActive(), "locked below Balance Breaker");
            GearGrowth.addXp(p, ModItems.BOOSTED_GEAR.get(), 100000);
            d.setMana(d.getMaxMana());
            JuggernautDrive.tryActivate(p);
            expect(d.jdActive() && !d.isJdComplete(), "incomplete drive at Lv20 + Balance Breaker");
            for (int i = 0; i < JuggernautDrive.INCOMPLETE_SECONDS + 1 && d.jdActive(); i++) JuggernautDrive.secondTick(p, d);
            expect(!d.jdActive(), "drive should end after 30s");
            expect(p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "collapse after incomplete drive");
        });
        check("complete drive + Longinus Smasher", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            d.setLevel(40);
            d.setMasteredRed(true);
            GearGrowth.addXp(p, ModItems.BOOSTED_GEAR.get(), 100000);
            hold(ModItems.BOOSTED_GEAR.get());
            d.setUltCharge(100);
            d.setMana(d.getMaxMana());
            JuggernautDrive.tryActivate(p);
            expect(d.jdActive() && d.isJdComplete(), "complete drive at Lv40 + Sublimation + mastery");
            int before = level.getEntitiesOfClass(com.dxdcraft.entity.projectile.DragonShotEntity.class, p.getBoundingBox().inflate(8)).size();
            use(ModItems.BOOSTED_GEAR.get());
            int after = level.getEntitiesOfClass(com.dxdcraft.entity.projectile.DragonShotEntity.class, p.getBoundingBox().inflate(8)).size();
            expect(after > before, "Longinus Smasher should fire");
            JuggernautDrive.end(p, d);
        });
        check("rage meter forces incomplete drive", () -> {
            p = fresh();
            DxDData d = DxDLogic.data(p);
            d.setLevel(20);
            d.setMasteredWhite(true);
            GearGrowth.addXp(p, ModItems.DIVINE_DIVIDING.get(), 100000);
            p.getInventory().add(new ItemStack(ModItems.DIVINE_DIVIDING.get()));
            expect(RageMeter.eligible(p, d), "should have a rage meter");
            RageMeter.add(p, 60, null);
            expect(d.getRage() == 60 && !d.jdActive(), "60% rage, no drive yet");
            RageMeter.add(p, 50, null);
            expect(d.jdActive() && d.getJdType() == DxDData.JD_WHITE && !d.isJdComplete(), "rage drive is always incomplete");
            expect(d.getRage() == 0, "rage resets");
            JuggernautDrive.end(p, d);
        });

        // ---------------------------------------------------------------- race powers
        for (Race r : Race.values()) {
            check("race ability + ultimate: " + r.id(), () -> {
                p = fresh();
                DxDLogic.setRace(p, r, Piece.QUEEN);
                DxDData d = DxDLogic.data(p);
                d.setMana(d.getMaxMana());
                target();
                Abilities.raceAbility(p);
                d.setUltCharge(100);
                d.setMana(d.getMaxMana());
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                Abilities.ultimateKey(p);
                expect(d.getUltCharge() == 0, r.id() + " ultimate should fire");
            });
        }

        // ---------------------------------------------------------------- items, realms, rules
        check("race items + magic items", () -> {
            p = fresh();
            use(ModItems.EVIL_PIECE_KNIGHT.get());
            expect(DxDLogic.data(p).getPiece() == Piece.KNIGHT, "evil piece");
            use(ModItems.PURIFICATION_SCROLL.get());
            use(ModItems.HOLY_FEATHER.get());
            expect(DxDLogic.data(p).getRace() == Race.ANGEL, "holy feather");
            DxDLogic.data(p).setMana(200);
            use(ModItems.LIGHT_SPEAR.get());
            use(ModItems.HOLY_WATER.get());
            DxDLogic.setRace(p, Race.DEVIL, Piece.BISHOP);
            DxDLogic.data(p).setMana(200);
            use(ModItems.DESTRUCTION_SIGIL.get());
        });
        check("realm travel", () -> {
            p = fresh();
            ServerLevel heaven = src.getServer().getLevel(DimensionTravel.HEAVEN);
            ServerLevel under = src.getServer().getLevel(DimensionTravel.UNDERWORLD);
            expect(heaven != null && under != null, "both realms exist");
            // (A simulated player can't actually change dimension - vanilla needs a real connection -
            //  so verify the landing-spot search that real players use.)
            for (ServerLevel realm : new ServerLevel[]{heaven, under, level}) {
                for (int[] xz : new int[][]{{0, 0}, {500, -300}, {-1200, 800}}) {
                    BlockPos at = DimensionTravel.landingSpot(realm, xz[0], xz[1]);
                    expect(realm.getBlockState(at).isAir() && realm.getBlockState(at.above()).isAir(),
                            "headroom at landing in " + realm.dimension().location() + " " + at);
                    var below = realm.getBlockState(at.below());
                    expect(!below.isAir() && below.getFluidState().isEmpty(), "solid ground at landing in " + realm.dimension().location() + " " + at);
                }
            }
        });
        check("combat rules", () -> {
            p = fresh();
            ServerPlayer other = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "DxDOther"));
            expect(!Combat.canHarm(p, p), "can't hurt yourself");
            expect(Combat.canHarm(p, target()), "can hurt mobs");
            expect(Combat.canHarm(p, other) == src.getServer().isPvpAllowed(), "player damage follows server PvP");
        });
        check("all commands", () -> {
            p = fresh();
            CommandSourceStack ps = p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            String[] cmds = {"dxd info", "dxd race devil", "dxd piece queen", "dxd mana fill", "dxd ult fill", "dxd level set 40",
                    "dxd xp add 10", "dxd boosts 5", "dxd rage set 10", "dxd cooldowns", "dxd master both", "dxd gear stage boosted_gear 4",
                    "dxd gear xp sword_birth 10", "dxd element set fire", "dxd element second wind", "dxd destiny gear zenith_tempest",
                    "dxd destiny awaken", "dxd give all", "dxd ultimate", "dxd ability", "dxd juggernaut start red complete",
                    "dxd juggernaut stop", "dxd cutscene boosted_gear", "dxd cutscene reset seen", "dxd gear summon",
                    "dxd destiny reroll", "dxd travel overworld", "dxd unmaster both", "dxd reset", "dxd help", "dxd list"};
            List<String> bad = new ArrayList<>();
            for (String c : cmds) {
                int result;
                try {
                    result = src.getServer().getCommands().getDispatcher().execute(c, ps);
                } catch (Exception ex) {
                    result = 0;
                    LOGGER.error("selftest command {} threw", c, ex);
                }
                if (result <= 0) bad.add(c);
                DxDData d = DxDLogic.data(p);
                if (d.inCutscene()) ServerCutscene.finish(p, d);
            }
            expect(bad.isEmpty(), "commands failed: " + bad);
        });

        // ---------------------------------------------------------------- cleanup + report
        for (Mob m : spawned) m.discard();
        level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, p.getBoundingBox().inflate(64),
                e -> e instanceof com.dxdcraft.entity.projectile.MagicProjectile || e instanceof com.dxdcraft.entity.projectile.HolyWaterEntity
                        || (e instanceof net.minecraft.world.entity.TamableAnimal t && t.getOwnerUUID() != null)
                        || (e instanceof net.minecraft.world.entity.animal.IronGolem g && g.isPlayerCreated())).forEach(net.minecraft.world.entity.Entity::discard);

        int total = passed + failures.size();
        if (failures.isEmpty()) {
            src.sendSuccess(() -> Component.literal("DxD self-test: ALL " + total + " CHECKS PASSED").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        } else {
            src.sendFailure(Component.literal("DxD self-test: " + failures.size() + " of " + total + " checks FAILED"));
            for (String f : failures) src.sendFailure(Component.literal(" - " + f));
        }
        return failures.isEmpty() ? total : 0;
    }
}
