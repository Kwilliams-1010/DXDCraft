package com.dxdcraft.cutscene;

import java.util.List;
import java.util.Map;

/**
 * The awakening cutscene for each Sacred Gear: dialogue, title card and colours.
 * Shared by server (timing) and client (rendering). All lines are original writing.
 *
 * Timeline (ticks, 20 = 1 second):
 *   0-20    fade in from black, letterbox slides in
 *   20-120  build-up: two dialogue lines, gear-specific particles grow
 *   120     CLIMAX: white flash, title card
 *   120-185 aftermath: closing line
 *   185-200 fade out
 */
public final class CutsceneScripts {
    public static final int DURATION = 200;
    public static final int CLIMAX = 120;
    public static final int SKIPPABLE_AFTER = 40;

    /** speaker == null means "the player". */
    public record Line(int start, int end, String speaker, String text, int color) {}

    public record Script(String gearId, String title, String tagline, int color, List<Line> lines) {}

    private static Script s(String id, String title, String tagline, int color, String voice,
                            String playerLine, String voiceLine, String closing) {
        return new Script(id, title, tagline, color, List.of(
                new Line(22, 68, null, playerLine, 0xFFFFFF),
                new Line(70, 118, voice, voiceLine, color),
                new Line(140, 186, voice, closing, color)));
    }

    public static final Map<String, Script> SCRIPTS = Map.of(
            "boosted_gear", s("boosted_gear", "BOOSTED GEAR", "Gauntlet of the Red Dragon Emperor", 0xFF3B3B, "Red Dragon",
                    "My heart... why is it burning?",
                    "So you're the one I've been sleeping inside. Took you long enough, partner.",
                    "Every ten seconds, your power grows. Don't you dare waste it."),
            "divine_dividing", s("divine_dividing", "DIVINE DIVIDING", "Wings of the Vanishing Dragon", 0xBFE3FF, "White Dragon",
                    "Everything feels... lighter. Quieter.",
                    "A new host. Let us see if you can halve the world before it halves you.",
                    "Strike them, and I will take what they have. Divide."),
            "twilight_healing", s("twilight_healing", "TWILIGHT HEALING", "Rings of Mending Light", 0x8CFF9E, "Gentle Voice",
                    "There's... warmth in my hands.",
                    "This light was made to mend, never to harm. Share it with anyone who needs it.",
                    "Friend or foe, a wound is a wound."),
            "sword_birth", s("sword_birth", "SWORD BIRTH", "Forge of Demonic Blades", 0xA05CFF, "Echo of Blades",
                    "There's steel in my blood...",
                    "Picture a blade. Any blade at all. Now make it real.",
                    "Your imagination is your armory."),
            "absorption_line", s("absorption_line", "ABSORPTION LINE", "The Line That Drinks", 0x7BD63A, "Sly Voice",
                    "Something is reaching out of me...",
                    "Everything out there has strength to spare. Let's borrow some. Permanently.",
                    "Latch on, and drink deep."),
            "annihilation_maker", s("annihilation_maker", "ANNIHILATION MAKER", "Creator of Monsters", 0x6E5A8A, "Many Voices",
                    "The shadows... they're moving on their own.",
                    "Give us shape. Give us purpose. We will obey you.",
                    "Every shadow you cast can become an army."),
            "dimension_lost", s("dimension_lost", "DIMENSION LOST", "The Mist Between Worlds", 0xDDE3EA, "Distant Whisper",
                    "The air is thickening into fog...",
                    "Here and there are the same place, if the mist says so.",
                    "Step into the fog. Step out anywhere."),
            "regulus_nemea", s("regulus_nemea", "REGULUS NEMEA", "The Golden Lion King", 0xFFC93C, "Lion's Spirit",
                    "Something wild is roaring inside me!",
                    "A king does not hide. Stand tall, and I will be your mane of gold.",
                    "Roar, and the battlefield will kneel."),
            "zenith_tempest", s("zenith_tempest", "ZENITH TEMPEST", "Sovereign of the Sky", 0x7CC8FF, "Voice of the Storm",
                    "The sky... is it answering me?",
                    "Rain, wind and thunder. Ask, and the heavens will listen.",
                    "Call the storm, and it will always come."),
            "forbidden_balor_view", s("forbidden_balor_view", "FORBIDDEN BALOR VIEW", "The Eye That Stops Time", 0xB070FF, "The Eye",
                    "My eye burns... everything is slowing down...",
                    "I see all things, and all I see stands still.",
                    "Look upon them, and time itself will wait for you."));

    private CutsceneScripts() {}

    public static Script get(String gearId) {
        return SCRIPTS.get(gearId);
    }
}
