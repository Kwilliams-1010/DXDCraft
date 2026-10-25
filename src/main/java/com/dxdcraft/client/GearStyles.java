package com.dxdcraft.client;

/** Look of each gear's Balance Breaker armor and worn form: colors and which armor pieces it has. */
public final class GearStyles {
    public enum Worn { GAUNTLET, BRACER, BELT, PENDANT, RINGS, FOLDED_WINGS, FLOATING }

    /**
     * primary / secondary = metal tints for the add-on pieces; glow = jewels and energy.
     * wings: 0 none, 1 solid (bone + membrane), 2 energy (translucent, glowing).
     */
    public record Style(int primary, int secondary, int glow, boolean horns, boolean crest, boolean halo,
                        int wings, boolean pauldrons, boolean claws, boolean tail, Worn worn) {}

    private GearStyles() {}

    public static Style of(String gear) {
        return switch (gear) {
            case "boosted_gear" -> new Style(0xB3121C, 0xE0B040, 0xFFB028, true, false, false, 1, true, true, true, Worn.GAUNTLET);
            case "divine_dividing" -> new Style(0xE8EEF8, 0x5A8FD8, 0x7FD8FF, false, true, false, 2, true, false, false, Worn.FOLDED_WINGS);
            case "sword_birth" -> new Style(0x3A2A5C, 0xB08CFF, 0xC9A6FF, false, true, false, 0, true, false, false, Worn.BELT);
            case "absorption_line" -> new Style(0x2D5A2A, 0x9BE05A, 0xA8FF60, false, false, false, 0, true, true, false, Worn.BRACER);
            case "forbidden_balor_view" -> new Style(0x2A1D3A, 0xD4B25A, 0xE06CFF, false, true, true, 0, true, false, false, Worn.FLOATING);
            case "dimension_lost" -> new Style(0x6E7C8C, 0xC8D6E6, 0xB8E8FF, false, true, false, 2, true, false, false, Worn.FLOATING);
            case "zenith_tempest" -> new Style(0x2E6FA8, 0xE8F4FF, 0x9CE8FF, false, true, false, 2, false, false, false, Worn.FLOATING);
            case "annihilation_maker" -> new Style(0x1E1820, 0x8A1A2A, 0xFF3040, true, false, false, 1, true, true, true, Worn.FLOATING);
            case "regulus_nemea" -> new Style(0xC8901E, 0x6A3A10, 0xFFD24A, false, true, false, 0, true, true, true, Worn.PENDANT);
            case "twilight_healing" -> new Style(0xF0E6C8, 0xD8B440, 0x9CFFB0, false, false, true, 2, false, false, false, Worn.RINGS);
            default -> new Style(0x888888, 0xCCCCCC, 0xFFFFFF, false, false, false, 0, false, false, false, Worn.FLOATING);
        };
    }

    /** Juggernaut Drive: every piece, bigger and darker. */
    public static Style juggernaut(boolean red) {
        return red ? new Style(0x7A0A10, 0x2A0A0A, 0xFF4020, true, true, false, 1, true, true, true, Worn.GAUNTLET)
                : new Style(0xD8DEE8, 0x30406A, 0x60C8FF, true, true, false, 2, true, true, true, Worn.FOLDED_WINGS);
    }
}
