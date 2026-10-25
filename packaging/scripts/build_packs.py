"""Builds all three DxD Craft packs from a freshly built mod jar.

  python3 packaging/scripts/build_packs.py <dxdcraft jar> <version> <out dir>

Outputs:
  DxDCraftPack-<v>-CurseForge.zip   CurseForge app import (manifest + DxD Craft in overrides)
  DxDCraftPack-<v>.mrpack           Modrinth App / Prism / ATLauncher
  DxDCraft-ServerPack-<v>.zip       dedicated server (launcher downloads the other mods)
Third-party mods are only referenced (official CurseForge / Modrinth downloads), never bundled.
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
PACKAGING = os.path.dirname(HERE)
ROOT = os.path.dirname(PACKAGING)

jar, version, out = sys.argv[1], sys.argv[2], sys.argv[3]
os.makedirs(out, exist_ok=True)
jar_name = os.path.basename(jar)
server_toml = os.path.join(PACKAGING, "server-pack", "config", "dxdcraft-server.toml")

# ---------------------------------------------------------------- CurseForge
manifest = json.load(open(os.path.join(PACKAGING, "curseforge", "manifest.json")))
manifest["version"] = version
cf = os.path.join(out, f"DxDCraftPack-{version}-CurseForge.zip")
with zipfile.ZipFile(cf, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("manifest.json", json.dumps(manifest, indent=2))
    z.write(os.path.join(PACKAGING, "curseforge", "modlist.html"), "modlist.html")
    z.write(jar, f"overrides/mods/{jar_name}")
    z.write(server_toml, "overrides/config/dxdcraft-server.toml")

# ---------------------------------------------------------------- Modrinth (.mrpack)
pack = json.load(open(os.path.join(PACKAGING, "server-pack", "pack.json")))
files = [{"path": f"mods/{m['file']}", "hashes": {"sha512": m["sha512"], "sha1": m["sha1"]},
          "env": {"client": "required", "server": "unsupported" if m.get("side") == "client" else "required"},
          "downloads": [m["url"]], "fileSize": m["size"]}
         for m in pack["mods"]]
index = {"formatVersion": 1, "game": "minecraft", "versionId": version, "name": "DxD Craft Pack",
         "summary": "DxD Craft + Sophisticated Backpacks, Applied Energistics 2, Relics + More Relics, Overflowing Bars",
         "files": files, "dependencies": {"minecraft": pack["minecraft"], "neoforge": pack["neoforge"]}}
mr = os.path.join(out, f"DxDCraftPack-{version}.mrpack")
with zipfile.ZipFile(mr, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("modrinth.index.json", json.dumps(index, indent=2))
    z.write(jar, f"overrides/mods/{jar_name}")

# ---------------------------------------------------------------- server pack
sp_src = os.path.join(PACKAGING, "server-pack")
with tempfile.TemporaryDirectory() as tmp:
    classes = os.path.join(tmp, "classes")
    os.makedirs(classes)
    subprocess.run(["javac", "-d", classes, os.path.join(sp_src, "ServerPackLauncher.java")], check=True)
    shutil.copy(os.path.join(sp_src, "pack.json"), classes)
    with open(os.path.join(tmp, "manifest.txt"), "w") as f:
        f.write("Main-Class: ServerPackLauncher\n")
    launcher = os.path.join(tmp, "dxdcraft-server-launcher.jar")
    subprocess.run(["jar", "cfm", launcher, os.path.join(tmp, "manifest.txt"), "-C", classes, "."], check=True)
    sv = os.path.join(out, f"DxDCraft-ServerPack-{version}.zip")
    with zipfile.ZipFile(sv, "w", zipfile.ZIP_DEFLATED) as z:
        z.write(launcher, "dxdcraft-server-launcher.jar")
        z.write(jar, f"mods/{jar_name}")
        z.write(server_toml, "config/dxdcraft-server.toml")
        for name in ("README-SERVER.txt", "server.properties", "user_jvm_args.txt", "start.bat"):
            z.write(os.path.join(sp_src, name), name)
        info = zipfile.ZipInfo("start.sh")
        info.external_attr = 0o755 << 16
        z.writestr(info, open(os.path.join(sp_src, "start.sh")).read())

for p in (cf, mr, sv):
    print(f"{os.path.basename(p):40s} {os.path.getsize(p) // 1024} KB")
