package com.dxdcraft.data;

import net.minecraft.ChatFormatting;

public enum Race {
    HUMAN("human", "Human", ChatFormatting.WHITE, false),
    DEVIL("devil", "Devil", ChatFormatting.DARK_RED, true),
    ANGEL("angel", "Angel", ChatFormatting.GOLD, true),
    FALLEN_ANGEL("fallen_angel", "Fallen Angel", ChatFormatting.DARK_PURPLE, true);

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final boolean hasWings;

    Race(String id, String displayName, ChatFormatting color, boolean hasWings) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.hasWings = hasWings;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public ChatFormatting color() { return color; }
    public boolean hasWings() { return hasWings; }
    public boolean usesLight() { return this == ANGEL || this == FALLEN_ANGEL; }

    public static Race byId(String id) {
        for (Race r : values()) {
            if (r.id.equalsIgnoreCase(id)) return r;
        }
        return null;
    }
}
