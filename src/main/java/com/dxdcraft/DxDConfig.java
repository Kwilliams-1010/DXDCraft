package com.dxdcraft;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.function.Supplier;

/**
 * Server-side settings (per world: saves/&lt;world&gt;/serverconfig/dxdcraft-server.toml; on a dedicated
 * server: world/serverconfig/dxdcraft-server.toml). Synced to clients automatically by NeoForge.
 */
public final class DxDConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ABILITIES_RESPECT_PVP;
    public static final ModConfigSpec.BooleanValue DESTRUCTION_BREAKS_BLOCKS;
    public static final ModConfigSpec.BooleanValue BERSERK_HITS_PLAYERS;
    public static final ModConfigSpec.IntValue ANNOUNCE_RADIUS;
    public static final ModConfigSpec.BooleanValue ANNOUNCEMENTS;
    public static final ModConfigSpec.DoubleValue DESTINY_GEAR_CHANCE;
    public static final ModConfigSpec.DoubleValue DOUBLE_ELEMENT_CHANCE;
    public static final ModConfigSpec.BooleanValue FIRST_DRAW_CUTSCENES;
    public static final ModConfigSpec.BooleanValue AWAKENING_CUTSCENES;
    public static final ModConfigSpec.BooleanValue TRANSFORM_SCENES;
    public static final ModConfigSpec.IntValue REALM_TRAVEL_COOLDOWN;
    public static final ModConfigSpec.IntValue MAX_ABILITY_PACKETS_PER_SECOND;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("combat");
        ABILITIES_RESPECT_PVP = b.comment("If true, DxD abilities, projectiles and auras only hurt other players when the server has PvP on,",
                        "and never hurt teammates (vanilla /team).")
                .define("abilitiesRespectPvp", true);
        BERSERK_HITS_PLAYERS = b.comment("If true, the Incomplete Juggernaut Drive's berserk shockwaves can hit other players (still subject to abilitiesRespectPvp).")
                .define("berserkHitsPlayers", true);
        DESTRUCTION_BREAKS_BLOCKS = b.comment("If true, the Power of Destruction erases soft blocks it hits.",
                        "Block breaking is reported to protection/claim mods, which can still prevent it.")
                .define("destructionBreaksBlocks", true);
        b.pop();

        b.push("announcements");
        ANNOUNCEMENTS = b.comment("Announce awakenings, Balance Breakers and Juggernaut Drives to other players.")
                .define("enabled", true);
        ANNOUNCE_RADIUS = b.comment("Only players within this many blocks (same dimension) see announcements. 0 = whole server.")
                .defineInRange("radius", 0, 0, 100000);
        b.pop();

        b.push("destiny");
        DESTINY_GEAR_CHANCE = b.comment("Chance a new player is born with a dormant Sacred Gear (otherwise they get an element).")
                .defineInRange("gearChance", 0.35, 0.0, 1.0);
        DOUBLE_ELEMENT_CHANCE = b.comment("Chance an element user is born with two elements.")
                .defineInRange("doubleElementChance", 0.15, 0.0, 1.0);
        b.pop();

        b.push("cutscenes");
        AWAKENING_CUTSCENES = b.comment("Play the cutscene when an innate Sacred Gear awakens.")
                .define("awakening", true);
        FIRST_DRAW_CUTSCENES = b.comment("Play the cutscene the first time a player draws each Sacred Gear type.")
                .define("firstDraw", true);
        TRANSFORM_SCENES = b.comment("Play the animated transformation scene (camera sweep, armor flying in) for Balance Breaker",
                        "and Juggernaut Drive. The player can't move or be hurt for those 3 seconds. Off = armor appears instantly.")
                .define("transformation", true);
        b.pop();

        b.push("server");
        REALM_TRAVEL_COOLDOWN = b.comment("Cooldown in seconds for Heaven's Gate Key / Underworld Magic Circle.")
                .defineInRange("realmTravelCooldownSeconds", 10, 0, 3600);
        MAX_ABILITY_PACKETS_PER_SECOND = b.comment("Ability key presses accepted per player per second (anti-spam).")
                .defineInRange("maxAbilityPacketsPerSecond", 10, 1, 100);
        b.pop();

        SPEC = b.build();
    }

    private DxDConfig() {}

    /** Reads a value, falling back to a default if the config isn't loaded yet (e.g. very early). */
    public static <T> T get(Supplier<T> value, T fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }
}
