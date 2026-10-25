"""Download NeoForge 1.21.1 mods from Modrinth (official CDN) with hash checks. Prints a manifest."""
import hashlib, json, os, sys, urllib.request

OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
API = "https://api.modrinth.com/v2"
WANT = {  # slug -> pinned version_number prefix (None = newest)
    "sophisticated-backpacks": None,
    "sophisticated-core": None,
    "ae2": None,
    "guideme": None,
    "relics-mod": "0.12.8",          # More Relics is built against Relics 0.12.8
    "more-relics": "1.7.7-forRelics-0.12.8",
    "shatterbyte-lib": None,
    "curios": None,
    "architectury-api": None,
}

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "dxdcraft-modpack-test/1.0"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()

manifest = []
for slug, pin in WANT.items():
    versions = json.loads(get(f"{API}/project/{slug}/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D"))
    v = next((v for v in versions if pin is None or v["version_number"].startswith(pin)), None)
    if v is None:
        print("NO VERSION", slug); continue
    f = next((x for x in v["files"] if x["primary"]), v["files"][0])
    path = os.path.join(OUT, f["filename"])
    data = get(f["url"])
    if hashlib.sha512(data).hexdigest() != f["hashes"]["sha512"]:
        print("HASH MISMATCH", slug); continue
    open(path, "wb").write(data)
    manifest.append({"slug": slug, "version": v["version_number"], "file": f["filename"], "url": f"https://modrinth.com/mod/{slug}"})
    print(f"{slug:26s} {v['version_number']:32s} {len(data)//1024} KB")
json.dump(manifest, open(os.path.join(OUT, "..", "modlist.json"), "w"), indent=2)
