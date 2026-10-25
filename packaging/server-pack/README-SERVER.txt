DxD CRAFT SERVER PACK 1.1.0
Minecraft 1.21.1 / NeoForge 21.1.264 / Java 21

Included mods
  DxD Craft 1.1.0, Sophisticated Backpacks + Sophisticated Core, Applied Energistics 2 (+ GuideME),
  Relics 0.12.8 + More Relics, OctoLib, Curios, Architectury.
  Only DxD Craft is inside this zip. The others are downloaded automatically from their official
  Modrinth pages the first time the server starts (their licenses don't allow re-uploading them).

------------------------------------------------------------------------------------------
HOSTING ON YOUR OWN PC / VPS
  1. Install Java 21 (e.g. https://adoptium.net).
  2. Unzip this folder where you want the server to live.
  3. Run start.bat (Windows) or ./start.sh (Linux/Mac).
     First start: downloads the mods, installs NeoForge, then stops and asks you to accept the EULA.
  4. Open eula.txt, read https://aka.ms/MinecraftEULA, change eula=false to eula=true.
  5. Run start again. When you see "Done", the server is up (port 25565).

HOSTING ON A SERVER HOST (Apex, Bisect, Shockbyte, etc.)
  1. Set the server type to "Custom Jar" / Java 21 if the panel asks.
  2. Upload EVERYTHING in this folder to the server's root (via the file manager or SFTP).
  3. Set the startup / server jar to:  dxdcraft-server-launcher.jar
  4. Start it once, accept the EULA (eula.txt -> eula=true), start again.
  Give the server at least 4 GB of RAM (6 GB recommended).
  If your host blocks outgoing downloads, run the pack once on your own PC, then upload the
  whole folder (it will then contain mods/ and libraries/ and won't need to download anything).

------------------------------------------------------------------------------------------
UPDATING AN EXISTING SERVER
  Stop the server, upload the new mods/dxdcraft-<version>.jar and delete the old dxdcraft-*.jar
  (two versions in mods/ will crash the server). Worlds and player progress are kept.
  If you start with dxdcraft-server-launcher.jar, it removes the old jar for you.

------------------------------------------------------------------------------------------
PLAYERS (CLIENT) - everyone needs the same mods
  Easiest: import DxDCraftPack-1.1.0.mrpack into Modrinth App, Prism Launcher or ATLauncher.
  CurseForge app: Create Custom Profile -> Minecraft 1.21.1, NeoForge 21.1.264, then add:
    Sophisticated Backpacks, Sophisticated Core, Applied Energistics 2, Relics (version 0.12.8!),
    More Relics, Curios API, Architectury API, and drop dxdcraft-1.1.0.jar into the profile's mods folder.

------------------------------------------------------------------------------------------
SETTINGS
  config/dxdcraft-server.toml  - PvP rules for abilities, Sacred Gear chance, cutscenes, announcements,
                                 realm travel cooldown, anti-spam limit.
  server.properties            - allow-flight MUST stay true or winged players get kicked.
  Admin commands: /dxd ... (op).  /dxd selftest runs a 55-point health check.

Harmless log lines you can ignore: "Mixin ... relics/morerelics ... invalid dist" on startup
(client-only code being skipped on a dedicated server).
