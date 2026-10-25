"""Draws the 32x32 Sacred Gear item icons (original designs).

Each icon is painted with vector shapes on a 4x canvas, downsampled, then cleaned up into
pixel art: hard alpha, a 1px dark outline, and a light rim on the top-left edges.
Usage: python3 tools/gen_gear_icons.py <assets/dxdcraft/textures/item dir>
"""
import math
import sys
from PIL import Image, ImageDraw, ImageFilter

S = 4          # supersampling
N = 32 * S     # canvas size


def canvas():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def P(*pts):
    """Scale 32-grid points to the canvas."""
    return [(x * S, y * S) for x, y in pts]


def ell(d, cx, cy, rx, ry, **kw):
    d.ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], **kw)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def finish(im, outline=(24, 14, 18, 255)):
    """Downsample to 32x32 pixel art with a dark outline and a soft top-left rim light."""
    small = im.resize((32, 32), Image.LANCZOS)
    px = small.load()
    for y in range(32):
        for x in range(32):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= 110 else (0, 0, 0, 0)
    out = small.copy()
    o = out.load()
    for y in range(32):
        for x in range(32):
            if px[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 32 and 0 <= ny < 32 and px[nx, ny][3]:
                    o[x, y] = outline
                    break
    # rim light: pixels whose upper-left neighbour is empty get brighter
    for y in range(32):
        for x in range(32):
            if px[x, y][3] and (x == 0 or y == 0 or not px[x - 1, y - 1][3]):
                r, g, b, _ = px[x, y]
                o[x, y] = (min(255, r + 38), min(255, g + 38), min(255, b + 38), 255)
    return out


def gem(d, cx, cy, r, color, glint=(255, 255, 255, 255)):
    ell(d, cx, cy, r, r, fill=shade(color, 0.55))
    ell(d, cx - 0.25, cy - 0.25, r * 0.78, r * 0.78, fill=color)
    ell(d, cx - r * 0.38, cy - r * 0.42, r * 0.3, r * 0.3, fill=glint)


# --------------------------------------------------------------------- icons

def boosted_gear():
    im, d = canvas()
    red, dark, gold = (196, 28, 36, 255), (110, 12, 20, 255), (232, 182, 64, 255)
    # forearm cuff (diagonal, lower left)
    d.polygon(P((4, 30), (2, 24), (11, 17), (17, 23), (10, 31)), fill=dark)
    d.polygon(P((5, 28), (3.5, 24.5), (11, 18.5), (15.5, 23), (9, 29.5)), fill=red)
    d.line(P((4, 25.5), (11.5, 19)), fill=gold, width=5)
    d.line(P((6.5, 29), (14, 22.5)), fill=gold, width=4)
    # wrist spikes
    d.polygon(P((3, 22), (1, 17), (6, 20)), fill=gold)
    d.polygon(P((9, 16), (8, 11), (12, 15)), fill=gold)
    # hand plate
    d.polygon(P((10, 17), (17, 9), (25, 12), (24, 20), (17, 24)), fill=dark)
    d.polygon(P((11.5, 17), (17, 10.5), (23.5, 13), (22.5, 19), (17, 22.5)), fill=red)
    # knuckle plates + claws
    for i, (x, y) in enumerate([(17, 9), (21, 9.5), (24.5, 11.5)]):
        d.polygon(P((x - 1.6, y + 0.8), (x + 1.6, y - 0.4), (x + 4.5, y - 6.5 + i), (x + 0.5, y - 2.2)), fill=(236, 230, 220, 255))
        d.polygon(P((x - 1.4, y + 1.4), (x + 1.6, y + 0.4), (x + 1.2, y + 2.4), (x - 1.6, y + 3.2)), fill=gold)
    d.polygon(P((24, 18), (29, 17), (30, 21), (25, 21)), fill=dark)  # thumb
    d.polygon(P((24.5, 18.6), (28.4, 17.8), (29, 20.4), (25.2, 20.4)), fill=red)
    gem(d, 17.5, 16.5, 3.1, (255, 176, 40, 255))
    return finish(im)


def divine_dividing():
    im, d = canvas()
    white, steel, blue, deep = (238, 244, 252, 255), (176, 194, 220, 255), (92, 150, 230, 255), (40, 74, 150, 255)
    for side in (-1, 1):
        cx = 16
        for i, (ang, length, w) in enumerate([(-55, 14, 6.0), (-28, 15, 6.0), (-3, 14, 5.5), (20, 11, 5.0)]):
            a = math.radians(ang)
            sx, sy = cx + side * 2.5, 16 + i * 0.6
            ex, ey = sx + side * math.cos(a) * length, sy + math.sin(a) * length
            nx, ny = -math.sin(a) * w, math.cos(a) * w
            col = [white, white, steel, steel][i]
            d.polygon(P((sx, sy), (sx + side * nx * 0.5, sy + ny * 0.5), (ex, ey), (sx - side * nx * 0.5, sy - ny * 0.5)), fill=col)
            d.line(P((sx, sy), (ex, ey)), fill=blue, width=3)
    # center diamond
    d.polygon(P((16, 9), (20.5, 16), (16, 24), (11.5, 16)), fill=deep)
    d.polygon(P((16, 11), (19, 16), (16, 21.5), (13, 16)), fill=blue)
    d.polygon(P((15, 12.5), (16.5, 13), (15, 16.5)), fill=(210, 236, 255, 255))
    return finish(im, outline=(20, 26, 48, 255))


def sword_birth():
    im, d = canvas()
    ghost = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    g = ImageDraw.Draw(ghost)
    g.polygon(P((9, 25), (11, 27), (29, 6), (28, 4)), fill=(150, 120, 230, 140))  # echo blade behind
    im.alpha_composite(ghost)
    blade, edge, dark = (140, 104, 228, 255), (214, 196, 255, 255), (58, 36, 108, 255)
    d.polygon(P((6, 23), (9, 26), (27.5, 5.5), (28.5, 2.5), (25.5, 3.5)), fill=dark)
    d.polygon(P((7, 23), (9, 25), (27, 4.5), (26, 4)), fill=blade)
    d.line(P((8, 23.6), (26.6, 4.4)), fill=edge, width=3)
    # guard
    d.polygon(P((3.5, 21), (6, 18.5), (13.5, 26), (11, 28.5)), fill=(206, 176, 92, 255))
    d.polygon(P((4.5, 21), (6, 19.5), (12.5, 26), (11, 27.5)), fill=(240, 214, 128, 255))
    gem(d, 8.4, 23.6, 1.9, (210, 120, 255, 255))
    # grip + pommel
    d.line(P((6, 26), (2.8, 29.2)), fill=(70, 46, 40, 255), width=9)
    d.line(P((5.5, 26.4), (3.2, 28.8)), fill=(110, 76, 60, 255), width=4)
    ell(d, 2.4, 29.6, 1.7, 1.7, fill=(206, 176, 92, 255))
    return finish(im, outline=(22, 12, 34, 255))


def absorption_line():
    im, d = canvas()
    green, dark, light = (82, 168, 64, 255), (34, 84, 30, 255), (176, 236, 120, 255)
    # bracer band (perspective ellipse)
    ell(d, 13, 22, 11, 7.5, fill=dark)
    ell(d, 13, 20.5, 11, 7.5, fill=green)
    ell(d, 13, 18.6, 7.2, 3.6, fill=(0, 0, 0, 0))
    ell(d, 13, 18.6, 7.2, 3.6, outline=dark, width=4)
    d.arc([3 * S, 14 * S, 23 * S, 26 * S], 200, 340, fill=light, width=5)
    for x in (6, 13, 20):
        ell(d, x, 23.4 if x == 13 else 22.2, 1.3, 1.3, fill=light)
    # tendril coiling out of the band
    pts = []
    for i in range(60):
        t = i / 59
        r = 6.5 * (1 - t) + 1.5
        a = t * 4.2 * math.pi
        pts.append((21 + t * 5 + math.cos(a) * r * 0.55, 15 - t * 12 + math.sin(a) * r * 0.35))
    d.line(P(*pts), fill=dark, width=10)
    d.line(P(*pts), fill=green, width=6)
    d.line(P(*pts[::2]), fill=light, width=2)
    gem(d, pts[-1][0], pts[-1][1], 2.2, (190, 255, 110, 255))
    return finish(im, outline=(14, 30, 12, 255))


def forbidden_balor_view():
    im, d = canvas()
    gold, dgold, purple, deep = (226, 186, 88, 255), (140, 104, 40, 255), (168, 80, 220, 255), (40, 16, 64, 255)
    d.line(P((16, 1), (16, 5)), fill=dgold, width=5)            # bail
    ell(d, 16, 15, 12, 12, fill=dgold)
    ell(d, 16, 14.5, 11, 11, fill=gold)
    for k in range(8):                                          # filigree studs
        a = k * math.pi / 4
        ell(d, 16 + math.cos(a) * 9.2, 14.5 + math.sin(a) * 9.2, 1.1, 1.1, fill=(255, 236, 160, 255))
    ell(d, 16, 14.5, 7.5, 7.5, fill=deep)
    d.polygon(P((8.6, 14.5), (16, 9.4), (23.4, 14.5), (16, 19.6)), fill=(232, 220, 240, 255))  # eye white
    ell(d, 16, 14.5, 3.6, 3.6, fill=purple)
    ell(d, 16, 14.5, 1.6, 2.6, fill=(18, 6, 28, 255))
    ell(d, 14.8, 13.2, 0.8, 0.8, fill=(255, 255, 255, 255))
    d.polygon(P((14, 26), (18, 26), (16, 31)), fill=gold)       # pendulum tip
    return finish(im, outline=(36, 20, 10, 255))


def dimension_lost():
    im, d = canvas()
    ell(d, 16, 16, 10.5, 10.5, fill=(92, 110, 132, 255))
    ell(d, 15, 15, 9, 9, fill=(150, 170, 192, 255))
    for k, col in enumerate([(200, 216, 232, 255), (120, 140, 164, 255), (226, 236, 246, 255)]):
        d.arc([(6 + k) * S, (8 + k * 2) * S, (26 - k) * S, (22 + k) * S], 160 + k * 40, 330 + k * 40, fill=col, width=6)
    ell(d, 12.5, 11.5, 2.4, 1.6, fill=(244, 250, 255, 255))
    # tilted orbit ring around the orb
    ring = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    r = ImageDraw.Draw(ring)
    r.ellipse([1 * S, 12 * S, 31 * S, 21 * S], outline=(184, 232, 255, 255), width=6)
    ring = ring.rotate(-22, center=(N / 2, N / 2), resample=Image.BICUBIC)
    # draw the front half of the ring over the orb
    front = Image.new("L", (N, N), 0)
    ImageDraw.Draw(front).rectangle([0, N // 2, N, N], fill=255)
    back = ring.copy()
    back.putalpha(Image.eval(back.getchannel("A"), lambda a: a // 2))
    base = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    base.alpha_composite(back)
    base.alpha_composite(im)
    base.paste(ring, (0, 0), Image.composite(ring.getchannel("A"), Image.new("L", (N, N), 0), front))
    return finish(base, outline=(22, 30, 44, 255))


def zenith_tempest():
    im, d = canvas()
    ell(d, 16, 16, 11, 11, fill=(34, 86, 150, 255))
    ell(d, 15, 15, 9.5, 9.5, fill=(66, 136, 204, 255))
    for k in range(3):                                   # wind arcs
        d.arc([(4 + k * 2) * S, (5 + k * 2) * S, (28 - k * 2) * S, (27 - k * 2) * S], 200 + k * 70, 300 + k * 70,
              fill=(232, 246, 255, 255), width=5)
    bolt = [(18, 2), (11, 15), (16, 15), (12, 29), (22, 13), (17, 13), (21, 2)]
    d.polygon(P(*bolt), fill=(255, 250, 200, 255))
    d.line(P((18.5, 4), (13, 14.2)), fill=(255, 255, 255, 255), width=3)
    return finish(im, outline=(12, 28, 52, 255))


def annihilation_maker():
    im, d = canvas()
    # a dark crystal heart with red fissures and shadow tendrils
    for a in (200, 250, 290, 340):
        r = math.radians(a)
        d.line(P((16, 18), (16 + math.cos(r) * 15, 18 + math.sin(r) * 13)), fill=(46, 30, 52, 255), width=6)
    d.polygon(P((16, 3), (26, 12), (23, 25), (16, 30), (9, 25), (6, 12)), fill=(30, 22, 36, 255))
    d.polygon(P((16, 5), (24, 12.5), (21.5, 23.5), (16, 27.5), (10.5, 23.5), (8, 12.5)), fill=(58, 44, 70, 255))
    d.polygon(P((16, 5), (24, 12.5), (16, 14)), fill=(92, 74, 110, 255))
    crack = [(16, 7), (14, 12), (17.5, 15), (14.5, 20), (16.5, 25)]
    d.line(P(*crack), fill=(255, 52, 64, 255), width=4)
    d.line(P((17.5, 15), (21, 17)), fill=(255, 52, 64, 255), width=3)
    d.line(P((14.5, 20), (11, 21.5)), fill=(255, 52, 64, 255), width=3)
    ell(d, 16, 16, 2.2, 2.2, fill=(255, 120, 110, 255))
    return finish(im, outline=(10, 6, 12, 255))


def regulus_nemea():
    im, d = canvas()
    gold, dgold, mane = (232, 176, 52, 255), (150, 98, 24, 255), (196, 120, 30, 255)
    for k in range(16):                                  # mane rays
        a = k * math.pi / 8
        d.polygon(P((16 + math.cos(a - 0.2) * 9, 16 + math.sin(a - 0.2) * 9),
                    (16 + math.cos(a) * 15, 16 + math.sin(a) * 15),
                    (16 + math.cos(a + 0.2) * 9, 16 + math.sin(a + 0.2) * 9)), fill=mane if k % 2 else dgold)
    ell(d, 16, 16, 10, 10, fill=dgold)
    ell(d, 16, 15.5, 9, 9, fill=gold)
    # stylised lion face: brow, eyes, muzzle
    d.polygon(P((9.5, 12), (16, 10), (22.5, 12), (21, 14), (16, 12.6), (11, 14)), fill=dgold)
    d.polygon(P((11, 14.2), (14, 14.2), (13.2, 15.4)), fill=(60, 30, 10, 255))
    d.polygon(P((18, 14.2), (21, 14.2), (18.8, 15.4)), fill=(60, 30, 10, 255))
    d.polygon(P((13, 18), (16, 15.5), (19, 18), (16, 22.5)), fill=(248, 214, 130, 255))
    d.polygon(P((14.6, 17.4), (17.4, 17.4), (16, 19)), fill=(70, 34, 12, 255))
    gem(d, 16, 7.2, 1.7, (230, 48, 40, 255))
    return finish(im, outline=(44, 24, 6, 255))


def twilight_healing():
    im, d = canvas()
    gold, dgold = (236, 196, 84, 255), (150, 112, 36, 255)
    for cx, cy in ((11.5, 17), (20.5, 15)):
        ell(d, cx, cy, 7.5, 7.5, fill=dgold)
        ell(d, cx - 0.4, cy - 0.4, 6.6, 6.6, fill=gold)
        ell(d, cx, cy, 4.6, 4.6, fill=(0, 0, 0, 0))
    # re-draw the overlap so the rings interlock
    d.arc([13 * S, 7.5 * S, 28 * S, 22.5 * S], 150, 215, fill=gold, width=7)
    gem(d, 11.5, 9.8, 2.4, (110, 240, 150, 255))
    gem(d, 20.5, 22.4, 2.4, (110, 240, 150, 255))
    for x, y in ((4, 5), (27, 4), (28, 27)):            # sparkles
        d.line(P((x - 2, y), (x + 2, y)), fill=(255, 255, 220, 255), width=3)
        d.line(P((x, y - 2), (x, y + 2)), fill=(255, 255, 220, 255), width=3)
    return finish(im, outline=(42, 28, 8, 255))


ICONS = {
    "boosted_gear": boosted_gear, "divine_dividing": divine_dividing, "sword_birth": sword_birth,
    "absorption_line": absorption_line, "forbidden_balor_view": forbidden_balor_view, "dimension_lost": dimension_lost,
    "zenith_tempest": zenith_tempest, "annihilation_maker": annihilation_maker, "regulus_nemea": regulus_nemea,
    "twilight_healing": twilight_healing,
}

if __name__ == "__main__":
    out = sys.argv[1]
    for name, fn in ICONS.items():
        fn().save(f"{out}/{name}.png")
    print("wrote", len(ICONS), "icons")
