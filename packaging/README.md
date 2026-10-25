# Packaging

Only DxD Craft is shipped in the packs. Every other mod is referenced by its official download
(several are All Rights Reserved and can't be re-uploaded).

| Pack | Built from | Used by |
|---|---|---|
| `DxDCraftPack-<ver>-CurseForge.zip` | `curseforge/manifest.json` + `overrides/mods/dxdcraft-<ver>.jar` + `overrides/config/` | CurseForge app → Import |
| `DxDCraftPack-<ver>.mrpack` | `scripts/build_mrpack.py <mods dir> <dxd jar> <out>` | Modrinth App, Prism, ATLauncher |
| `DxDCraft-ServerPack-<ver>.zip` | `server-pack/` (launcher + configs) + `mods/dxdcraft-<ver>.jar` | Dedicated servers |

## Server launcher
`server-pack/ServerPackLauncher.java` embeds `pack.json`, downloads and SHA-512-verifies each mod,
installs NeoForge on first start, then launches the server.

```
javac -d build ServerPackLauncher.java
cp pack.json build/
printf 'Main-Class: ServerPackLauncher\n' > manifest.txt
jar cfm dxdcraft-server-launcher.jar manifest.txt -C build .
```

## Updating mod versions
1. `scripts/fetch_mods.py <dir>` downloads the pinned versions from Modrinth.
2. Test with `/dxd selftest` on a dedicated server.
3. Rebuild `pack.json` / the `.mrpack`, and run `scripts/cf_match.py <dir>` to look up CurseForge file IDs for `manifest.json`.
