# DxD Craft

A High School DxD inspired mod for **Minecraft 1.21.1 + NeoForge**. All action, no fan service.

## Build the .jar (one time setup, ~5 minutes)

1. Install **Java 21** (e.g. Temurin JDK 21 from adoptium.net).
2. Unzip this folder, open a terminal inside it, and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. The mod appears at `build/libs/dxdcraft-1.0.0.jar`. Drop it into your `.minecraft/mods` folder
   (with NeoForge 1.21.1 installed).

To test without installing: `gradlew runClient` launches a dev copy of Minecraft with the mod loaded.

## How to play

### Races
| Race | How | Perks |
|---|---|---|
| Devil | Use an **Evil Piece** | Wing flight, night vision at night, Power of Destruction. Weak to holy water and light spears. |
| Angel | Use a **Holy Feather** (humans only) | Wing flight, slow healing in daylight, Light Spears. Killing a villager makes you fall. |
| Fallen Angel | Use a **Fallen Feather** | Wing flight, strength at night, stronger Light Spears. |
| Human | Use a **Purification Scroll** | Resets you. |

Flying with wings costs 1 mana per second. If you run out mid-air, you float down safely.

### Evil Pieces (Devils)
- **Pawn** – higher jumps
- **Knight** – Speed II
- **Bishop** – double mana and mana regen, stronger Destruction
- **Rook** – Strength + Resistance
- **Queen** – everything above

Using a different piece later swaps your piece.

### Sacred Gears (anyone can use)
Every gear has an active skill (right-click) and an **Ultimate** (see below).

| Gear | Active skill | Ultimate |
|---|---|---|
| **Boosted Gear** | Gains a BOOST every 10 s (max 10). Punch to release them (+50% dmg each) or right-click for a Dragon Shot | **Balance Breaker: Scale Mail** – max boosts + 30 s of Strength III, Resistance III, Speed II |
| **Divine Dividing** | Hits weaken/slow and empower you. Right-click: Divide all enemies in 8 blocks (30 mana) | **Half Dimension** – every enemy within 16 blocks loses half its health |
| **Twilight Healing** | Heal yourself, or right-click a creature to heal it | **Saint's Miracle** – full heal + cleanse for you and allies within 16 blocks |
| **Sword Birth** | Netherite-grade sword. Right-click: blades erupt under enemies in 6 blocks (25 mana) | **Blade Blacksmith** – 20 damage to every enemy within 10 blocks and launches them |
| **Absorption Line** | Latch onto what you're looking at: drain HP, heal, gain mana | **Line Drain** – drains every enemy within 12 blocks, refills your mana |
| **Annihilation Maker** | Create 2 tamed Annihilation Beasts (60 mana, 30 s cooldown) | **Anti-Monster Legion** – creates 2 empowered iron guardians |
| **Dimension Lost** | Mist Step: teleport 12 blocks ahead, enemies lose track of you (20 mana) | **Mist Banishment** – enemies within 16 blocks are teleported far away (bosses resist) |
| **Regulus Nemea** | Hits like a diamond axe. Right-click: Lion's Roar knockback blast (30 mana) | **Lion King Rage** – 45 s of Strength III, Resistance II, Regen II, extra hearts |
| **Zenith Tempest** | Call lightning on what you're looking at, up to 32 blocks (30 mana) | **Tempest Storm** – thunderstorm + lightning on every enemy within 20 blocks |
| **Forbidden Balor View** | Freeze Gaze: stop one target for 5 s (40 mana) | **Time Stop** – freezes every enemy within 20 blocks for 8 s |

### Ultimates and the ULT gauge
- You charge the **ULT gauge** by dealing damage, faster by taking damage, and slowly over time. It's shown on the action bar as `ULT xx%`.
- At 100% you get an "ULTIMATE READY" alert. Press **G** (or sneak + right-click a Sacred Gear) to unleash it.
- Holding a Sacred Gear fires that gear's ultimate (the gear must be at **Stage III: Balance Breaker**). Holding no gear fires your **race ultimate**:

| Race | Race ultimate (G) | Racial ability (V) |
|---|---|---|
| Human | **Last Stand** – full heal, Resistance II, Strength II, Speed II for 20 s | **Adrenaline** – speed, haste, jump boost (20 mana, 30 s cd) |
| Devil | **Crimson Annihilation** – fan of 9 Destruction blasts. Pawns instead get **Promotion: Queen** (60 s of Queen-level buffs + full mana) | **Magic Circle Teleport** – warp to your bed or world spawn (40 mana, 60 s cd) |
| Angel | **Judgment Light** – smites enemies within 10 blocks (double vs devils/undead), heals allies | **Holy Barrier** – absorption shield + knocks enemies away (30 mana, 25 s cd) |
| Fallen Angel | **Black Wing Storm** – 16 light spears in every direction, enemies levitate | **Light Rain** – a spear falls on every enemy within 14 blocks (40 mana, 20 s cd) |

Press **R** to cast your element (Sneak+R for the second). Press **J** for the Juggernaut Drive (see below). Keys can be rebound in Options → Controls → DxD Craft. Creative mode ignores gauge, mana and cooldowns for testing.

### DxD Levels
You start at **Lv 1** (max 100). Earn DxD XP by:
- **Defeating mobs** – XP equal to the mob's max health (a zombie is 20, the Heavenly Dragon is 300, players are 20)
- **Spending mana** – 1 XP per 5 mana spent on any ability

Next level needs `30 + 15 × level` XP. Every level gives **+5 max mana**, and every 10 levels gives **+1 mana regen**. Your level shows on the action bar and in `/dxd info`.

| Level | Unlocks |
|---|---|
| 20 | **Incomplete Juggernaut Drive** |
| 40 | **Complete Juggernaut Drive** (also needs that gear mastered with a Heavenly Dragon Heart) |

If you've mastered a gear but aren't Lv 40 yet, pressing J still triggers the drive, but it runs as the incomplete, berserk version.

### Mana costs
| Ability | Mana |
|---|---|
| Any ultimate (gear or race) | 50 + full ULT gauge |
| Juggernaut Drive activation | 80 + full ULT gauge |
| Longinus Smasher / Compression Divider | 40 |
| Dragon Shot | 10 |
| Twilight Healing (self / other) | 15 / 20 |
| Absorption Line | 10 (steals 25 back) |
| Divine Dividing area Divide | 30 |
| Sword Birth / Dimension Lost / Regulus Nemea / Zenith Tempest | 25 / 20 / 30 / 30 |
| Forbidden Balor View / Annihilation Maker | 40 / 60 |
| Sigil of Destruction / Light Spear | 25 / 15 |
| Racial ability (Human / Devil / Angel / Fallen) | 20 / 40 / 30 / 40 |
| Wing flight | 1 per second |

During a Juggernaut Drive your mana regenerates fast (+3/s incomplete, +6/s complete) instead of refilling instantly.

### Juggernaut Drive (Boosted Gear & Divine Dividing only)
Reach **DxD Lv 20**, grow the gear to **Stage III: Balance Breaker**, hold the Boosted Gear (Red Dragon) or Divine Dividing (White Dragon), fill the ULT gauge, have 80 mana, and press **J**.

| | Incomplete (default) | Complete (mastered) |
|---|---|---|
| Duration | 30 s | 45 s |
| Power | Strength IV, Resistance III, Speed II, flight, +8 hearts | Strength V, Resistance III, Speed III, Regeneration, flight, +10 hearts, full heal |
| Cost while active | Drains 1 HP per second (stops at half a heart) and burns hunger | None |
| Control | **Berserk** – roaring shockwaves every 2 s hit everything within 4 blocks, including friends and pets | Full control |
| Afterwards | Collapse: Weakness III, Slowness III, Mining Fatigue III, Hunger, 0 mana for 30 s | Brief Weakness I |
| Red Dragon extra | Boosts refill quickly | Boosts stay maxed; right-click = **Longinus Smasher** (80-damage blast, 10 s cooldown) |
| White Dragon extra | Aura Divides enemies within 8 blocks and drains them | Same aura; right-click = **Compression Divider** (half the target's max health, up to 80, 10 s cooldown) |

#### Rage meter (involuntary Incomplete drive)
Once you're **Lv 20+** and carry the Boosted Gear or Divine Dividing *anywhere in your inventory*, a **RAGE** meter appears on your action bar.

| Fills from | Rage |
|---|---|
| Taking damage | 1.5 per point of damage |
| Being under 30% health | +3 every second |
| A player dying within 24 blocks | +40 |
| One of your pets being killed | +25 |
| A villager being killed within 16 blocks | +10 |

- You get warnings at 50%, 75% and 90%, and a heartbeat sound from 75%.
- It cools down by 1 per second once you've gone 10 seconds without being hurt and are above half health.
- If you stop carrying a dragon gear, it drains fast.
- **At 100% the dragon takes over.** An **Incomplete** Juggernaut Drive starts on its own, with no ULT gauge, mana or key press needed. It's always the incomplete, berserk version, even if you've mastered the gear. The meter then resets.
- The meter clears when you die.

**Unlocking the Complete drive:** reach **Lv 40**, grow the gear to **Stage IV: Sublimation**, and master the gear. The Heavenly Dragon boss drops a **Heavenly Dragon Heart**. Use it while holding the Boosted Gear or Divine Dividing in your other hand to master that gear permanently. Each gear needs its own Heart. (Ops can use `/dxd master both` to unlock both for testing.)

### Realms: Heaven and the Underworld
Two new dimensions, each reached with a reusable travel item (use it again there to come home):

| Item | Recipe | Goes to |
|---|---|---|
| **Heaven's Gate Key** | 4 Light Fragments, 4 Gold Ingots, Ender Pearl (center) | **Heaven** |
| **Underworld Magic Circle** | 4 Demonic Essence, 4 Blaze Powder, Ender Pearl (center) | **The Underworld** |

**Heaven** is endless floating islands of grass, birch and flowers under a bright sky that never turns to night. It's peaceful (no hostile spawns). Devils take holy damage there and are weakened. Angels regenerate mana faster.

**The Underworld** is a dark crimson cavern realm (bed-safe) crawling with Stray Devils and Rogue Fallen Angels. Angels are weakened there. Devils regenerate mana faster.

Points of interest (each has its own loot chest):

| Realm | POI | What's there |
|---|---|---|
| Heaven | **Gate of Heaven** | Giant quartz-and-gold gate. Light Fragments, Holy Water, rare Twilight Healing / Zenith Tempest |
| Heaven | **Seraph Cathedral** | Quartz cathedral with stained glass and an altar. Best Heaven loot: Light Spear, rare Divine Dividing / Balor View |
| Heaven | **Brave Saint Shrine** | Ring of pillars around a golden dais. Rare Regulus Nemea / Sword Birth |
| Heaven | **Garden of Eden** | Meadow with a blossoming tree. Golden apples, sometimes enchanted |
| Underworld | **Devil Noble Castle** | Walled castle with towers and a throne room. Evil Pieces (rare Queen), Sigil of Destruction, Annihilation Maker |
| Underworld | **Rating Game Arena** | Chessboard battlefield with a Stray Devil spawner. Rare Boosted Gear, Dragon Summoning Gem |
| Underworld | **Stray Devil Lair** | Ruined crypt with a spawner and two chests |
| Underworld | **Grigori Outpost** | Obsidian Fallen Angel tower with a spawner. Loot at the top, rare Dragon Heart |

Ops can use `/dxd travel <heaven|underworld|overworld> [targets]`.

### Destiny: innate Sacred Gears and Elements
The first time a player joins, they roll their **Destiny**:

**35%: born with a dormant Sacred Gear.** You aren't told which one. It has to be **awakened** first, and rarer gears are harder:

| Tier | Gears (rarity weight) | Awakening requirement |
|---|---|---|
| Common | Twilight Healing, Sword Birth, Absorption Line (10 each) | Reach Growth Lv 5, then survive a near-death moment (3 hearts or less). The gear awakens and heals you |
| Rare | Dimension Lost, Forbidden Balor View, Annihilation Maker (6 each) | Reach Growth Lv 10 and defeat 50 hostile creatures |
| Longinus | Boosted Gear, Divine Dividing, Zenith Tempest, Regulus Nemea (2 each) | Reach Growth Lv 20 and slay a boss-class foe (200+ max health: Heavenly Dragon, Wither, Ender Dragon, Warden…) |

On awakening the gear appears in your inventory with a server-wide announcement. If you ever lose it, **Sneak + V** re-manifests it (5 minute cooldown). `/dxd info` shows your progress.

**65%: born with an Element** instead (equal odds): **Lightning, Fire, Water, Wind, Light or Dark**. There's a **15%** chance you're born with **two**.

| Element | Cast — [R] (Sneak+[R] for your 2nd element) | Mana / cooldown | Passive |
|---|---|---|---|
| Lightning | **Thunder Strike** – lightning on what you look at (24 blocks) | 25 / 4 s | Speed |
| Fire | **Flame Burst** – cone of fire, sets enemies alight | 20 / 2 s | Fire immunity |
| Water | **Tidal Wave** – pushes back and slows enemies around you, heals and extinguishes allies | 20 / 3 s | Water breathing, Dolphin's Grace in water |
| Wind | **Gale Dash** – launch yourself forward and blast enemies in front | 15 / 2 s | Jump Boost II, no fall damage |
| Light | **Holy Flash** – damages and blinds enemies around you (double vs devils/undead), heals allies | 25 / 5 s | Night vision |
| Dark | **Shadow Grasp** – withers and slows your target (or nearby enemies), steals life | 25 / 4 s | Invisible while sneaking |

Element damage grows +2% per DxD level.

**Second element through Growth** (element users born with one element): when your **Growth (DxD level) reaches 15** you get a **40%** chance to awaken a second element. If that fails, you get another **60%** chance at **Lv 30**.

Anyone can still craft or loot Sacred Gears. Destiny is an extra, personal power on top of that.

### Sacred Gear growth (stages)
Every Sacred Gear grows with its wielder through four stages. Growth is stored on **you**, per gear, so a lost or re-summoned gear keeps its stage.

| Stage | Gear XP needed* | DxD level | Unlocks |
|---|---|---|---|
| I: Awakened | – | – | Active skill |
| II: Enhanced | 150 | 5 | +25% skill power, -10% mana cost |
| III: **Balance Breaker** | 500 | 15 | +50% power, -20% mana, enchanted glow, **the gear's Ultimate**, the Incomplete Juggernaut Drive and the Rage meter (dragon gears) |
| IV: **Sublimation** | 1500 | 30 | +75% power, -30% mana, required for the **Complete** Juggernaut Drive |

\*Rare gears need 1.25× the XP, Longinus gears 1.5×.

Gear XP comes from using that gear: **+5** per active skill, **+1** per melee hit with it (held or worn in Curios), **+25** per ultimate, **+1** per second of Juggernaut Drive. The tooltip shows the stage, XP and what's next. Reaching Balance Breaker or Sublimation plays a big animation and is announced to everyone.

### Summoning animation
Sacred Gears visibly manifest:
- **Drawing** a gear (switching to it in your hotbar, or equipping it in a Curios slot) shows a quick magic circle and rising spiral in the gear's colour, plus its name and stage on the action bar.
- **Awakening** an innate gear or **re-summoning** it (Sneak+V) plays a larger 2-second summoning circle with gathering sparks and a flash.
- **Stage-ups** play the biggest version (more spiral arms at Balance Breaker+) with a dragon roar.

Everyone nearby sees the particles.

### Magic
- **Sigil of Destruction** (Devils) – Power of Destruction blast, 25 mana. Erases soft blocks it hits.
- **Light Spear** (Angels / Fallen Angels) – 15 mana. Double damage to devils and undead.
- **Holy Water** – throw it. Burns devils and undead, heals everyone else.

The action bar shows your race and mana whenever you hold a DxD item or fly.

### Mobs
- **Stray Devil** – night-time overworld melee monster. Drops Demonic Essence.
- **Rogue Fallen Angel** – throws Light Spears. Drops Light Fragments.
- **Heavenly Dragon** (boss) – summon it with a **Dragon Summoning Gem** on the ground. 300 HP, flies, breathes Dragon Shots, dives at you, and starts BOOSTING below half health. Drops Dragon Scales.

### Progression
Stray Devils → Demonic Essence → Evil Pieces / Sigil
Rogue Fallen Angels → Light Fragments → Feathers / Holy Water / Light Spear
Essence + Fragments + Diamond → Dragon Summoning Gem → Dragon Scales → Boosted Gear & Divine Dividing
Diamond + Essence + Fragment + a themed item → the other Sacred Gears (e.g. 2 Ender Pearls → Dimension Lost)

All recipes show up in the recipe book. Full list in `src/main/resources/data/dxdcraft/recipe/`.

### Commands
Everything is under `/dxd`. Type `/dxd` (or `/dxd help`) in game for the full list. `[targets]` is an optional player selector like `@a` or a name; leave it off to affect yourself. All commands except `help`, `list` and `info` need cheats/op.

| Command | What it does |
|---|---|
| `/dxd help` | List all commands |
| `/dxd list` | Races, Evil Pieces, and every Sacred Gear with its ultimate |
| `/dxd info [targets]` | Level, XP, race, piece, mana, ULT, boosts, mastery, drive status |
| `/dxd race <human\|devil\|angel\|fallen_angel> [targets]` | Set race (Devils start as Pawn) |
| `/dxd piece <pawn\|knight\|bishop\|rook\|queen> [targets]` | Become a Devil with that piece |
| `/dxd reset [targets]` | Back to a fresh Lv 1 Human: clears mastery, gauge, effects, drive |
| `/dxd mana fill [targets]` / `set <n>` / `add <n>` | Change mana |
| `/dxd ult fill [targets]` / `set <0-100>` / `add <n>` | Change the ULT gauge |
| `/dxd level set\|add <n> [targets]` | Change DxD level (1-100) |
| `/dxd xp add <n> [targets]` | Give DxD XP (levels up normally) |
| `/dxd rage fill\|clear\|set <n> [targets]` | Change the Rage meter (fill triggers the drive if eligible) |
| `/dxd boosts <0-10> [targets]` | Set Boosted Gear boosts |
| `/dxd cooldowns [targets]` | Clear racial ability and all gear cooldowns |
| `/dxd ultimate [targets]` | Fire the ultimate right now (gear in hand, else race) |
| `/dxd ability [targets]` | Fire the racial ability for free |
| `/dxd juggernaut start <red\|white> <incomplete\|complete> [targets]` | Force a Juggernaut Drive (ignores level, gear, gauge and mana) |
| `/dxd juggernaut stop [targets]` | End an active drive (incomplete still has its after-effects) |
| `/dxd master <red\|white\|both> [targets]` | Unlock the Complete Juggernaut Drive |
| `/dxd unmaster <red\|white\|both> [targets]` | Lock it again |
| `/dxd give <gears\|pieces\|race_items\|magic\|materials\|all> [targets]` | Item kits |
| `/dxd destiny reroll [targets]` | Reroll the birth power |
| `/dxd destiny awaken [targets]` | Instantly awaken a dormant innate gear |
| `/dxd destiny gear <gear> [targets]` | Give a specific dormant innate gear |
| `/dxd element set <element> [targets]` | Set the first element (removes any innate gear) |
| `/dxd element second <element\|none> [targets]` | Set or remove the second element |
| `/dxd gear stage <gear> <1-4> [targets]` | Set a Sacred Gear's growth stage |
| `/dxd gear xp <gear> <n> [targets]` | Give gear XP |
| `/dxd gear summon [targets]` | Play the summon animation for the held/innate gear |
| `/dxd travel <heaven\|underworld\|overworld> [targets]` | Teleport to a realm |
| `/dxd summon <heavenly_dragon\|stray_devil\|rogue_fallen_angel>` | Spawn a mod mob where you stand (dragon spawns 4 blocks up) |

## Mod compatibility (optional)
DxD Craft runs fine alone. If these mods are installed it integrates with them automatically:

**Lootr:** every loot chest in Heaven and Underworld POIs is placed as a Lootr chest, so each player gets their own loot from every chest.

**Curios API:** Sacred Gears can be worn in Curios slots instead of taking up your hands. Worn gears still work: Boosted Gear builds boosts and releases them on punches, Divine Dividing Divides on hit, the Juggernaut Drive [J] and Rage meter trigger, and [G] fires the worn gear's ultimate. (Active right-click skills still need the gear in hand.)

| Slot | Gear |
|---|---|
| Hands | Boosted Gear |
| Back | Divine Dividing |
| Ring | Twilight Healing |
| Bracelet | Absorption Line |
| Necklace | Regulus Nemea |
| Belt | Sword Birth |
| Charm | Forbidden Balor View, Dimension Lost, Zenith Tempest, Annihilation Maker |

DxD Craft adds these slots to players when Curios is present. To change which slot a gear uses, edit the tags in `src/main/resources/data/curios/tags/item/`.

## Textures
All textures are simple original placeholders. Replace any PNG in
`src/main/resources/assets/dxdcraft/textures/` with your own art (same file name and size) and rebuild.
Mob skins use the standard 64x64 player skin layout. The dragon texture is 128x128.
