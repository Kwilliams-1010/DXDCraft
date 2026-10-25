"""Generates the Balance Breaker / Juggernaut Drive armor textures (64x32 humanoid layout) and their
glow maps, the shared add-on texture (horns, wings, halo...) and the worn-gear texture.
All designs are original. Usage: python3 tools/gen_gear_armor.py <assets/dxdcraft/textures dir>
"""
import math
import os
import random
import sys
from PIL import Image

# ---------------------------------------------------------------- 64x32 humanoid face rectangles (x, y, w, h)
HEAD = {"top": (8, 0, 8, 8), "bottom": (16, 0, 8, 8), "right": (0, 8, 8, 8), "front": (8, 8, 8, 8), "left": (16, 8, 8, 8), "back": (24, 8, 8, 8)}
BODY = {"top": (20, 16, 8, 4), "bottom": (28, 16, 8, 4), "right": (16, 20, 4, 12), "front": (20, 20, 8, 12), "left": (28, 20, 4, 12), "back": (32, 20, 8, 12)}
ARM = {"top": (44, 16, 4, 4), "bottom": (48, 16, 4, 4), "right": (40, 20, 4, 12), "front": (44, 20, 4, 12), "left": (48, 20, 4, 12), "back": (52, 20, 4, 12)}
LEG = {"top": (4, 16, 4, 4), "bottom": (8, 16, 4, 4), "right": (0, 20, 4, 12), "front": (4, 20, 4, 12), "left": (8, 20, 4, 12), "back": (12, 20, 4, 12)}

# ---------------------------------------------------------------- per-gear designs
#  base / trim / gem colours, motif, visor style
GEARS = {
    "boosted_gear":         dict(base=(178, 22, 34), trim=(226, 176, 62), gem=(255, 176, 40), motif="dragon", visor="fangs"),
    "divine_dividing":      dict(base=(226, 232, 244), trim=(84, 132, 214), gem=(110, 214, 255), motif="knight", visor="slit"),
    "sword_birth":          dict(base=(64, 46, 104), trim=(186, 150, 255), gem=(206, 160, 255), motif="blades", visor="cross"),
    "absorption_line":      dict(base=(44, 92, 40), trim=(150, 220, 90), gem=(170, 255, 96), motif="vines", visor="slit"),
    "forbidden_balor_view": dict(base=(46, 32, 64), trim=(214, 178, 90), gem=(230, 110, 255), motif="runes", visor="eye"),
    "dimension_lost":       dict(base=(112, 126, 142), trim=(200, 214, 230), gem=(180, 232, 255), motif="mist", visor="slit"),
    "zenith_tempest":       dict(base=(46, 110, 170), trim=(232, 244, 255), gem=(156, 232, 255), motif="storm", visor="slit"),
    "annihilation_maker":   dict(base=(34, 28, 38), trim=(138, 26, 42), gem=(255, 48, 64), motif="dragon", visor="fangs"),
    "regulus_nemea":        dict(base=(204, 146, 34), trim=(110, 58, 16), gem=(255, 60, 48), motif="beast", visor="beast"),
    "twilight_healing":     dict(base=(240, 230, 200), trim=(214, 178, 64), gem=(150, 255, 176), motif="runes", visor="slit"),
}
JUGGERNAUT = {
    "juggernaut_red":   dict(base=(120, 10, 16), trim=(40, 8, 10), gem=(255, 70, 30), motif="dragon", visor="fangs", spikes=True),
    "juggernaut_white": dict(base=(214, 222, 234), trim=(48, 64, 108), gem=(96, 200, 255), motif="dragon", visor="fangs", spikes=True),
}


def mul(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3])


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


class Painter:
    def __init__(self, design, seed):
        self.d = design
        self.img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        self.px, self.gx = self.img.load(), self.glow.load()
        self.rng = random.Random(seed)

    # -- primitives (face-local coords)
    def put(self, r, x, y, c, glow=False):
        fx, fy, w, h = r
        if 0 <= x < w and 0 <= y < h:
            self.px[fx + x, fy + y] = tuple(c[:3]) + (255,)
            if glow:
                self.gx[fx + x, fy + y] = tuple(c[:3]) + (255,)

    def plate(self, r, shade_top=1.15, shade_bottom=0.8):
        fx, fy, w, h = r
        base = self.d["base"]
        for y in range(h):
            f = shade_top + (shade_bottom - shade_top) * (y / max(1, h - 1))
            for x in range(w):
                n = 1.0 + self.rng.uniform(-0.05, 0.05)
                self.put(r, x, y, mul(base, f * n))
        # edges darker -> reads as separate plates
        for x in range(w):
            self.put(r, x, h - 1, mul(base, 0.55))
        for y in range(h):
            self.put(r, 0, y, mul(self.px[fx, fy + y], 0.85))
            self.put(r, w - 1, y, mul(self.px[fx + w - 1, fy + y], 0.75))

    def band(self, r, y, color=None, glow=False):
        c = color or self.d["trim"]
        for x in range(r[2]):
            self.put(r, x, y, mul(c, 1.1 if x % 3 == 0 else 1.0), glow)

    def gem(self, r, x, y, w=2, h=2):
        g = self.d["gem"]
        for dy in range(h):
            for dx in range(w):
                c = mix(g, (255, 255, 255), 0.45) if (dx, dy) == (0, 0) else mul(g, 0.9 if dy else 1.0)
                self.put(r, x + dx, y + dy, c, glow=True)
        # setting
        t = self.d["trim"]
        for dx in range(-1, w + 1):
            for dy in (-1, h):
                if self.px_inside(r, x + dx, y + dy):
                    self.put(r, x + dx, y + dy, mul(t, 0.8))

    def px_inside(self, r, x, y):
        return 0 <= x < r[2] and 0 <= y < r[3]

    def motif(self, r, kind):
        m = self.d["motif"]
        w, h = r[2], r[3]
        base, trim, gem = self.d["base"], self.d["trim"], self.d["gem"]
        if m == "dragon":
            for y in range(1, h - 1):
                for x in range(w):
                    if (x + (y // 2) % 2) % 2 == 0 and y % 2 == 0:
                        self.put(r, x, y, mul(base, 0.72))
        elif m == "knight":
            if w >= 4:
                for y in range(1, h - 1):
                    self.put(r, w // 2, y, mul(base, 1.25))
                    self.put(r, w // 2 - 1, y, mul(base, 0.82))
        elif m == "blades":
            for y in range(1, h - 1, 3):
                for x in range(w):
                    if (x + y) % 4 == 0:
                        self.put(r, x, y, mix(trim, gem, 0.5), glow=True)
        elif m == "vines":
            for y in range(h):
                x = int(w / 2 + math.sin(y * 0.9) * (w / 2 - 0.6))
                self.put(r, x, y, gem, glow=True)
        elif m == "runes":
            glyphs = [(0, 0), (1, 1), (0, 2), (2, 0), (2, 2)]
            for gy in range(1, h - 3, 4):
                gx = (gy * 3) % max(1, w - 2)
                for dx, dy in glyphs[: 3 + gy % 3]:
                    self.put(r, gx + dx % max(1, w - gx), gy + dy, mix(trim, gem, 0.4), glow=True)
        elif m == "mist":
            for y in range(h):
                for x in range(w):
                    v = math.sin(x * 0.9 + y * 0.6) + math.sin(y * 1.3 - x * 0.4)
                    if v > 1.2:
                        self.put(r, x, y, mix(base, trim, 0.6))
        elif m == "storm":
            x = w // 2
            for y in range(1, h - 1):
                x = max(0, min(w - 1, x + (1 if (y // 2) % 2 else -1)))
                self.put(r, x, y, mix(trim, gem, 0.5), glow=True)
        elif m == "beast":
            for y in range(0, h, 2):
                for x in range(w):
                    if (x * 7 + y * 3) % 5 == 0:
                        self.put(r, x, y, mul(base, 1.2))

    def spikes(self, r):
        # light triangular studs along the top edge (Juggernaut)
        for x in range(0, r[2], 2):
            self.put(r, x, 0, mix(self.d["base"], (255, 255, 255), 0.35))
            self.put(r, x, 1, mul(self.d["trim"], 1.2))

    # -- parts
    def head(self):
        for face, r in HEAD.items():
            self.plate(r)
            if face in ("top", "bottom"):
                self.motif(r, "top")
                continue
            if face == "front":
                self.front_visor(r)
            else:
                self.motif(r, "side")
                self.band(r, 7 if face != "back" else 6)
                if face == "back" and self.d["visor"] == "beast":
                    for x in range(8):
                        for y in range(0, 4 + (x * 5) % 3):
                            self.put(r, x, y, mul(self.d["trim"], 1.0 + 0.1 * (x % 2)))
        if self.d["visor"] == "beast":   # mane around the sides and top
            for face in ("right", "left", "top"):
                r = HEAD[face]
                for x in range(8):
                    for y in range(0, 2 + (x * 3) % 3):
                        self.put(r, x, y, mul(self.d["trim"], 0.9 + 0.12 * (x % 2)))

    def front_visor(self, r):
        d = self.d
        v = d["visor"]
        dark = (16, 10, 14)
        self.band(r, 0)
        self.gem(r, 3, 1, 2, 1)
        if v == "slit":
            for x in range(1, 7):
                self.put(r, x, 4, dark)
            self.put(r, 2, 4, d["gem"], True); self.put(r, 5, 4, d["gem"], True)
        elif v == "fangs":
            for x in range(1, 7):
                self.put(r, x, 3, dark); self.put(r, x, 4, dark)
            self.put(r, 2, 3, d["gem"], True); self.put(r, 5, 3, d["gem"], True)
            for x in (1, 3, 4, 6):
                self.put(r, x, 6, mix(d["base"], (255, 255, 255), 0.4))
                self.put(r, x, 7, mix(d["base"], (255, 255, 255), 0.25))
        elif v == "cross":
            for x in range(1, 7):
                self.put(r, x, 4, dark)
            for y in range(3, 7):
                self.put(r, 3, y, dark); self.put(r, 4, y, dark)
            self.put(r, 2, 4, d["gem"], True); self.put(r, 5, 4, d["gem"], True)
        elif v == "eye":
            for x in range(1, 7):
                self.put(r, x, 4, dark)
            for x in range(2, 6):
                self.put(r, x, 3, dark); self.put(r, x, 5, dark)
            self.put(r, 3, 4, d["gem"], True); self.put(r, 4, 4, d["gem"], True)
        elif v == "beast":
            for x in range(1, 7):
                self.put(r, x, 3, dark)
            self.put(r, 2, 3, d["gem"], True); self.put(r, 5, 3, d["gem"], True)
            for x in range(2, 6):
                self.put(r, x, 5, mix(d["base"], (255, 240, 200), 0.4))
            self.put(r, 3, 6, (40, 20, 10)); self.put(r, 4, 6, (40, 20, 10))
        for x in range(8):
            self.put(r, x, 7, mul(d["trim"], 0.85))

    def body(self):
        for face, r in BODY.items():
            self.plate(r)
            if face in ("top", "bottom"):
                continue
            self.motif(r, face)
            self.band(r, 0)
            self.band(r, 9)
            self.band(r, 10, mul(self.d["trim"], 0.75))
            if face == "front":
                # chest plates: a V of trim and the core jewel
                for i in range(3):
                    self.put(r, 1 + i, 2 + i, mul(self.d["trim"], 0.9))
                    self.put(r, 6 - i, 2 + i, mul(self.d["trim"], 0.9))
                self.gem(r, 3, 5, 2, 2)
                self.put(r, 3, 10, self.d["gem"], True); self.put(r, 4, 10, self.d["gem"], True)
            if face == "back":
                for y in range(2, 8):
                    self.put(r, 3, y, mul(self.d["base"], 0.6)); self.put(r, 4, y, mul(self.d["base"], 0.6))
            if self.d.get("spikes"):
                self.spikes(r)

    def arm(self):
        for face, r in ARM.items():
            self.plate(r)
            if face in ("top", "bottom"):
                if face == "top":
                    for x in range(4):
                        for y in range(4):
                            self.put(r, x, y, mul(self.d["trim"], 0.95 + 0.05 * ((x + y) % 2)))
                continue
            self.motif(r, face)
            self.band(r, 0); self.band(r, 1, mul(self.d["trim"], 0.8))   # pauldron rim
            self.band(r, 7)                                                # wrist
            if face == "right":                                            # outer side: back of the hand jewel
                self.gem(r, 1, 9, 2, 2)
            if face == "front":
                for y in range(9, 12):
                    self.put(r, 1, y, mul(self.d["base"], 0.6)); self.put(r, 2, y, mul(self.d["base"], 0.6))
            if self.d.get("spikes") and face == "right":
                for y in range(2, 7, 2):
                    self.put(r, 0, y, mix(self.d["base"], (255, 255, 255), 0.4))

    def leg(self):
        for face, r in LEG.items():
            self.plate(r)
            if face in ("top", "bottom"):
                continue
            self.motif(r, face)
            self.band(r, 0)
            if face == "front":                                            # knee guard with a small jewel
                for x in range(4):
                    self.put(r, x, 4, mul(self.d["trim"], 1.0)); self.put(r, x, 6, mul(self.d["trim"], 0.8))
                self.put(r, 1, 5, self.d["gem"], True); self.put(r, 2, 5, self.d["gem"], True)
            for y in range(9, 12):                                         # sabaton
                for x in range(4):
                    self.put(r, x, y, mul(self.d["base"], 0.62 + 0.06 * (y - 9)))
            self.band(r, 9, mul(self.d["trim"], 0.9))

    def paint(self):
        self.head(); self.body(); self.arm(); self.leg()
        return self.img, self.glow


def extras(path_base):
    """Shared add-on texture: greyscale metal (tinted in-game), translucent wing membrane, halo ring."""
    rng = random.Random(7)
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()
    for y in range(0, 32):
        for x in range(64):
            v = 205 + rng.randint(-14, 14) + (25 if (x + y) % 9 == 0 else 0) - (35 if y % 6 == 5 else 0)
            px[x, y] = (v, v, v, 255)
    # wing membrane: front face (0,32) and back face (15,32), each 15x13. Tapers towards the tip with
    # feathered / scalloped trailing edge; veins fan out from the root. The back face is mirrored.
    for face_x, mirrored in ((0, False), (15, True)):
        for fy in range(13):
            for fx in range(15):
                t = (14 - fx) if mirrored else fx          # 0 at the root, 14 at the tip
                limit = 13 - t * 0.62 - (1.5 if t % 3 == 1 else 0)
                if fy >= limit:
                    continue
                vein = (t % 4 == 0 and fy < limit - 1) or fy == 0
                a = 255 if vein else int(235 - 7 * t - 3 * fy)
                v = 255 if vein else 225
                px[face_x + fx, 32 + fy] = (v, v, v, max(90, min(255, a)))
    # halo ring: top face (34,48)-(44,58) and bottom face (44,48)-(54,58)
    for ox in (34, 44):
        for y in range(10):
            for x in range(10):
                dist = math.hypot(x - 4.5, y - 4.5)
                if 3.3 <= dist <= 4.7:
                    px[ox + x, 48 + y] = (255, 255, 255, 255)
    im.save(path_base + ".png")
    # glow: the back core's jewel (front + back faces of the 5x5 core box at texOffs 36,0)
    g = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    gp = g.load()
    for ox in (38, 45):
        for y in range(2, 7):
            for x in range(5):
                d = math.hypot(x - 2, y - 4.5)
                if d <= 1.6:
                    gp[ox + x, y] = (255, 255, 255, 255)
                elif d <= 2.3:
                    gp[ox + x, y] = (150, 150, 150, 255)
    g.save(path_base + "_glow.png")


def worn(path):
    """Worn-gear pieces: greyscale metal with ridges (tinted per gear), white jewels, feathered folded wings."""
    rng = random.Random(11)
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    for y in range(32):
        for x in range(48):
            v = 200 + rng.randint(-12, 12) - (40 if y % 4 == 3 else 0) + (30 if x % 4 == 0 else 0)
            px[x, y] = (v, v, v, 255)
    for y in range(0, 4):                       # jewels
        for x in range(48, 56):
            px[x, y] = (255, 255, 255, 255) if (x + y) % 3 else (210, 210, 210, 255)
    for y in range(4, 14):                      # folded wing feathers
        for x in range(56, 62):
            a = 255 if (x + y) % 3 else 170
            px[x, y] = (255, 255, 255, a)
    im.save(path)


if __name__ == "__main__":
    root = sys.argv[1]
    os.makedirs(f"{root}/armor", exist_ok=True)
    os.makedirs(f"{root}/worn", exist_ok=True)
    for i, (name, design) in enumerate(list(GEARS.items()) + list(JUGGERNAUT.items())):
        img, glow = Painter(design, seed=100 + i).paint()
        file = name if name.startswith("juggernaut") else "bb_" + name
        img.save(f"{root}/armor/{file}.png")
        glow.save(f"{root}/armor/{file}_glow.png")
    extras(f"{root}/armor/gear_extras")
    worn(f"{root}/worn/worn_gear.png")
    print("armor textures written")
