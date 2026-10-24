package com.dxdcraft.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Per-player DxD state, stored as a data attachment and kept on death. */
public class DxDData implements INBTSerializable<CompoundTag> {
    private Race race = Race.HUMAN;
    private Piece piece = Piece.NONE;
    private int mana = 60;

    // Boosted Gear state (not saved: resets on relog, like canon)
    private int boosts = 0;
    private int boostTimer = 0;

    // Ultimate gauge (saved) and racial ability cooldown (game time, not saved)
    public static final int MAX_ULT = 100;
    private int ultCharge = 0;
    private long abilityReadyAt = 0;

    // Juggernaut Drive: 0 = none, 1 = Red (Boosted Gear), 2 = White (Divine Dividing)
    public static final int JD_NONE = 0, JD_RED = 1, JD_WHITE = 2;
    private int jdType = JD_NONE;
    private int jdSeconds = 0;
    private boolean jdComplete = false;
    private boolean masteredRed = false;   // saved
    private boolean masteredWhite = false; // saved

    // DxD level (saved). XP comes from defeating mobs and from spending mana.
    public static final int MAX_LEVEL = 100;
    public static final int JD_LEVEL = 20;           // Incomplete Juggernaut Drive unlocks
    public static final int JD_COMPLETE_LEVEL = 40;  // Complete drive also needs this level (plus a mastered gear)
    private int level = 1;
    private int xp = 0;
    private int announcedLevel = -1; // transient: last level we told the player about

    // Rage meter (saved): fills under stress, forces an Incomplete Juggernaut Drive at 100
    public static final int MAX_RAGE = 100;
    private int rage = 0;
    private int rageStage = 0;        // transient: highest warning shown since the meter was last low
    private long lastHurtTime = 0;    // transient: game time of the last hit taken

    public Race getRace() { return race; }
    public void setRace(Race race) { this.race = race; }
    public Piece getPiece() { return piece; }
    public void setPiece(Piece piece) { this.piece = piece; }

    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = Math.max(0, Math.min(mana, getMaxMana())); }

    public int getMaxMana() {
        int max = race == Race.HUMAN ? 60 : 100;
        if (piece.magic()) max += 100;
        max += 5 * (level - 1);            // +5 max mana per level
        return max;
    }

    public int getManaRegen() {
        int regen = race == Race.HUMAN ? 1 : 2;
        if (piece.magic()) regen *= 2;
        regen += level / 10;               // +1 regen every 10 levels
        return regen;
    }

    public boolean tryUseMana(int amount) {
        if (mana < amount) return false;
        mana -= amount;
        addXp(Math.max(1, amount / 5));    // practicing magic makes you stronger
        return true;
    }

    public int getBoosts() { return boosts; }
    public void setBoosts(int boosts) { this.boosts = boosts; }
    public int getBoostTimer() { return boostTimer; }
    public void setBoostTimer(int t) { this.boostTimer = t; }

    public int getUltCharge() { return ultCharge; }
    public void setUltCharge(int v) { this.ultCharge = Math.max(0, Math.min(MAX_ULT, v)); }
    public void addUltCharge(float v) { setUltCharge(ultCharge + Math.max(0, Math.round(v))); }
    public boolean ultReady() { return ultCharge >= MAX_ULT; }
    public long getAbilityReadyAt() { return abilityReadyAt; }
    public void setAbilityReadyAt(long t) { this.abilityReadyAt = t; }

    public boolean jdActive() { return jdType != JD_NONE && jdSeconds > 0; }
    public int getJdType() { return jdType; }
    public int getJdSeconds() { return jdSeconds; }
    public boolean isJdComplete() { return jdComplete; }
    public void startJd(int type, int seconds, boolean complete) { jdType = type; jdSeconds = seconds; jdComplete = complete; }
    public void setJdSeconds(int s) { jdSeconds = s; }
    public void clearJd() { jdType = JD_NONE; jdSeconds = 0; jdComplete = false; }
    public boolean isMasteredRed() { return masteredRed; }
    public void setMasteredRed(boolean v) { masteredRed = v; }
    public boolean isMasteredWhite() { return masteredWhite; }
    public void setMasteredWhite(boolean v) { masteredWhite = v; }

    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public static int xpToNext(int level) { return 30 + 15 * level; }

    public void setLevel(int lvl) {
        level = Math.max(1, Math.min(MAX_LEVEL, lvl));
        xp = 0;
        setMana(mana);
    }

    /** Adds XP and levels up as many times as it covers. */
    public void addXp(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return;
        xp += amount;
        while (level < MAX_LEVEL && xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
        }
        if (level >= MAX_LEVEL) xp = 0;
    }

    // ---- Destiny (birth roll), saved ----
    private boolean destinyRolled = false;
    private String innateGear = "";       // registry path of a dormant/awakened Sacred Gear, "" if none
    private boolean gearAwakened = false;
    private int element1 = -1;            // Element ordinal, -1 = none
    private int element2 = -1;
    private int secondChanceStage = 0;    // how many second-element chances have been rolled
    private int hostileKills = 0;
    private boolean bossSlain = false;
    // transient
    private final long[] elementReadyAt = new long[2];
    private long resummonReadyAt = 0;

    // ---- Sacred Gear growth (saved): per gear id ----
    private final java.util.Map<String, Integer> gearXp = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> gearStage = new java.util.HashMap<>();
    // transient
    private int castingStage = 0;          // >0 while a gear skill/ultimate is executing
    private String lastHeldGear = "";
    private String lastWornGear = "";
    private int animTicks = 0, animTotal = 0, animKind = 0;
    private String animGear = "";

    // ---- Awakening cutscenes ----
    private final java.util.Set<String> seenCutscenes = new java.util.HashSet<>(); // saved
    private String cutsceneGear = "";   // transient: active cutscene
    private int cutsceneTick = 0;
    private boolean cutsceneAwaken = false;

    public boolean hasSeenCutscene(String id) { return seenCutscenes.contains(id); }
    public void markCutsceneSeen(String id) { seenCutscenes.add(id); }
    public void clearSeenCutscenes() { seenCutscenes.clear(); }
    public boolean inCutscene() { return !cutsceneGear.isEmpty(); }
    public String getCutsceneGear() { return cutsceneGear; }
    public int getCutsceneTick() { return cutsceneTick; }
    public void setCutsceneTick(int t) { cutsceneTick = t; }
    public boolean isCutsceneAwaken() { return cutsceneAwaken; }
    public void beginCutscene(String gear, boolean awaken) { cutsceneGear = gear; cutsceneTick = 0; cutsceneAwaken = awaken; }
    public void endCutscene() { cutsceneGear = ""; cutsceneTick = 0; cutsceneAwaken = false; }

    public int getGearXp(String id) { return gearXp.getOrDefault(id, 0); }
    public void setGearXp(String id, int xp) { gearXp.put(id, Math.max(0, xp)); }
    public int getGearStage(String id) { return gearStage.getOrDefault(id, 1); }
    public void setGearStage(String id, int st) { gearStage.put(id, Math.max(1, Math.min(4, st))); }
    public java.util.Set<String> grownGears() { return gearXp.keySet(); }
    public int getCastingStage() { return castingStage; }
    public void setCastingStage(int s) { castingStage = s; }
    public String getLastHeldGear() { return lastHeldGear; }
    public void setLastHeldGear(String s) { lastHeldGear = s; }
    public String getLastWornGear() { return lastWornGear; }
    public void setLastWornGear(String s) { lastWornGear = s; }
    public int getAnimTicks() { return animTicks; }
    public int getAnimTotal() { return animTotal; }
    public int getAnimKind() { return animKind; }
    public String getAnimGear() { return animGear; }
    public void startAnim(String gear, int total, int kind) { animGear = gear; animTotal = total; animTicks = total; animKind = kind; }
    public void tickAnim() { if (animTicks > 0) animTicks--; }

    public boolean isDestinyRolled() { return destinyRolled; }
    public void setDestinyRolled(boolean v) { destinyRolled = v; }
    public String getInnateGear() { return innateGear; }
    public void setInnateGear(String id) { innateGear = id == null ? "" : id; }
    public boolean hasInnateGear() { return !innateGear.isEmpty(); }
    public boolean isGearAwakened() { return gearAwakened; }
    public void setGearAwakened(boolean v) { gearAwakened = v; }
    public Element getElement1() { return Element.byOrdinal(element1); }
    public Element getElement2() { return Element.byOrdinal(element2); }
    public void setElement1(Element e) { element1 = e == null ? -1 : e.ordinal(); }
    public void setElement2(Element e) { element2 = e == null ? -1 : e.ordinal(); }
    public boolean hasElement(Element e) { return e != null && (element1 == e.ordinal() || element2 == e.ordinal()); }
    public int getSecondChanceStage() { return secondChanceStage; }
    public void setSecondChanceStage(int v) { secondChanceStage = v; }
    public int getHostileKills() { return hostileKills; }
    public void setHostileKills(int v) { hostileKills = v; }
    public boolean isBossSlain() { return bossSlain; }
    public void setBossSlain(boolean v) { bossSlain = v; }
    public long getElementReadyAt(int slot) { return elementReadyAt[slot]; }
    public void setElementReadyAt(int slot, long t) { elementReadyAt[slot] = t; }
    public long getResummonReadyAt() { return resummonReadyAt; }
    public void setResummonReadyAt(long t) { resummonReadyAt = t; }

    public int getRage() { return rage; }
    public void setRage(int v) { rage = Math.max(0, Math.min(MAX_RAGE, v)); if (rage < 40) rageStage = 0; }
    public int getRageStage() { return rageStage; }
    public void setRageStage(int s) { rageStage = s; }
    public long getLastHurtTime() { return lastHurtTime; }
    public void setLastHurtTime(long t) { lastHurtTime = t; }

    public int getAnnouncedLevel() { return announcedLevel; }
    public void setAnnouncedLevel(int l) { announcedLevel = l; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("race", race.id());
        tag.putString("piece", piece.id());
        tag.putInt("mana", mana);
        tag.putInt("ult", ultCharge);
        tag.putBoolean("masteredRed", masteredRed);
        tag.putBoolean("masteredWhite", masteredWhite);
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("rage", rage);
        tag.putBoolean("destinyRolled", destinyRolled);
        tag.putString("innateGear", innateGear);
        tag.putBoolean("gearAwakened", gearAwakened);
        tag.putInt("element1", element1);
        tag.putInt("element2", element2);
        tag.putInt("secondChanceStage", secondChanceStage);
        tag.putInt("hostileKills", hostileKills);
        tag.putBoolean("bossSlain", bossSlain);
        CompoundTag growth = new CompoundTag();
        for (var e : gearXp.entrySet()) {
            CompoundTag g = new CompoundTag();
            g.putInt("xp", e.getValue());
            g.putInt("stage", getGearStage(e.getKey()));
            growth.put(e.getKey(), g);
        }
        tag.put("gearGrowth", growth);
        net.minecraft.nbt.ListTag seen = new net.minecraft.nbt.ListTag();
        for (String id : seenCutscenes) seen.add(net.minecraft.nbt.StringTag.valueOf(id));
        tag.put("seenCutscenes", seen);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        Race r = Race.byId(tag.getString("race"));
        race = r == null ? Race.HUMAN : r;
        Piece p = Piece.byId(tag.getString("piece"));
        piece = p == null ? Piece.NONE : p;
        level = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("level")));
        xp = Math.max(0, tag.getInt("xp"));
        rage = Math.max(0, Math.min(MAX_RAGE, tag.getInt("rage")));
        destinyRolled = tag.getBoolean("destinyRolled");
        innateGear = tag.getString("innateGear");
        gearAwakened = tag.getBoolean("gearAwakened");
        element1 = tag.contains("element1") ? tag.getInt("element1") : -1;
        element2 = tag.contains("element2") ? tag.getInt("element2") : -1;
        secondChanceStage = tag.getInt("secondChanceStage");
        hostileKills = tag.getInt("hostileKills");
        bossSlain = tag.getBoolean("bossSlain");
        gearXp.clear();
        gearStage.clear();
        seenCutscenes.clear();
        net.minecraft.nbt.ListTag seen = tag.getList("seenCutscenes", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < seen.size(); i++) seenCutscenes.add(seen.getString(i));
        CompoundTag growth = tag.getCompound("gearGrowth");
        for (String key : growth.getAllKeys()) {
            CompoundTag g = growth.getCompound(key);
            gearXp.put(key, g.getInt("xp"));
            gearStage.put(key, Math.max(1, Math.min(4, g.getInt("stage"))));
        }
        mana = tag.getInt("mana");
        ultCharge = tag.getInt("ult");
        masteredRed = tag.getBoolean("masteredRed");
        masteredWhite = tag.getBoolean("masteredWhite");
    }
}
