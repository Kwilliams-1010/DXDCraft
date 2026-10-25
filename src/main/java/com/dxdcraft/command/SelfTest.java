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

    /** Counts two ore blocks in the 3x3 chunks around the origin of a realm (generates them if needed). */
    private static int[] countOres(ServerLevel lvl, net.minecraft.world.level.block.Block a, net.minecraft.world.level.block.Block b, int maxY) {
        int[] n = new int[2];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                var chunk = lvl.getChunk(cx, cz);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = lvl.getMinBuildHeight(); y < maxY; y++) {
                    var st = chunk.getBlockState(m.set(cx * 16 + x, y, cz * 16 + z));
                    if (st.is(a)) n[0]++;
                    else if (st.is(b)) n[1]++;
                }
            }
        }
        return n;
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

        // ---------------------------------------------------------------- ores, equipment, bosses, NPCs, achievements
        check("ores generate in both realms", () -> {
            ServerLevel heaven = src.getServer().getLevel(DimensionTravel.HEAVEN);
            ServerLevel under = src.getServer().getLevel(DimensionTravel.UNDERWORLD);
            int[] h = countOres(heaven, com.dxdcraft.registry.ModBlocks.HOLY_SILVER_ORE.get(), com.dxdcraft.registry.ModBlocks.SERAPHITE_ORE.get(), 256);
            int[] u = countOres(under, com.dxdcraft.registry.ModBlocks.DEMONITE_ORE.get(), com.dxdcraft.registry.ModBlocks.HELLFIRE_RUBY_ORE.get(), 128);
            src.sendSuccess(() -> Component.literal("  ore scan (9 chunks): holy silver " + h[0] + ", seraphite " + h[1]
                    + ", demonite " + u[0] + ", hellfire ruby " + u[1]).withStyle(ChatFormatting.GRAY), false);
            expect(h[0] > 0 && u[0] > 0, "common ores should generate");
            expect(h[1] > 0 || u[1] > 0, "rare ores should generate");
        });
        check("tool tiers mine the right ores", () -> {
            var G = com.dxdcraft.registry.ModGear.GearSet.class;
            ItemStack ironLevel = new ItemStack(com.dxdcraft.registry.ModGear.tool(com.dxdcraft.registry.ModGear.GearSet.HOLY_SILVER, "pickaxe"));
            ItemStack diaLevel = new ItemStack(com.dxdcraft.registry.ModGear.tool(com.dxdcraft.registry.ModGear.GearSet.SERAPHITE, "pickaxe"));
            ItemStack nethLevel = new ItemStack(com.dxdcraft.registry.ModGear.tool(com.dxdcraft.registry.ModGear.GearSet.DRAGONITE, "pickaxe"));
            ItemStack stone = new ItemStack(net.minecraft.world.item.Items.STONE_PICKAXE);
            var seraphOre = com.dxdcraft.registry.ModBlocks.SERAPHITE_ORE.get().defaultBlockState();
            var silverOre = com.dxdcraft.registry.ModBlocks.HOLY_SILVER_ORE.get().defaultBlockState();
            expect(stone.isCorrectToolForDrops(silverOre) && !stone.isCorrectToolForDrops(seraphOre), "stone pick: silver yes, seraphite no");
            expect(ironLevel.isCorrectToolForDrops(seraphOre), "holy silver pick mines seraphite");
            expect(diaLevel.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()), "seraphite pick mines obsidian");
            expect(nethLevel.isCorrectToolForDrops(com.dxdcraft.registry.ModBlocks.DRAGONITE_BLOCK.get().defaultBlockState()), "dragonite pick mines dragonite");
        });
        for (var set : com.dxdcraft.registry.ModGear.GearSet.values()) {
            check("full set bonus: " + set.id, () -> {
                p = fresh();
                for (var e : com.dxdcraft.registry.ModGear.ARMOR.get(set).entrySet()) {
                    p.setItemSlot(e.getKey().getSlot(), new ItemStack(e.getValue().get()));
                }
                expect(com.dxdcraft.logic.ArmorSets.fullSet(p) == set, "full set detected");
                com.dxdcraft.logic.ArmorSets.secondTick(p, DxDLogic.data(p));
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
                expect(com.dxdcraft.logic.ArmorSets.fullSet(p) == null, "3 pieces is not a full set");
            });
        }
        check("Blessed Hoe grows crops", () -> {
            p = fresh();
            // Above the treetops: wheat needs light/open sky or it pops off (fresh worlds often spawn in forests).
            BlockPos col = p.blockPosition().offset(6, 0, 0);
            int top = 0;
            for (BlockPos q : BlockPos.betweenClosed(col.offset(-2, 0, -2), col.offset(2, 0, 2))) {
                top = Math.max(top, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, q.getX(), q.getZ()));
            }
            BlockPos base = new BlockPos(col.getX(), top + 3, col.getZ());
            for (BlockPos q : BlockPos.betweenClosed(base.offset(-2, -1, -2), base.offset(2, -1, 2))) level.setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState());
            for (BlockPos q : BlockPos.betweenClosed(base.offset(-2, 0, -2), base.offset(2, 0, 2))) level.setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.WHEAT.defaultBlockState());
            hold(com.dxdcraft.registry.ModGear.BLESSED_HOE.get());
            p.setShiftKeyDown(true);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(base), net.minecraft.core.Direction.UP, base, false);
            p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p, InteractionHand.MAIN_HAND, hit));
            p.setShiftKeyDown(false);
            int grown = 0;
            for (BlockPos q : BlockPos.betweenClosed(base.offset(-2, 0, -2), base.offset(2, 0, 2))) {
                if (level.getBlockState(q).getValue(net.minecraft.world.level.block.CropBlock.AGE) > 0) grown++;
            }
            for (BlockPos q : BlockPos.betweenClosed(base.offset(-2, -1, -2), base.offset(2, 0, 2))) level.setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            expect(grown >= 20, "most of the 25 wheat should grow, got " + grown);
        });
        check("Overlord's Excavator mines 3x3", () -> {
            p = fresh();
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            BlockPos c = p.blockPosition().offset(0, 0, 3);
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-1, 0, 0), c.offset(1, 2, 0))) level.setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            hold(com.dxdcraft.registry.ModGear.OVERLORDS_EXCAVATOR.get());
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, net.minecraft.world.phys.Vec3.atCenterOf(c.above()));
            p.gameMode.destroyBlock(c.above());
            int left = 0;
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-1, 0, 0), c.offset(1, 2, 0))) if (!level.getBlockState(q).isAir()) left++;
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-1, 0, 0), c.offset(1, 2, 0))) level.setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            expect(left == 0, "all 9 blocks should be mined, " + left + " left");
        });
        check("Angelic Ward smites hostiles", () -> {
            p = fresh();
            BlockPos at = p.blockPosition().offset(-6, 0, 0);
            level.setBlockAndUpdate(at, com.dxdcraft.registry.ModBlocks.ANGELIC_WARD.get().defaultBlockState());
            Zombie z = target();
            z.moveTo(at.getX() + 3, at.getY(), at.getZ(), 0, 0);
            float hp = z.getHealth();
            var be = (com.dxdcraft.block.AngelicWardBlockEntity) level.getBlockEntity(at);
            expect(be != null, "ward block entity exists");
            for (int i = 0; i < 20; i++) com.dxdcraft.block.AngelicWardBlockEntity.serverTick(level, at, level.getBlockState(at), be);
            level.setBlockAndUpdate(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            expect(z.getHealth() < hp, "zombie should be damaged by the ward");
        });
        for (var bossType : List.of(ModEntities.SERAPH_GUARDIAN, ModEntities.UNDERWORLD_OVERLORD, ModEntities.HEAVENLY_DRAGON)) {
            check("boss fights: " + bossType.getId().getPath(), () -> {
                p = fresh();
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                Mob boss = (Mob) bossType.get().create(level);
                boss.moveTo(p.getX() + 6, p.getY() + 1, p.getZ(), 0, 0);
                level.addFreshEntity(boss);
                spawned.add(boss);
                boss.setTarget(p);
                for (int i = 0; i < 400; i++) {
                    boss.tick();
                    if (boss.getTarget() == null) boss.setTarget(p);
                }
                int projectiles = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, boss.getBoundingBox().inflate(48)).size();
                expect(boss.isAlive(), "boss survives its own attacks");
                expect(projectiles > 0, "boss should have attacked with projectiles");
                boss.hurt(level.damageSources().playerAttack(p), boss.getMaxHealth() * 0.6F);
                for (int i = 0; i < 60; i++) boss.tick();  // phase 2
                expect(boss.isAlive(), "boss enters phase 2");
            });
        }
        for (var kind : com.dxdcraft.entity.npc.DxDMerchant.Kind.values()) {
            check("merchant trades: " + kind, () -> {
                var npc = com.dxdcraft.entity.npc.DxDMerchant.typeFor(kind).create(level);
                expect(npc != null && npc.getOffers().size() >= 8, kind + " should have 8 trades");
            });
        }
        check("boss summon sigils", () -> {
            ServerLevel heaven = src.getServer().getLevel(DimensionTravel.HEAVEN);
            expect(heaven != null, "heaven exists");
            p = fresh();
            hold(com.dxdcraft.registry.ModGear.SERAPH_SIGIL.get());
            BlockPos at = p.blockPosition().below();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false);
            var r = p.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(p, InteractionHand.MAIN_HAND, hit));
            expect(r == net.minecraft.world.InteractionResult.FAIL, "Seraph Sigil must not work in the Overworld");
        });
        check("Curios: worn Sacred Gear works", () -> {
            if (!com.dxdcraft.compat.CuriosCompat.isLoaded()) return; // optional mod not installed: nothing to test
            p = fresh();
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object inv = ((java.util.Optional<?>) api.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, p))
                    .orElseThrow(() -> new AssertionError("player has no Curios inventory"));
            Class<?> handlerI = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            Object hands = ((java.util.Optional<?>) handlerI.getMethod("getStacksHandler", String.class).invoke(inv, "hands"))
                    .orElseThrow(() -> new AssertionError("DxD Craft's 'hands' Curios slot is missing"));
            Class<?> stacksI = Class.forName("top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            var stacks = (net.neoforged.neoforge.items.IItemHandlerModifiable) stacksI.getMethod("getStacks").invoke(hands);
            stacks.setStackInSlot(0, new ItemStack(ModItems.BOOSTED_GEAR.get()));
            p.tickCount++; // new tick -> fresh Curios cache
            expect(com.dxdcraft.compat.CuriosCompat.has(p, ModItems.BOOSTED_GEAR.get()), "worn Boosted Gear should be seen");
            expect(DxDLogic.holdingBoostedGear(p), "worn Boosted Gear should build boosts");
            for (String slot : new String[]{"back", "ring", "bracelet", "necklace", "belt", "charm"}) {
                expect(((java.util.Optional<?>) handlerI.getMethod("getStacksHandler", String.class).invoke(inv, slot)).isPresent(), "Curios slot " + slot);
            }
        });
        check("Balance Breaker transformation scene", () -> {
            p = fresh();
            com.dxdcraft.logic.BalanceBreaker.start(p, "boosted_gear", true);
            expect(com.dxdcraft.logic.BalanceBreaker.active(p), "form should be active");
            expect(com.dxdcraft.logic.BalanceBreaker.transforming(p) == com.dxdcraft.logic.BalanceBreaker.scenesEnabled(),
                    "scene window should match the config");
            if (com.dxdcraft.logic.BalanceBreaker.transforming(p)) {
                float before = p.getHealth();
                p.hurt(level.damageSources().generic(), 6.0F);
                expect(p.getHealth() == before, "player must not be hurt mid-scene");
            }
            com.dxdcraft.logic.BalanceBreaker.end(p, true);
            expect(!com.dxdcraft.logic.BalanceBreaker.active(p) && !com.dxdcraft.logic.BalanceBreaker.transforming(p), "form should end cleanly");
        });
        check("Curios: every gear passes Curios' own slot validation", () -> {
            if (!com.dxdcraft.compat.CuriosCompat.isLoaded()) return;
            p = fresh();
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            var slotsFor = api.getMethod("getItemStackSlots", ItemStack.class, net.minecraft.world.entity.LivingEntity.class);
            List<String> bad = new ArrayList<>();
            for (var e : com.dxdcraft.compat.CuriosCompat.GEAR_SLOTS.entrySet()) {
                java.util.Map<?, ?> m = (java.util.Map<?, ?>) slotsFor.invoke(null, new ItemStack(e.getKey().get()), p);
                if (!m.containsKey(e.getValue())) bad.add(e.getKey().getId().getPath() + " -> " + m.keySet());
            }
            expect(bad.isEmpty(), "Curios rejects: " + bad);
        });
        check("achievements and recipes load", () -> {
            String[] advs = {"root", "born_anew", "level_20", "level_50", "level_100", "sacred_awakening", "balance_breaker", "sublimation",
                    "juggernaut", "juggernaut_complete", "elemental", "twin_elements", "trade_exorcist", "stairway_heaven", "holy_silver",
                    "seraphite", "trade_angel", "full_set_holy_silver", "full_set_seraphite", "seraph_slayer", "angelic_ward", "blessed_hoe",
                    "into_underworld", "demonite", "hellfire_ruby", "trade_devil", "full_set_demonite", "full_set_hellfire_ruby",
                    "overlord_slayer", "excavator", "dragon_slayer", "dragonite", "full_set_dragonite", "triple_crown"};
            List<String> missing = new ArrayList<>();
            for (String a : advs) if (src.getServer().getAdvancements().get(com.dxdcraft.DxDCraft.id(a)) == null) missing.add(a);
            expect(missing.isEmpty(), "missing achievements: " + missing);
            String[] recipes = {"dragonite_ingot", "seraph_sigil", "infernal_sigil", "blessed_hoe", "overlords_excavator", "angelic_ward",
                    "holy_silver_ingot_from_smelting_raw", "demonite_ingot_from_blasting_raw", "seraphite_chestplate", "dragonite_pickaxe"};
            List<String> missingR = new ArrayList<>();
            for (String r : recipes) if (src.getServer().getRecipeManager().byKey(com.dxdcraft.DxDCraft.id(r)).isEmpty()) missingR.add(r);
            expect(missingR.isEmpty(), "missing recipes: " + missingR);
        });

        // ---------------------------------------------------------------- cleanup + report
        for (Mob m : spawned) m.discard();
        level.getEntitiesOfClass(com.dxdcraft.entity.StrayDevil.class, p.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
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
