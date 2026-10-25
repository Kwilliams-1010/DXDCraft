"""Builds a Modrinth modpack (.mrpack): a manifest pointing at the official Modrinth downloads
(nothing redistributed) + DxD Craft itself in overrides/mods."""
import hashlib, json, os, sys, urllib.request, zipfile

MODS_DIR, DXD_JAR, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
API = "https://api.modrinth.com/v2"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "dxdcraft-modpack/1.0"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()

files = []
for name in sorted(os.listdir(MODS_DIR)):
    data = open(os.path.join(MODS_DIR, name), "rb").read()
    sha1 = hashlib.sha1(data).hexdigest()
    v = json.loads(get(f"{API}/version_file/{sha1}?algorithm=sha1"))
    f = next(x for x in v["files"] if x["hashes"]["sha1"] == sha1)
    files.append({"path": f"mods/{name}", "hashes": {"sha1": sha1, "sha512": hashlib.sha512(data).hexdigest()},
                  "env": {"client": "required", "server": "required"}, "downloads": [f["url"]], "fileSize": len(data)})
index = {"formatVersion": 1, "game": "minecraft", "versionId": "1.0.0", "name": "DxD Craft Pack",
         "summary": "DxD Craft + Sophisticated Backpacks, Applied Energistics 2, Relics + More Relics",
         "files": files, "dependencies": {"minecraft": "1.21.1", "neoforge": "21.1.264"}}
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("modrinth.index.json", json.dumps(index, indent=2))
    z.write(DXD_JAR, f"overrides/mods/{os.path.basename(DXD_JAR)}")
print(f"{OUT}: {len(files)} mods referenced + DxD Craft")
