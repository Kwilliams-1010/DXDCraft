package com.dxdcraft.data;

import net.minecraft.ChatFormatting;

/** Elemental affinities for players born without a Sacred Gear. */
public enum Element {
    LIGHTNING("lightning", "Lightning", ChatFormatting.YELLOW, 25, 4, "Thunder Strike", "Speed"),
    FIRE("fire", "Fire", ChatFormatting.RED, 20, 2, "Flame Burst", "Fire immunity"),
    WATER("water", "Water", ChatFormatting.BLUE, 20, 3, "Tidal Wave", "Water breathing, swim speed"),
    WIND("wind", "Wind", ChatFormatting.GREEN, 15, 2, "Gale Dash", "Jump boost, no fall damage"),
    LIGHT("light", "Light", ChatFormatting.GOLD, 25, 5, "Holy Flash", "Night vision"),
    DARK("dark", "Dark", ChatFormatting.DARK_PURPLE, 25, 4, "Shadow Grasp", "Invisible while sneaking");

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final int manaCost;
    private final int cooldownSeconds;
    private final String castName;
    private final String passive;

    Element(String id, String displayName, ChatFormatting color, int manaCost, int cooldownSeconds, String castName, String passive) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
        this.castName = castName;
        this.passive = passive;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public ChatFormatting color() { return color; }
    public int manaCost() { return manaCost; }
    public int cooldownSeconds() { return cooldownSeconds; }
    public String castName() { return castName; }
    public String passive() { return passive; }

    public static Element byId(String id) {
        for (Element e : values()) if (e.id.equalsIgnoreCase(id)) return e;
        return null;
    }

    public static Element byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }
}
