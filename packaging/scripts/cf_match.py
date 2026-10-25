import json, os, sys, urllib.request, time
MODS = sys.argv[1]
IDS = [309927, 223794, 1173950, 422301, 618298, 445274, 1269280, 916747, 419699]
def get(u):
    r = urllib.request.Request(u, headers={"User-Agent": "dxdcraft-pack/1.0"})
    with urllib.request.urlopen(r, timeout=60) as f: return json.loads(f.read())
jars = {n for n in os.listdir(MODS) if not n.startswith("dxdcraft")}
out = []
for pid in IDS:
    d = get(f"https://api.cfwidget.com/{pid}")
    hit = [f for f in d["files"] if f["name"] in jars]
    if not hit: print("NO MATCH", pid, d["title"]); continue
    f = hit[0]; jars.discard(f["name"])
    print(f"{d['title']:28s} project {pid:8d} file {f['id']:8d} {f['name']}  {f['versions']}")
    out.append({"projectID": pid, "fileID": f["id"], "required": True, "title": d["title"], "file": f["name"]})
print("unmatched jars:", jars)
json.dump(out, open("cf_files.json", "w"), indent=2)
