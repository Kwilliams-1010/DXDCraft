# DxD Craft

A High School DxD inspired mod for **Minecraft 1.21.1 + NeoForge**. All action, no fan service.

## Build the .jar (one time setup, ~5 minutes)

1. Install **Java 21** (e.g. Temurin JDK 21 from adoptium.net).
2. Unzip this folder, open a terminal inside it, and run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. The mod appears at `build/libs/dxdcraft-1.1.0.jar`. Drop it into your `.minecraft/mods` folder
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

Press **R** to cast your element (Sneak+R for the second). Press **H** for Balance Breaker and **J** for the Juggernaut Drive (see below). Keys can be rebound in Options → Controls → DxD Craft. Creative mode ignores gauge, mana and cooldowns for testing.

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

### Balance Breaker transformation
Once a gear reaches **Stage III: Balance Breaker**, hold or wear it and press **H** to transform. The gear's armor assembles on you piece by piece (legs, body, arms, helmet, then horns, wings, crest or halo), glowing as each piece forms. Everyone nearby sees it. Press **H** again to release it.

| | |
|---|---|
| Cost | 40 mana to transform, then 2-3 mana per second (less at Stage IV) |
| Ends | when you press H, run out of mana, die, lose the gear, or start a Juggernaut Drive |
| Powers | each gear's own buffs, e.g. Boosted Gear: Strength II, Resistance II, flight and double boost speed; Divine Dividing: Speed II, flight and Divides nearby enemies; Twilight Healing: Regeneration II and heals nearby allies |

Juggernaut Drive has its own heavier, spiked armor in red or white.

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

On awakening the gear's unique cutscene plays, then the gear appears in your inventory with a server-wide announcement. If you ever lose it, **Sneak + V** re-manifests it (5 minute cooldown). `/dxd info` shows your progress.

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

### Awakening cutscenes
Every Sacred Gear has its own 10-second awakening cutscene. It plays when an **innate gear awakens** (the gear is granted when the scene ends), and the **first time you ever draw** a gear you crafted or looted (not in creative).

During the scene:
- Cinematic letterbox bars, a fade from black and a slowly orbiting third-person camera. The normal HUD is hidden and your view returns to normal afterwards.
- You're frozen and can't be hurt, and nearby mobs lose interest in you.
- An exchange between you and the gear's inner voice, typed out line by line, then a white flash and a big title card.
- **Sneak** to skip after the first 2 seconds.

| Gear | Inner voice | Set-piece |
|---|---|---|
| Boosted Gear | Red Dragon | Flame spirals, rising heartbeat, fire pillars, explosion and dragon roar |
| Divine Dividing | White Dragon | Glowing wings unfold behind you, sparks, a high-pitched roar |
| Twilight Healing | Gentle Voice | Twin rings of light at your hands, chimes, a burst of hearts |
| Sword Birth | Echo of Blades | Blades of light erupt from the ground, ending in a ring of swords |
| Absorption Line | Sly Voice | Green lines lash out and retract around you |
| Annihilation Maker | Many Voices | Pools of shadow, rising ink pillars, sculk souls |
| Dimension Lost | Distant Whisper | Fog thickens until everything vanishes in mist |
| Regulus Nemea | Lion's Spirit | A golden mane flares around your head, lion roars |
| Zenith Tempest | Voice of the Storm | Storm clouds and rain overhead, lightning strikes around you (harmless) |
| Forbidden Balor View | The Eye | A still ring of violet light, tolling bells slowing down: time stops |

Ops: `/dxd cutscene <gear> [targets]` plays one, and `/dxd cutscene reset seen [targets]` lets first-draw cutscenes play again.

### Ores, materials and equipment
| Ore | Found in | Mine with | Gives |
|---|---|---|---|
| Holy Silver Ore | Heaven (common, all heights) | Stone pickaxe+ | Raw Holy Silver → smelt → **Holy Silver Ingot** |
| Seraphite Ore | Heaven (rare, glows) | Iron-tier pickaxe+ | **Seraphite** gem |
| Demonite Ore | Underworld (common, y 10–120) | Stone pickaxe+ | Raw Demonite → smelt → **Demonite Ingot** |
| Hellfire Ruby Ore | Underworld (rare, y 10–70, glows) | Iron-tier pickaxe+ | **Hellfire Ruby** gem |

**Dragonite Ingot** (endgame): Netherite Ingot + Dragon Scale + Seraphite + Hellfire Ruby (shapeless).
Ingots also craft into storage blocks (Holy Silver, Demonite, Dragonite) and back.

**Five equipment sets.** Each set has a helmet, chestplate, leggings, boots, sword, pickaxe, axe, shovel and hoe, all crafted in the vanilla shapes:

| Set | Tier | Armor (boots/legs/chest/helm) | Full-set bonus |
|---|---|---|---|
| Holy Silver | Iron+ (faster, 400 uses) | 2/5/6/2 | Regeneration in daylight or in Heaven; immune to Holy Water |
| Demonite | Iron+ (500 uses, fireproof) | 2/5/7/2, toughness 1 | Fire Resistance; Night Vision in the Underworld |
| Seraphite | Diamond+ (1800 uses) | 3/6/8/3, toughness 2.5 | +3 mana regen; Slow Falling while sneaking mid-air |
| Hellfire Ruby | Diamond+ (1700 uses, fireproof) | 3/6/8/3, toughness 2.5 | Strength; anyone who hits you is set on fire |
| Dragonite | Netherite+ (3000 uses, fireproof) | 4/7/9/4, toughness 4 | Strength + Resistance; ULT gauge +1 per second |

All armor works with vanilla armor trims, and the tools are in the vanilla tool tags so other mods recognise them.

**Special tools:**
- **Blessed Hoe** (farming): Holy Silver Hoe + Seraphite + Light Fragments, or buy it from the Angel Merchant. **Sneak + use** on crops to give everything in a 5×5 area a growth pulse.
- **Overlord's Excavator** (mining): Dragonite Pickaxe + Overlord's Crown + Dragonite. Mines **3×3** (sneak for a single block). Respects claims and protection like normal mining.
- **Angelic Ward** (defending): a placeable block made from 2 Seraph Halos (one per Seraph Guardian kill), Seraphite and a Holy Silver Block. Every second it smites hostile mobs within 16 blocks (double damage to undead and devils), pushes them back and makes them glow.

### Bosses
| Boss | Summon | Fight | Drops |
|---|---|---|---|
| **Heavenly Dragon** | Dragon Summoning Gem (anywhere) | Flies, Dragon Shots, dives, BOOSTs below half health | Dragon Scales, Diamonds, Dragon Heart |
| **Seraph Guardian** | **Seraph Sigil**, used in Heaven | Hovers and throws Light Spear volleys; Judgment beam every 10 s smites and lifts you; below half health raises a Sanctuary shield and doubles its spears | **Seraph Halo**, Seraphite, Light Fragments, Holy Silver |
| **Underworld Overlord** | **Infernal Sigil**, used in the Underworld | Heavy melee, Power of Destruction barrages, summons Stray Devils; below half health erupts in hellfire | **Overlord's Crown**, Hellfire Rubies, Demonic Essence, Demonite |

Sigils: Seraph Sigil = Holy Silver + Seraphite + Diamond; Infernal Sigil = Demonite + Hellfire Ruby + Diamond (or buy them from the realm merchants).

### NPC merchants
| NPC | Where | Currency | Sells |
|---|---|---|---|
| **Wandering Exorcist** | Visits players in the Overworld | Emeralds | Holy Water, Light Fragments, Demonic Essence, Purification Scroll, **Heaven's Gate Key**, **Underworld Magic Circle**; buys Fragments and Essence |
| **Angel Merchant** | Lives in every Seraph Cathedral; also visits players in Heaven | Holy Silver | Holy Water, golden carrots, Holy Feather, Seraphite, **Seraph Sigil**, **Blessed Hoe**, **Twilight Healing** |
| **Devil Merchant** | Lives in every Devil Noble Castle; also visits players in the Underworld | Demonite | Evil Pieces (Pawn → Rook), Hellfire Ruby, **Infernal Sigil**, **Sword Birth** |

Every 5 minutes each player has a 25% chance of a visit from their current realm's merchant. Visiting merchants leave after one day; the ones in cathedrals and castles stay forever.

### Progression path
1. **Overworld:** fight Stray Devils and Rogue Fallen Angels for Essence and Fragments, or buy a travel item from a **Wandering Exorcist**.
2. **Enter Heaven / the Underworld:** mine Holy Silver / Demonite for iron-plus gear, and trade with the realm merchant.
3. **Rare gems:** Seraphite / Hellfire Ruby for diamond-plus gear and the sigils.
4. **Realm bosses:** the Seraph Guardian (Halo → Angelic Ward) and the Underworld Overlord (Crown → Excavator).
5. **Heavenly Dragon** → Dragon Scales → **Dragonite**, the endgame set.

### Achievements
A **DxD Craft** tab in the Advancements screen with 34 achievements:
- Destiny, race and level milestones (Lv 20 / 50 / 100)
- Sacred Gear awakening, Balance Breaker, Sublimation, and both Juggernaut Drives
- Elements and Twin Elements
- Entering each realm, every ore, every full armor set, and trading with each merchant
- Each special tool, Dragonite, each boss, and **Triple Crown** for defeating all three

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
| `/dxd balancebreaker start <gear> [targets]` | Force the Balance Breaker armor (no stage, mana or gear needed) |
| `/dxd balancebreaker stop [targets]` | Release it |
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
| `/dxd cutscene <gear> [targets]` | Play a gear's awakening cutscene |
| `/dxd cutscene reset seen [targets]` | Let first-draw cutscenes play again |
| `/dxd selftest` | Run the full 54-check logic self-test against a simulated player (op 4) |
| `/dxd travel <heaven\|underworld\|overworld> [targets]` | Teleport to a realm |
| `/dxd summon <heavenly_dragon\|stray_devil\|rogue_fallen_angel>` | Spawn a mod mob where you stand (dragon spawns 4 blocks up) |

## Multiplayer & dedicated servers
DxD Craft is built to run on servers (NeoForge 1.21.1 dedicated server or LAN).

**Check your install:** run `/dxd selftest` from the server console (or as an op level 4 player). It runs 56 checks (ores generating in both realms, tool tiers, all five armor set bonuses, the Blessed Hoe, Excavator and Angelic Ward, all three boss fights, merchant trades, achievements and recipes, data saving, races, levels, Destiny odds, awakening and cutscenes, all 6 elements, all 10 Sacred Gears with their skills, stages and ultimates, Juggernaut Drive gates, rage, race powers, items, realm landing spots, PvP rules and every `/dxd` command) and reports pass/fail.

**Setup:** put the same `dxdcraft-1.1.0.jar` in the server's `mods` folder **and** every player's `mods` folder. If you use Curios or Lootr, they also go on both. To test a local server from the project: `gradlew runServer`.

**What's server-safe:**
- All game logic (races, mana, levels, gears, drives, rage, destiny, realms) runs on the server and is saved per player in the world save. Players can't cheat it from their client.
- Ability keys are checked server-side (mana, cooldowns, gauge) and rate-limited per player against key spam.
- Summon animations, stage-ups and cutscene effects send **one small packet** to nearby players, and each client draws the particles itself. This keeps server bandwidth low even with many players. Each client caps how many effects it plays at once.
- The cutscene letterbox, dialogue and camera only show for the player whose gear is awakening. Everyone nearby sees the particles and hears the sounds.
- Players in a cutscene can't be hurt or targeted by mobs, and can't cast during it.
- Curios lookups are cached per tick, and realm travel generates only a few chunks to find a landing spot.

**Multiplayer rules:**
- Abilities, projectiles, auras and splash damage follow the server's **PvP setting** and vanilla **teams** (`/team`). With PvP off, Light Spears and Dragon Shots fly straight through other players. You never hurt your own pets.
- **Power of Destruction** block-erasing respects spawn protection and fires the normal block-break event, so claim/protection mods (FTB Chunks, Open Parties and Claims, etc.) can block it.
- Lightning (Zenith Tempest, Lightning element) behaves like vanilla lightning.

### Server config
`world/serverconfig/dxdcraft-server.toml` (created on first start; singleplayer uses `saves/<world>/serverconfig/`):

| Setting | Default | What it does |
|---|---|---|
| `combat.abilitiesRespectPvp` | true | Abilities only hurt players when PvP is on and they aren't teammates |
| `combat.berserkHitsPlayers` | true | Incomplete Juggernaut Drive shockwaves can hit players |
| `combat.destructionBreaksBlocks` | true | Power of Destruction erases soft blocks |
| `announcements.enabled` | true | Announce awakenings, Balance Breakers, drives, dragon summons |
| `announcements.radius` | 0 | Only players this close hear announcements (0 = whole server) |
| `destiny.gearChance` | 0.35 | Chance a new player is born with a dormant Sacred Gear |
| `destiny.doubleElementChance` | 0.15 | Chance of being born with two elements |
| `cutscenes.awakening` | true | Cutscene when an innate gear awakens |
| `cutscenes.firstDraw` | true | Cutscene the first time a player draws each gear |
| `server.realmTravelCooldownSeconds` | 10 | Cooldown for the realm travel items |
| `server.maxAbilityPacketsPerSecond` | 10 | Anti-spam limit for ability keys |

## Recommended modpack
DxD Craft has been tested together with these mods (dedicated server, all checks passing, 20 TPS). They don't depend on each other; install whichever you like:

| Mod | Version tested | Notes |
|---|---|---|
| Sophisticated Backpacks | 1.21.1-3.26.14.2222 | needs Sophisticated Core 1.21.1-1.5.17.2426 |
| Applied Energistics 2 | 19.2.18 | needs GuideME 21.1.20 |
| Relics | **0.12.8** | needs OctoLib 0.6.2 and Curios API. Use 0.12.8, because More Relics is built for it |
| More Relics | 1.7.7-forRelics-0.12.8-1.0 | needs Relics, Architectury API 13.0.11, OctoLib, Curios |
| Curios API | 9.5.1+1.21.1 | also unlocks DxD Craft's worn Sacred Gears |

**Default key bindings don't clash.** DxD Craft uses G / J / R / V. Sophisticated Backpacks uses B / C / X / Z and [ / ]. Relics and AE2 use Shift / Ctrl / Alt combos.

**Expected server log noise:** Relics and More Relics print a few red "Attempted to load class ... for invalid dist DEDICATED_SERVER" lines on dedicated servers. Those are their client-only tweaks being skipped and are harmless.

`DxDCraftPack-1.1.0.mrpack` installs all of the above plus DxD Craft in one click in the Modrinth App, Prism Launcher or ATLauncher.

## Mod compatibility (optional)
DxD Craft runs fine alone. If these mods are installed it integrates with them automatically:

**Lootr:** every loot chest in Heaven and Underworld POIs is placed as a Lootr chest, so each player gets their own loot from every chest.

**Curios API:** Sacred Gears can be worn in Curios slots instead of taking up your hands. Every player gets a dedicated **Sacred Gear** slot (top of the Curios panel, gem icon) that accepts any gear; the classic slots below also still work. A worn gear is visible on your character: the Boosted Gear as a gauntlet, Divine Dividing as folded light wings, Absorption Line as a bracer, Sword Birth as a belt, Regulus Nemea as a pendant, Twilight Healing as rings, and the other four hover over your shoulders. Worn gears still work: Boosted Gear builds boosts and releases them on punches, Divine Dividing Divides on hit, the Juggernaut Drive [J] and Rage meter trigger, and [G] fires the worn gear's ultimate. (Active right-click skills still need the gear in hand.)

| Slot | Gear |
|---|---|
| **Sacred Gear** | any gear |
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
