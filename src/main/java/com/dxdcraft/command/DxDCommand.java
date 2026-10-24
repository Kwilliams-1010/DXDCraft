package com.dxdcraft.command;

import com.dxdcraft.data.DxDData;
import com.dxdcraft.data.Piece;
import com.dxdcraft.data.Race;
import com.dxdcraft.entity.HeavenlyDragon;
import com.dxdcraft.item.SacredGearItem;
import com.dxdcraft.logic.Abilities;
import com.dxdcraft.logic.Destiny;
import com.dxdcraft.data.Element;
import com.dxdcraft.logic.DxDLogic;
import com.dxdcraft.logic.JuggernautDrive;
import com.dxdcraft.logic.RageMeter;
import com.dxdcraft.registry.ModEntities;
import com.dxdcraft.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Every DxD Craft command lives under /dxd. Run /dxd or /dxd help for the list.
 * Everything except help, list and info needs operator permission (cheats on, level 2).
 * Most commands take an optional [targets] player selector; without it they affect you.
 */
public final class DxDCommand {
    private DxDCommand() {}

    @FunctionalInterface
    private interface TargetAction {
        int run(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) throws CommandSyntaxException;
    }

    private static final SuggestionProvider<CommandSourceStack> RACES =
            (c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Race.values()).map(Race::id), b);
    private static final SuggestionProvider<CommandSourceStack> PIECES =
            (c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Piece.values()).filter(p -> p != Piece.NONE).map(Piece::id), b);
    private static final SuggestionProvider<CommandSourceStack> DRAGONS =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.of("red", "white"), b);
    private static final SuggestionProvider<CommandSourceStack> DRAGONS_BOTH =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.of("red", "white", "both"), b);
    private static final SuggestionProvider<CommandSourceStack> JD_MODES =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.of("incomplete", "complete"), b);
    private static final SuggestionProvider<CommandSourceStack> KITS =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.of("gears", "pieces", "race_items", "magic", "materials", "all"), b);
    private static final SuggestionProvider<CommandSourceStack> ELEMENTS =
            (c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Element.values()).map(Element::id), b);
    private static final SuggestionProvider<CommandSourceStack> ELEMENTS_NONE =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.concat(Arrays.stream(Element.values()).map(Element::id), Stream.of("none")), b);
    private static final SuggestionProvider<CommandSourceStack> GEAR_IDS =
            (c, b) -> SharedSuggestionProvider.suggest(Destiny.gearIds().stream(), b);
    private static final SuggestionProvider<CommandSourceStack> REALMS =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.of("heaven", "underworld", "overworld"), b);
    private static final SuggestionProvider<CommandSourceStack> MOBS =
            (c, b) -> SharedSuggestionProvider.suggest(Stream.of("heavenly_dragon", "stray_devil", "rogue_fallen_angel"), b);

    // ------------------------------------------------------------------ registration

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dxd")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(targeted(Commands.literal("info"), DxDCommand::info))

                // Race & piece
                .then(Commands.literal("race").requires(DxDCommand::op)
                        .then(targeted(Commands.argument("race", StringArgumentType.word()).suggests(RACES), DxDCommand::setRace)))
                .then(Commands.literal("piece").requires(DxDCommand::op)
                        .then(targeted(Commands.argument("piece", StringArgumentType.word()).suggests(PIECES), DxDCommand::setPiece)))
                .then(targeted(Commands.literal("reset").requires(DxDCommand::op), DxDCommand::reset))

                // Resources
                .then(Commands.literal("mana").requires(DxDCommand::op)
                        .then(targeted(Commands.literal("fill"), (ctx, t) -> mana(ctx, t, "fill", 0)))
                        .then(Commands.literal("set").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(0, 10000)),
                                (ctx, t) -> mana(ctx, t, "set", IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("add").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(-10000, 10000)),
                                (ctx, t) -> mana(ctx, t, "add", IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("ult").requires(DxDCommand::op)
                        .then(targeted(Commands.literal("fill"), (ctx, t) -> ult(ctx, t, "set", DxDData.MAX_ULT)))
                        .then(Commands.literal("set").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(0, DxDData.MAX_ULT)),
                                (ctx, t) -> ult(ctx, t, "set", IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("add").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(-100, 100)),
                                (ctx, t) -> ult(ctx, t, "add", IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("level").requires(DxDCommand::op)
                        .then(Commands.literal("set").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(1, DxDData.MAX_LEVEL)),
                                (ctx, t) -> level(ctx, t, true, IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("add").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(-100, 100)),
                                (ctx, t) -> level(ctx, t, false, IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("xp").requires(DxDCommand::op)
                        .then(Commands.literal("add").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(1, 1000000)),
                                (ctx, t) -> xp(ctx, t, IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("rage").requires(DxDCommand::op)
                        .then(targeted(Commands.literal("fill"), (ctx, t) -> rage(ctx, t, "fill", DxDData.MAX_RAGE)))
                        .then(targeted(Commands.literal("clear"), (ctx, t) -> rage(ctx, t, "clear", 0)))
                        .then(Commands.literal("set").then(targeted(Commands.argument("amount", IntegerArgumentType.integer(0, DxDData.MAX_RAGE)),
                                (ctx, t) -> rage(ctx, t, "set", IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("boosts").requires(DxDCommand::op)
                        .then(targeted(Commands.argument("amount", IntegerArgumentType.integer(0, DxDLogic.MAX_BOOSTS)), DxDCommand::boosts)))
                .then(targeted(Commands.literal("cooldowns").requires(DxDCommand::op), DxDCommand::cooldowns))

                // Abilities
                .then(targeted(Commands.literal("ultimate").requires(DxDCommand::op), DxDCommand::ultimate))
                .then(targeted(Commands.literal("ability").requires(DxDCommand::op), DxDCommand::ability))

                // Juggernaut Drive & mastery
                .then(Commands.literal("juggernaut").requires(DxDCommand::op)
                        .then(Commands.literal("start")
                                .then(Commands.argument("dragon", StringArgumentType.word()).suggests(DRAGONS)
                                        .then(targeted(Commands.argument("mode", StringArgumentType.word()).suggests(JD_MODES), DxDCommand::jdStart))))
                        .then(targeted(Commands.literal("stop"), DxDCommand::jdStop)))
                .then(Commands.literal("master").requires(DxDCommand::op)
                        .then(targeted(Commands.argument("gear", StringArgumentType.word()).suggests(DRAGONS_BOTH), (ctx, t) -> master(ctx, t, true))))
                .then(Commands.literal("unmaster").requires(DxDCommand::op)
                        .then(targeted(Commands.argument("gear", StringArgumentType.word()).suggests(DRAGONS_BOTH), (ctx, t) -> master(ctx, t, false))))

                // Destiny & elements
                .then(Commands.literal("destiny").requires(DxDCommand::op)
                        .then(targeted(Commands.literal("reroll"), DxDCommand::destinyReroll))
                        .then(targeted(Commands.literal("awaken"), DxDCommand::destinyAwaken))
                        .then(Commands.literal("gear").then(targeted(Commands.argument("gear", StringArgumentType.word()).suggests(GEAR_IDS), DxDCommand::destinyGear))))
                .then(Commands.literal("element").requires(DxDCommand::op)
                        .then(Commands.literal("set").then(targeted(Commands.argument("element", StringArgumentType.word()).suggests(ELEMENTS),
                                (ctx, t) -> elementSet(ctx, t, false))))
                        .then(Commands.literal("second").then(targeted(Commands.argument("element", StringArgumentType.word()).suggests(ELEMENTS_NONE),
                                (ctx, t) -> elementSet(ctx, t, true)))))

                // Realms
                .then(Commands.literal("travel").requires(DxDCommand::op)
                        .then(targeted(Commands.argument("realm", StringArgumentType.word()).suggests(REALMS), DxDCommand::travel)))

                // Items & mobs
                .then(Commands.literal("give").requires(DxDCommand::op)
                        .then(targeted(Commands.argument("kit", StringArgumentType.word()).suggests(KITS), DxDCommand::give)))
                .then(Commands.literal("summon").requires(DxDCommand::op)
                        .then(Commands.argument("mob", StringArgumentType.word()).suggests(MOBS)
                                .executes(ctx -> summon(ctx.getSource(), StringArgumentType.getString(ctx, "mob")))))
        );
    }

    /** Adds "run on yourself" to the node, plus a [targets] child that runs on the selected players. */
    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T targeted(T node, TargetAction action) {
        return node
                .executes(ctx -> action.run(ctx, List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> action.run(ctx, EntityArgument.getPlayers(ctx, "targets"))));
    }

    private static boolean op(CommandSourceStack s) {
        return s.hasPermission(2);
    }

    private static void ok(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.GREEN), true);
    }

    private static int fail(CommandSourceStack src, String msg) {
        src.sendFailure(Component.literal(msg));
        return 0;
    }

    private static String names(Collection<ServerPlayer> targets) {
        if (targets.size() == 1) return targets.iterator().next().getName().getString();
        return targets.size() + " players";
    }

    // ------------------------------------------------------------------ help / list / info

    private static int help(CommandSourceStack src) {
        String[][] rows = {
                {"/dxd help", "this list"},
                {"/dxd list", "races, pieces and Sacred Gears"},
                {"/dxd info [targets]", "race, piece, mana, ULT, mastery, drive"},
                {"/dxd race <race> [targets]", "set race (devils get Pawn)"},
                {"/dxd piece <piece> [targets]", "become a Devil with that Evil Piece"},
                {"/dxd reset [targets]", "back to a fresh Human"},
                {"/dxd mana fill|set|add [n] [targets]", "change mana"},
                {"/dxd ult fill|set|add [n] [targets]", "change the ULT gauge"},
                {"/dxd level set|add <n> [targets]", "change DxD level (1-100)"},
                {"/dxd xp add <n> [targets]", "give DxD XP"},
                {"/dxd rage fill|clear|set <n> [targets]", "change the Rage meter"},
                {"/dxd boosts <0-10> [targets]", "set Boosted Gear boosts"},
                {"/dxd cooldowns [targets]", "clear ability and gear cooldowns"},
                {"/dxd ultimate [targets]", "fire the ultimate (fills gauge)"},
                {"/dxd ability [targets]", "fire the racial ability (free)"},
                {"/dxd juggernaut start <red|white> <incomplete|complete> [targets]", "force a drive"},
                {"/dxd juggernaut stop [targets]", "end a drive"},
                {"/dxd master <red|white|both> [targets]", "unlock Complete Juggernaut Drive"},
                {"/dxd unmaster <red|white|both> [targets]", "lock it again"},
                {"/dxd give <gears|pieces|race_items|magic|materials|all> [targets]", "item kits"},
                {"/dxd destiny reroll|awaken [targets]", "reroll birth power / awaken dormant gear"},
                {"/dxd destiny gear <gear> [targets]", "give a dormant innate Sacred Gear"},
                {"/dxd element set|second <element> [targets]", "set elements (second accepts none)"},
                {"/dxd travel <heaven|underworld|overworld> [targets]", "teleport to a realm"},
                {"/dxd summon <heavenly_dragon|stray_devil|rogue_fallen_angel>", "spawn a mod mob"},
        };
        MutableComponent out = Component.literal("DxD Craft commands").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        for (String[] r : rows) {
            out.append(Component.literal("\n" + r[0]).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("  " + r[1]).withStyle(ChatFormatting.GRAY));
        }
        out.append(Component.literal("\nAll but help/list/info need cheats (op level 2).").withStyle(ChatFormatting.DARK_GRAY));
        src.sendSuccess(() -> out, false);
        return 1;
    }

    private static int list(CommandSourceStack src) {
        MutableComponent out = Component.literal("Races: ").withStyle(ChatFormatting.GOLD);
        out.append(Component.literal(String.join(", ", Arrays.stream(Race.values()).map(Race::id).toList())).withStyle(ChatFormatting.WHITE));
        out.append(Component.literal("\nPieces: ").withStyle(ChatFormatting.GOLD));
        out.append(Component.literal(String.join(", ", Arrays.stream(Piece.values()).filter(p -> p != Piece.NONE).map(Piece::id).toList())).withStyle(ChatFormatting.WHITE));
        out.append(Component.literal("\nSacred Gears:").withStyle(ChatFormatting.GOLD));
        for (Item item : gears()) {
            SacredGearItem gear = (SacredGearItem) item;
            out.append(Component.literal("\n  ").append(new ItemStack(item).getHoverName()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" - ult: " + gear.ultimateName()).withStyle(ChatFormatting.GRAY));
        }
        src.sendSuccess(() -> out, false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            MutableComponent c = Component.literal(p.getName().getString() + ": ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Lv " + d.getLevel() + " ").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(DxDLogic.describe(d))
                    .append(Component.literal("\n  XP " + d.getXp() + "/" + DxDData.xpToNext(d.getLevel())
                            + "   Juggernaut Drive: " + (d.getLevel() >= DxDData.JD_COMPLETE_LEVEL ? "complete-ready"
                            : d.getLevel() >= DxDData.JD_LEVEL ? "incomplete only" : "locked until Lv " + DxDData.JD_LEVEL)).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal("\n  Mana " + d.getMana() + "/" + d.getMaxMana()
                            + "   ULT " + d.getUltCharge() + "%   Rage " + d.getRage() + "%   Boosts " + d.getBoosts()).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("\n  Mastered: Red " + yn(d.isMasteredRed()) + ", White " + yn(d.isMasteredWhite())).withStyle(ChatFormatting.GRAY));
            if (d.hasInnateGear()) {
                c.append(Component.literal("\n  Sacred Gear: " + (d.isGearAwakened()
                        ? Destiny.gearName(d.getInnateGear()) + " (awakened)"
                        : "dormant [" + Destiny.tierOf(d.getInnateGear()).name().toLowerCase() + "] - " + Destiny.progress(d))).withStyle(ChatFormatting.GREEN));
            } else if (d.getElement1() != null) {
                c.append(Component.literal("\n  Elements: ").withStyle(ChatFormatting.GREEN)).append(Destiny.elements(d));
                if (d.getElement2() == null) c.append(Component.literal("  (next 2nd-element chance at Lv "
                        + (d.getSecondChanceStage() == 0 ? Destiny.SECOND_ELEMENT_LEVEL_1 : Destiny.SECOND_ELEMENT_LEVEL_2) + ")").withStyle(ChatFormatting.GRAY));
            }
            if (d.jdActive()) {
                c.append(Component.literal("\n  " + JuggernautDrive.name(d.getJdType(), d.isJdComplete()) + " - " + d.getJdSeconds() + "s left").withStyle(ChatFormatting.RED));
            }
            ctx.getSource().sendSuccess(() -> c, false);
        }
        return targets.size();
    }

    private static String yn(boolean b) {
        return b ? "yes" : "no";
    }

    // ------------------------------------------------------------------ race / piece / reset

    private static int setRace(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        String id = StringArgumentType.getString(ctx, "race");
        Race race = Race.byId(id);
        if (race == null) return fail(ctx.getSource(), "Unknown race: " + id + " (try: human, devil, angel, fallen_angel)");
        for (ServerPlayer p : targets) DxDLogic.setRace(p, race, Piece.PAWN);
        ok(ctx.getSource(), names(targets) + " is now " + race.displayName());
        return targets.size();
    }

    private static int setPiece(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        String id = StringArgumentType.getString(ctx, "piece");
        Piece piece = Piece.byId(id);
        if (piece == null || piece == Piece.NONE) return fail(ctx.getSource(), "Unknown piece: " + id + " (try: pawn, knight, bishop, rook, queen)");
        for (ServerPlayer p : targets) DxDLogic.setRace(p, Race.DEVIL, piece);
        ok(ctx.getSource(), names(targets) + " is now a Devil (" + piece.displayName() + ")");
        return targets.size();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            if (d.jdActive()) JuggernautDrive.end(p, d);
            d.clearJd();
            d.setMasteredRed(false);
            d.setMasteredWhite(false);
            d.setUltCharge(0);
            d.setRage(0);
            d.setBoosts(0);
            d.setBoostTimer(0);
            d.setAbilityReadyAt(0);
            d.setLevel(1);
            d.setAnnouncedLevel(1);
            DxDLogic.setRace(p, Race.HUMAN, Piece.NONE);
            p.removeAllEffects();
        }
        ok(ctx.getSource(), "Reset " + names(targets) + " to a fresh Human");
        return targets.size();
    }

    // ------------------------------------------------------------------ resources

    private static int mana(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String mode, int amount) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            switch (mode) {
                case "fill" -> d.setMana(d.getMaxMana());
                case "set" -> d.setMana(amount);
                default -> d.setMana(d.getMana() + amount);
            }
            DxDLogic.updateFlight(p, d);
        }
        ok(ctx.getSource(), "Mana " + mode + (mode.equals("fill") ? "ed" : " " + amount) + " for " + names(targets));
        return targets.size();
    }

    private static int ult(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String mode, int amount) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            if (mode.equals("set")) d.setUltCharge(amount);
            else d.setUltCharge(d.getUltCharge() + amount);
        }
        ok(ctx.getSource(), "ULT gauge " + mode + " " + amount + " for " + names(targets));
        return targets.size();
    }

    private static int level(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean set, int amount) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            d.setLevel(set ? amount : d.getLevel() + amount);
            DxDLogic.checkLevelUp(p, d);
        }
        ok(ctx.getSource(), "DxD level " + (set ? "set to " : "changed by ") + amount + " for " + names(targets));
        return targets.size();
    }

    private static int xp(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer p : targets) DxDLogic.giveXp(p, amount);
        ok(ctx.getSource(), "Gave " + amount + " DxD XP to " + names(targets));
        return targets.size();
    }

    private static int rage(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, String mode, int amount) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            if (mode.equals("fill")) {
                // Goes through the normal path so it triggers the drive if the player is eligible
                RageMeter.add(p, DxDData.MAX_RAGE, null);
                if (!d.jdActive()) d.setRage(DxDData.MAX_RAGE - 1);
            } else {
                d.setRage(amount);
            }
        }
        String extra = mode.equals("fill") ? " (triggers the drive if they are Lv " + DxDData.JD_LEVEL + "+ and carry a dragon gear)" : "";
        ok(ctx.getSource(), "Rage " + mode + (mode.equals("set") ? " " + amount : "") + " for " + names(targets) + extra);
        return targets.size();
    }

    private static int boosts(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        int n = IntegerArgumentType.getInteger(ctx, "amount");
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            d.setBoosts(n);
            d.setBoostTimer(0);
        }
        ok(ctx.getSource(), "Boosts set to " + n + " for " + names(targets) + " (they reset if the Boosted Gear isn't held)");
        return targets.size();
    }

    private static int cooldowns(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            DxDLogic.data(p).setAbilityReadyAt(0);
            for (DeferredHolder<Item, ? extends Item> holder : ModItems.ITEMS.getEntries()) {
                p.getCooldowns().removeCooldown(holder.get());
            }
        }
        ok(ctx.getSource(), "Cleared cooldowns for " + names(targets));
        return targets.size();
    }

    // ------------------------------------------------------------------ abilities

    private static int ultimate(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            d.setUltCharge(DxDData.MAX_ULT);
            d.setMana(d.getMaxMana());
            for (DeferredHolder<Item, ? extends Item> holder : ModItems.ITEMS.getEntries()) {
                p.getCooldowns().removeCooldown(holder.get());
            }
            Abilities.ultimateKey(p);
        }
        ok(ctx.getSource(), "Triggered ultimate for " + names(targets));
        return targets.size();
    }

    private static int ability(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            d.setAbilityReadyAt(0);
            d.setMana(d.getMaxMana());
            Abilities.raceAbility(p);
        }
        ok(ctx.getSource(), "Triggered racial ability for " + names(targets));
        return targets.size();
    }

    // ------------------------------------------------------------------ juggernaut / mastery

    private static int jdStart(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        String dragon = StringArgumentType.getString(ctx, "dragon");
        String mode = StringArgumentType.getString(ctx, "mode");
        int type;
        if (dragon.equalsIgnoreCase("red")) type = DxDData.JD_RED;
        else if (dragon.equalsIgnoreCase("white")) type = DxDData.JD_WHITE;
        else return fail(ctx.getSource(), "Dragon must be red or white");
        boolean complete;
        if (mode.equalsIgnoreCase("complete")) complete = true;
        else if (mode.equalsIgnoreCase("incomplete")) complete = false;
        else return fail(ctx.getSource(), "Mode must be incomplete or complete");
        for (ServerPlayer p : targets) JuggernautDrive.start(p, type, complete);
        ok(ctx.getSource(), "Started " + JuggernautDrive.name(type, complete) + " for " + names(targets));
        return targets.size();
    }

    private static int jdStop(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        int stopped = 0;
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            if (d.jdActive()) {
                JuggernautDrive.end(p, d);
                stopped++;
            }
        }
        if (stopped == 0) return fail(ctx.getSource(), "No active Juggernaut Drive to stop");
        ok(ctx.getSource(), "Stopped Juggernaut Drive for " + stopped + " player(s)");
        return stopped;
    }

    private static int master(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean value) {
        String gear = StringArgumentType.getString(ctx, "gear").toLowerCase();
        boolean red = gear.equals("red") || gear.equals("both");
        boolean white = gear.equals("white") || gear.equals("both");
        if (!red && !white) return fail(ctx.getSource(), "Gear must be red, white or both");
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            if (red) d.setMasteredRed(value);
            if (white) d.setMasteredWhite(value);
        }
        ok(ctx.getSource(), (value ? "Mastered " : "Unmastered ") + gear + " for " + names(targets)
                + (value ? " - Complete Juggernaut Drive unlocked" : ""));
        return targets.size();
    }

    // ------------------------------------------------------------------ destiny / elements

    private static int destinyReroll(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        for (ServerPlayer p : targets) Destiny.roll(p, DxDLogic.data(p), p.getRandom());
        ok(ctx.getSource(), "Rerolled destiny for " + names(targets));
        return targets.size();
    }

    private static int destinyAwaken(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        int n = 0;
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            if (d.hasInnateGear() && !d.isGearAwakened()) {
                Destiny.awaken(p, d);
                n++;
            }
        }
        if (n == 0) return fail(ctx.getSource(), "Nobody selected has a dormant Sacred Gear");
        ok(ctx.getSource(), "Awakened " + n + " Sacred Gear(s)");
        return n;
    }

    private static int destinyGear(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        String id = StringArgumentType.getString(ctx, "gear").toLowerCase();
        if (!Destiny.gearIds().contains(id)) return fail(ctx.getSource(), "Unknown gear: " + id);
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            d.setDestinyRolled(true);
            d.setInnateGear(id);
            d.setGearAwakened(false);
            d.setElement1(null);
            d.setElement2(null);
            p.sendSystemMessage(Component.literal("A dormant Sacred Gear now sleeps within you. To awaken it: "
                    + Destiny.tierOf(id).requirement).withStyle(ChatFormatting.GREEN));
        }
        ok(ctx.getSource(), "Gave dormant " + Destiny.gearName(id) + " to " + names(targets));
        return targets.size();
    }

    private static int elementSet(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets, boolean second) {
        String id = StringArgumentType.getString(ctx, "element");
        Element e = Element.byId(id);
        if (e == null && !(second && id.equalsIgnoreCase("none"))) return fail(ctx.getSource(), "Unknown element: " + id);
        for (ServerPlayer p : targets) {
            DxDData d = DxDLogic.data(p);
            d.setDestinyRolled(true);
            d.setInnateGear("");
            d.setGearAwakened(false);
            if (second) {
                if (d.getElement1() == null) d.setElement1(Element.FIRE);
                d.setElement2(e != null && e == d.getElement1() ? null : e);
                d.setSecondChanceStage(e == null ? 0 : 2);
            } else {
                d.setElement1(e);
                if (d.getElement2() == e) d.setElement2(null);
            }
        }
        ok(ctx.getSource(), (second ? "Second" : "First") + " element set to " + id + " for " + names(targets));
        return targets.size();
    }

    // ------------------------------------------------------------------ realms

    private static int travel(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        String realm = StringArgumentType.getString(ctx, "realm").toLowerCase();
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key = switch (realm) {
            case "heaven" -> com.dxdcraft.world.DimensionTravel.HEAVEN;
            case "underworld" -> com.dxdcraft.world.DimensionTravel.UNDERWORLD;
            case "overworld" -> net.minecraft.world.level.Level.OVERWORLD;
            default -> null;
        };
        if (key == null) return fail(ctx.getSource(), "Realm must be heaven, underworld or overworld");
        for (ServerPlayer p : targets) com.dxdcraft.world.DimensionTravel.travel(p, key);
        ok(ctx.getSource(), "Sent " + names(targets) + " to " + realm);
        return targets.size();
    }

    // ------------------------------------------------------------------ give / summon

    private static List<Item> gears() {
        List<Item> out = new ArrayList<>();
        for (DeferredHolder<Item, ? extends Item> holder : ModItems.ITEMS.getEntries()) {
            if (holder.get() instanceof SacredGearItem) out.add(holder.get());
        }
        return out;
    }

    private static List<ItemStack> kit(String name) {
        List<ItemStack> out = new ArrayList<>();
        switch (name) {
            case "gears" -> gears().forEach(i -> out.add(new ItemStack(i)));
            case "pieces" -> Stream.of(ModItems.EVIL_PIECE_PAWN, ModItems.EVIL_PIECE_KNIGHT, ModItems.EVIL_PIECE_BISHOP,
                    ModItems.EVIL_PIECE_ROOK, ModItems.EVIL_PIECE_QUEEN).forEach(i -> out.add(new ItemStack(i.get())));
            case "race_items" -> Stream.of(ModItems.HOLY_FEATHER, ModItems.FALLEN_FEATHER, ModItems.PURIFICATION_SCROLL)
                    .forEach(i -> out.add(new ItemStack(i.get())));
            case "magic" -> {
                out.add(new ItemStack(ModItems.DESTRUCTION_SIGIL.get()));
                out.add(new ItemStack(ModItems.LIGHT_SPEAR.get()));
                out.add(new ItemStack(ModItems.HOLY_WATER.get(), 16));
            }
            case "materials" -> {
                out.add(new ItemStack(ModItems.DEMONIC_ESSENCE.get(), 32));
                out.add(new ItemStack(ModItems.LIGHT_FRAGMENT.get(), 32));
                out.add(new ItemStack(ModItems.DRAGON_SCALE.get(), 16));
                out.add(new ItemStack(ModItems.DRAGON_HEART.get(), 2));
                out.add(new ItemStack(ModItems.DRAGON_SUMMONING_GEM.get()));
                out.add(new ItemStack(ModItems.HEAVEN_GATE_KEY.get()));
                out.add(new ItemStack(ModItems.UNDERWORLD_MAGIC_CIRCLE.get()));
            }
            case "all" -> {
                for (String k : List.of("gears", "pieces", "race_items", "magic", "materials")) out.addAll(kit(k));
            }
            default -> {}
        }
        return out;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> targets) {
        String name = StringArgumentType.getString(ctx, "kit").toLowerCase();
        if (kit(name).isEmpty()) return fail(ctx.getSource(), "Unknown kit: " + name + " (gears, pieces, race_items, magic, materials, all)");
        for (ServerPlayer p : targets) {
            for (ItemStack stack : kit(name)) {
                if (!p.getInventory().add(stack)) p.drop(stack, false);
            }
        }
        ok(ctx.getSource(), "Gave the " + name + " kit to " + names(targets));
        return targets.size();
    }

    private static int summon(CommandSourceStack src, String mob) {
        Supplier<? extends EntityType<? extends Mob>> type = switch (mob.toLowerCase()) {
            case "heavenly_dragon" -> ModEntities.HEAVENLY_DRAGON;
            case "stray_devil" -> ModEntities.STRAY_DEVIL;
            case "rogue_fallen_angel" -> ModEntities.ROGUE_FALLEN_ANGEL;
            default -> null;
        };
        if (type == null) return fail(src, "Unknown mob: " + mob + " (heavenly_dragon, stray_devil, rogue_fallen_angel)");
        ServerLevel level = src.getLevel();
        Mob entity = type.get().create(level);
        if (entity == null) return fail(src, "Could not create " + mob);
        Vec3 pos = src.getPosition();
        double y = entity instanceof HeavenlyDragon ? pos.y + 4 : pos.y;
        entity.moveTo(pos.x, y, pos.z, src.getRotation().y, 0.0F);
        if (entity instanceof HeavenlyDragon) entity.setPersistenceRequired();
        level.addFreshEntity(entity);
        ok(src, "Summoned " + mob);
        return 1;
    }
}
