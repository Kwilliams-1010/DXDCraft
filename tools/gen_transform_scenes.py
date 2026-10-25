"""Generates the Balance Breaker / Juggernaut Drive transformation scenes (original choreography).

Outputs, for every scene:
  src/main/resources/assets/dxdcraft/animations/transform/<name>.animation.json   (played by the mod)
  packaging/blockbench/transform_<name>.bbmodel                                    (Blockbench project to edit)

Editing in Blockbench: open the .bbmodel, edit the animation, then Animation > Export Animations and save
over the matching .animation.json (or put it in a resource pack at the same path). F3+T reloads it in game.

Units are Blockbench's: degrees, pixels, y up. +x is the player's right, +z is behind the player.
Usage: python3 tools/gen_transform_scenes.py
"""
import base64
import io
import json
import math
import os
import uuid as uuidlib
from collections import defaultdict

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ANIM_DIR = os.path.join(ROOT, "src/main/resources/assets/dxdcraft/animations/transform")
BB_DIR = os.path.join(ROOT, "packaging/blockbench")
TEX = os.path.join(ROOT, "src/main/resources/assets/dxdcraft/textures")

ARMOR = ["armor_head", "armor_body", "armor_right_arm", "armor_left_arm", "armor_right_leg", "armor_left_leg"]
# Centres of each armor piece (Blockbench coords, feet at y=0)
CENTER = {"armor_head": (0, 28, 0), "armor_body": (0, 18, 0), "armor_right_arm": (6, 20, 0), "armor_left_arm": (-6, 20, 0),
          "armor_right_leg": (2, 6, 0), "armor_left_leg": (-2, 6, 0)}
LOCK_FX = {"armor_head": "lock_head", "armor_body": "lock_body", "armor_right_arm": "lock_arms", "armor_left_arm": "lock_arms",
           "armor_right_leg": "lock_legs", "armor_left_leg": "lock_legs"}
EXTRAS = ["horn_left", "horn_right", "crest", "halo", "core", "wing_left", "wing_right", "tail",
          "pauldron_left", "pauldron_right", "claws_left", "claws_right"]


class Scene:
    def __init__(self, name, length):
        self.name, self.length = name, length
        self.ch = defaultdict(lambda: defaultdict(dict))  # bone -> channel -> {time: (vec, lerp)}
        self.sounds, self.particles = [], []
        self.glow_spikes = []

    def key(self, bone, channel, t, v, lerp="catmullrom"):
        t = round(max(0.0, min(self.length, t)), 3)
        if isinstance(v, (int, float)):
            v = (v, v, v)
        self.ch[bone][channel][t] = (tuple(round(float(x), 3) for x in v), lerp)

    def sound(self, t, sid, pitch=1.0, vol=1.0):
        spec = sid if ":" in sid else "minecraft:" + sid
        if pitch != 1.0 or vol != 1.0:
            spec += f"|{pitch}" + (f"|{vol}" if vol != 1.0 else "")
        self.sounds.append((round(t, 3), spec))

    def fx(self, t, effect):
        self.particles.append((round(t, 3), effect))

    # ---------------------------------------------------------------- building blocks

    def hide_all(self):
        """Every armor piece and add-on starts invisible (scale 0)."""
        for b in ARMOR + EXTRAS:
            self.key(b, "scale", 0.0, 0, "linear")

    def piece(self, bone, t_lock, start=None, spin=(0, 0, 0), travel=0.35, from_point=None, path=None,
              overshoot=0.18, sound=("item.armor.equip_netherite", 1.0), particle="dust", scale_from=0.3):
        """Armor piece flies from an offset (or a point / along a path) and locks on at t_lock."""
        t0 = max(0.02, t_lock - travel)
        if from_point is not None:
            c = CENTER[bone]
            start = (from_point[0] - c[0], from_point[1] - c[1], from_point[2] - c[2])
        start = start or (0, 0, 0)
        self.key(bone, "scale", t0 - 0.01, 0, "linear")
        self.key(bone, "scale", t0, scale_from, "linear")
        self.key(bone, "scale", t_lock, 1 + overshoot, "linear")
        self.key(bone, "scale", t_lock + 0.12, 1, "linear")
        if path:
            for i, (dt, off) in enumerate(path):
                self.key(bone, "position", t0 + dt, off)
        else:
            self.key(bone, "position", t0, start)
        self.key(bone, "position", t_lock, (0, 0, 0))
        self.key(bone, "rotation", t0, spin)
        self.key(bone, "rotation", t_lock, (0, 0, 0))
        if sound:
            self.sound(t_lock, sound[0], sound[1])
        self.fx(t_lock, f"{LOCK_FX[bone]}:{particle}")
        self.glow_spikes.append(t_lock)

    def pop(self, bone, t, grow_axis=None, overshoot=0.3, dur=0.25):
        """Add-on appears by growing in (optionally along one axis only)."""
        start = [0, 0, 0] if grow_axis is None else [1, 1, 1]
        if grow_axis is not None:
            start["xyz".index(grow_axis)] = 0
        self.key(bone, "scale", t - dur - 0.01, 0, "linear")
        self.key(bone, "scale", t - dur, start, "linear")
        self.key(bone, "scale", t, 1 + overshoot)
        self.key(bone, "scale", t + 0.15, 1)

    def wings(self, t_open, dur=0.3, energy=False, flare=True, overshoot=12):
        """Wings unfold from folded on the back (rotation y opens, z raises)."""
        for side, s in (("wing_left", -1), ("wing_right", 1)):
            self.key(side, "scale", t_open - dur - 0.01, 0, "linear")
            self.key(side, "scale", t_open - dur, 0.4 if energy else 0.7, "linear")
            self.key(side, "scale", t_open, 1)
            self.key(side, "rotation", t_open - dur, (0, 0, 0))
            self.key(side, "rotation", t_open, (0, s * (60 + overshoot), s * 10))
            self.key(side, "rotation", t_open + 0.15, (0, s * 58, s * 8))
            if flare:
                self.key(side, "rotation", t_open + 0.35, (0, s * 62, s * 30))
                self.key(side, "rotation", t_open + 0.6, (0, s * 58, s * 12))
            self.key(side, "rotation", self.length - 0.25, (0, s * 58, s * 12))
            self.key(side, "rotation", self.length, (0, s * 55, s * 10))

    def pose(self, t, **bones):
        """Player pose keyframe: bone=(x,y,z) rotation in degrees (player_ prefix added)."""
        for b, v in bones.items():
            self.key("player_" + b, "rotation", t, v)

    def rest(self, t):
        self.pose(t, head=(0, 0, 0), body=(0, 0, 0), right_arm=(0, 0, 0), left_arm=(0, 0, 0), right_leg=(0, 0, 0), left_leg=(0, 0, 0))

    def lift(self, t, y, lerp="catmullrom"):
        self.key("player_root", "position", t, (0, y, 0), lerp)

    def shake(self, t0, t1, amp=0.8, step=0.05):
        t, i = t0, 0
        while t < t1:
            self.key("player_root", "position", t, ((amp if i % 2 else -amp), self._lift_at(t), (amp * 0.5 if i % 3 else -amp * 0.5)), "linear")
            t += step
            i += 1

    def _lift_at(self, t):
        keys = sorted(self.ch["player_root"]["position"].items())
        y = 0
        for kt, (v, _) in keys:
            if kt <= t:
                y = v[1]
        return y

    def finish(self, glow_base=0.5):
        # glow: dim while building, flashing as each piece locks on, full at the end
        self.key("fx_glow", "scale", 0.0, glow_base, "linear")
        for t in sorted(set(self.glow_spikes)):
            self.key("fx_glow", "scale", t - 0.04, glow_base, "linear")
            self.key("fx_glow", "scale", t, 1.0, "linear")
            self.key("fx_glow", "scale", t + 0.18, glow_base, "linear")
        last = max(self.glow_spikes + [0.5])
        self.key("fx_glow", "scale", last + 0.3, 1.0, "linear")
        self.key("fx_glow", "scale", self.length, 1.0, "linear")

    # ---------------------------------------------------------------- output

    def bedrock(self):
        bones = {}
        for bone, chans in self.ch.items():
            out = {}
            for cname, keys in chans.items():
                kd = {}
                for t, (v, lerp) in sorted(keys.items()):
                    tk = f"{t:.3f}".rstrip("0").rstrip(".") if t else "0.0"
                    if "." not in tk:
                        tk += ".0"
                    kd[tk] = {"post": list(v), "lerp_mode": lerp} if lerp != "linear" else list(v)
                out[cname] = kd
            bones[bone] = out
        anim = {"animation_length": self.length, "bones": bones}
        if self.sounds:
            anim["sound_effects"] = group_events(self.sounds, "effect")
        if self.particles:
            anim["particle_effects"] = group_events(self.particles, "effect")
        return {"format_version": "1.8.0", "animations": {self.name: anim}}


def group_events(events, field):
    by_time = defaultdict(list)
    for t, e in sorted(events):
        by_time[f"{t:.3f}".rstrip("0").rstrip(".") if t else "0.0"].append({field: e})
    return {k: (v[0] if len(v) == 1 else v) for k, v in by_time.items()}


def orbit_path(radius, turns, rise_from, travel, start_angle, steps=8, center_y=0):
    """Positions circling the player while closing in (for orbiting pieces)."""
    pts = []
    for i in range(steps):
        f = i / steps
        a = math.radians(start_angle + turns * 360 * f)
        r = radius * (1 - f)
        pts.append((travel * f, (math.cos(a) * r, rise_from * (1 - f) + center_y, math.sin(a) * r)))
    return pts


# ==================================================================== the scenes

def common_open(s, sound=("block.beacon.power_select", 0.8), particle="dust", lift=5, brace=True):
    s.hide_all()
    s.sound(0.0, *sound)
    s.fx(0.0, f"implode:{particle}")
    s.fx(0.2, f"spiral:{particle}")
    s.rest(0.0)
    if brace:
        s.pose(0.45, head=(-15, 0, 0), body=(-10, 0, 0), right_arm=(-18, 0, 18), left_arm=(-18, 0, -18),
               right_leg=(0, 0, 6), left_leg=(0, 0, -6))
    s.lift(0.0, 0)
    s.lift(0.6, lift)


def common_close(s, t_finale, finale_pose, lift=5, settle=0.3):
    s.pose(t_finale, **finale_pose)
    s.pose(s.length - settle, **finale_pose)
    s.rest(s.length)
    s.lift(t_finale, lift + 1)
    s.lift(s.length - settle, lift)
    s.lift(s.length, 0)


def boosted_gear():
    s = Scene("transform.boosted_gear", 3.0)
    common_open(s, ("block.respawn_anchor.charge", 0.7), "dust")
    gauntlet = (-7, 12, -3)
    for bone, t in (("armor_left_arm", 0.75), ("armor_right_arm", 0.95), ("armor_body", 1.15),
                    ("armor_right_leg", 1.35), ("armor_left_leg", 1.4), ("armor_head", 1.6)):
        s.piece(bone, t, from_point=gauntlet, spin=(0, 0, 200 if "right" in bone else -200), travel=0.32,
                sound=("item.armor.equip_netherite", 0.85 + 0.05 * t), particle="flame")
    s.pop("pauldron_left", 1.75); s.pop("pauldron_right", 1.8)
    s.pop("claws_left", 1.85, "y"); s.pop("claws_right", 1.85, "y")
    s.pop("horn_left", 1.95, "y"); s.pop("horn_right", 1.95, "y"); s.sound(1.95, "entity.blaze.shoot", 0.6)
    s.pop("tail", 2.0); s.pop("core", 1.2)
    s.wings(2.2, dur=0.2); s.sound(2.2, "entity.ender_dragon.flap", 0.9)
    s.sound(2.3, "entity.ender_dragon.growl", 1.0, 0.8)
    s.fx(2.3, "flash:flame"); s.fx(2.32, "shockwave:flame")
    common_close(s, 2.3, dict(head=(15, 0, 0), body=(5, 0, 0), left_arm=(165, 0, -10), right_arm=(-10, 0, 30),
                              right_leg=(0, 0, 8), left_leg=(0, 0, -8)))
    s.finish()
    return s


def divine_dividing():
    s = Scene("transform.divine_dividing", 3.0)
    common_open(s, ("block.beacon.activate", 1.4), "end_rod", lift=8)
    s.fx(0.2, "rain:end_rod"); s.fx(0.6, "pillar:end_rod")
    for bone, t in (("armor_head", 0.95), ("armor_body", 1.15), ("armor_right_arm", 1.35), ("armor_left_arm", 1.35),
                    ("armor_right_leg", 1.55), ("armor_left_leg", 1.55)):
        s.piece(bone, t, start=(0, 34, 0), spin=(0, 90, 0), travel=0.45, sound=("block.amethyst_block.chime", 1.3), particle="spark")
    s.pop("pauldron_left", 1.65); s.pop("pauldron_right", 1.65); s.pop("crest", 1.75, "z"); s.pop("core", 1.2)
    s.wings(2.3, dur=0.65, energy=True); s.sound(1.8, "entity.phantom.flap", 1.5); s.sound(2.3, "entity.phantom.flap", 1.2)
    s.sound(2.35, "block.beacon.power_select", 1.6); s.sound(2.35, "item.totem.use", 1.4, 0.5)
    s.fx(2.35, "flash:spark"); s.fx(2.37, "ring:end_rod")
    common_close(s, 2.35, dict(head=(15, 0, 0), body=(6, 0, 0), right_arm=(10, 0, 80), left_arm=(10, 0, -80)), lift=8)
    s.finish()
    return s


def sword_birth():
    s = Scene("transform.sword_birth", 3.0)
    common_open(s, ("item.trident.return", 0.7), "witch")
    order = (("armor_right_arm", 0.95, 0), ("armor_left_arm", 1.05, 180), ("armor_right_leg", 1.25, 90),
             ("armor_left_leg", 1.3, 270), ("armor_body", 1.45, 45), ("armor_head", 1.65, 225))
    for bone, t, a in order:
        path = orbit_path(radius=18, turns=0.75, rise_from=4, travel=0.55, start_angle=a)
        s.piece(bone, t, path=path, spin=(0, 360, 0), travel=0.55, sound=("item.armor.equip_iron", 1.1), particle="enchant")
        s.sound(t - 0.5, "entity.player.attack.sweep", 1.2)
        s.fx(t - 0.3, "sparks:crit")
    s.pop("crest", 1.8, "z"); s.pop("pauldron_left", 1.85); s.pop("pauldron_right", 1.85); s.pop("core", 1.5)
    s.sound(2.3, "item.trident.riptide_1", 0.9); s.sound(2.3, "block.anvil.land", 1.6, 0.6)
    s.fx(2.3, "flash:witch"); s.fx(2.32, "ring:crit")
    common_close(s, 2.3, dict(head=(0, -15, 0), body=(0, -15, 0), right_arm=(90, 0, -5), left_arm=(-20, 0, -35),
                              right_leg=(-10, 0, 0), left_leg=(10, 0, 0)))
    s.finish()
    return s


def absorption_line():
    s = Scene("transform.absorption_line", 3.0)
    common_open(s, ("block.sculk.spread", 0.8), "dust")
    s.fx(0.3, "ring:dust"); s.fx(0.8, "ring:dust")
    for i, (bone, t) in enumerate((("armor_right_leg", 0.85), ("armor_left_leg", 0.9), ("armor_body", 1.1),
                                   ("armor_right_arm", 1.35), ("armor_left_arm", 1.4), ("armor_head", 1.6))):
        wiggle = 25 if i % 2 else -25
        path = [(0.0, (0, -30, 0)), (0.15, (wiggle * 0.2, -18, 0)), (0.3, (-wiggle * 0.2, -7, 0))]
        s.piece(bone, t, path=path, spin=(0, 0, wiggle), travel=0.42, sound=("entity.slime.squish", 0.7), particle="dust")
        s.sound(t - 0.4, "block.sculk_vein.place", 1.0)
    s.pop("claws_left", 1.8, "y"); s.pop("claws_right", 1.8, "y"); s.pop("pauldron_left", 1.9); s.pop("pauldron_right", 1.9)
    s.pop("core", 1.15)
    s.fx(2.2, "sparks:glow")
    s.sound(2.3, "block.chorus_flower.grow", 0.6); s.sound(2.3, "block.sculk_shrieker.shriek", 1.4, 0.5)
    s.fx(2.3, "burst:dust"); s.fx(2.32, "ring:dust")
    common_close(s, 2.3, dict(head=(15, 0, 0), right_arm=(20, 0, 45), left_arm=(20, 0, -45), right_leg=(0, 0, 8), left_leg=(0, 0, -8)))
    s.finish()
    return s


def forbidden_balor_view():
    s = Scene("transform.forbidden_balor_view", 3.0)
    common_open(s, ("block.bell.use", 0.6), "portal")
    # Pieces appear frozen around the player at 0.6 and hang motionless (time has stopped)...
    for i, bone in enumerate(ARMOR):
        a = math.radians(i * 60 + 20)
        c = CENTER[bone]
        frozen = (math.cos(a) * 16, 6 + (i % 3) * 5, math.sin(a) * 16)
        s.key(bone, "scale", 0.59, 0, "linear")
        s.key(bone, "scale", 0.6, 1, "step")
        s.key(bone, "position", 0.6, frozen, "step")
        s.key(bone, "rotation", 0.6, (i * 40 - 100, i * 70, i * 25 - 60), "step")
        # ...then all snap on in the same instant.
        s.key(bone, "position", 1.5, (0, 0, 0), "linear")
        s.key(bone, "rotation", 1.5, (0, 0, 0), "linear")
        s.key(bone, "scale", 1.5, 1.15, "linear")
        s.key(bone, "scale", 1.62, 1, "linear")
        s.fx(1.5, f"{LOCK_FX[bone]}:witch")
    s.glow_spikes += [0.6, 1.5]
    s.sound(0.6, "block.bell.resonate", 0.5); s.fx(0.6, "ring:witch")
    s.sound(1.5, "entity.illusioner.cast_spell", 0.8); s.sound(1.5, "item.armor.equip_netherite", 1.4)
    s.fx(1.5, "flash:witch")
    s.pop("halo", 1.75); s.pop("crest", 1.8, "z"); s.pop("pauldron_left", 1.85); s.pop("pauldron_right", 1.85); s.pop("core", 1.55)
    s.key("halo", "rotation", 1.6, (0, 0, 0)); s.key("halo", "rotation", 2.4, (0, 180, 0))
    s.sound(2.3, "block.bell.use", 1.3); s.sound(2.32, "block.end_portal_frame.fill", 0.5)
    s.fx(2.3, "ring:portal")
    common_close(s, 2.3, dict(head=(5, 0, -8), right_arm=(100, 0, -10), left_arm=(0, 0, -12)))
    s.finish()
    return s


def dimension_lost():
    s = Scene("transform.dimension_lost", 3.0)
    common_open(s, ("entity.enderman.teleport", 0.6), "snow")
    s.sound(0.5, "block.respawn_anchor.charge", 1.4)
    s.fx(0.5, "spiral:snow"); s.fx(1.0, "rain:snow")
    for bone, t in (("armor_body", 1.0), ("armor_head", 1.2), ("armor_right_arm", 1.35), ("armor_left_arm", 1.4),
                    ("armor_right_leg", 1.55), ("armor_left_leg", 1.6)):
        s.key(bone, "scale", t - 0.61, 0, "linear")
        s.key(bone, "scale", t - 0.6, 1.7, "linear")   # appears oversized and ghostly...
        s.key(bone, "scale", t, 1.0)                  # ...and condenses into place
        s.fx(t, f"{LOCK_FX[bone]}:cloud")
        s.sound(t, "block.amethyst_block.resonate", 0.7)
        s.glow_spikes.append(t)
    s.key("fx_alpha", "scale", 0.0, 0.15, "linear"); s.key("fx_alpha", "scale", 1.9, 1.0)
    s.pop("pauldron_left", 1.75); s.pop("pauldron_right", 1.75); s.pop("crest", 1.9, "z"); s.pop("core", 1.1)
    s.wings(2.3, dur=0.55, energy=True)
    s.sound(2.35, "block.conduit.activate", 1.0); s.fx(2.35, "ring:snow"); s.fx(2.36, "flash:dust")
    common_close(s, 2.35, dict(head=(10, 0, 0), right_arm=(15, 0, 40), left_arm=(15, 0, -40)))
    s.finish()
    return s


def zenith_tempest():
    s = Scene("transform.zenith_tempest", 3.0)
    common_open(s, ("entity.wind_charge.wind_burst", 0.8), "snow", lift=10)
    s.key("player_root", "rotation", 0.4, (0, 0, 0)); s.key("player_root", "rotation", 1.5, (0, 360, 0))
    for t in (0.2, 0.7):
        s.fx(t, "spiral:snow")
    s.sound(0.5, "item.trident.riptide_1", 1.2); s.sound(0.9, "entity.wind_charge.wind_burst", 1.0)
    for bone, t, a in (("armor_right_leg", 0.95, 0), ("armor_left_leg", 1.0, 180), ("armor_body", 1.1, 90),
                       ("armor_right_arm", 1.22, 270), ("armor_left_arm", 1.27, 45), ("armor_head", 1.4, 225)):
        path = orbit_path(radius=20, turns=2.0, rise_from=-10, travel=0.8, start_angle=a, steps=12)
        s.piece(bone, t, path=path, spin=(0, 720, 0), travel=0.8, sound=("item.armor.equip_chain", 1.3), particle="spark")
    s.pop("crest", 1.55, "z"); s.pop("core", 1.15)
    s.wings(1.7, dur=0.15, energy=True, overshoot=20); s.sound(1.7, "entity.wind_charge.wind_burst", 1.2)
    s.fx(1.72, "sparks:spark")
    s.sound(2.3, "item.trident.thunder", 1.0, 0.6); s.sound(2.3, "entity.lightning_bolt.thunder", 1.5, 0.3)
    s.fx(2.3, "flash:spark"); s.fx(2.32, "shockwave:snow")
    common_close(s, 2.3, dict(head=(20, 0, 0), right_arm=(160, 0, 25), left_arm=(160, 0, -25), right_leg=(0, 0, 5), left_leg=(0, 0, -5)), lift=10)
    s.finish()
    return s


def annihilation_maker():
    s = Scene("transform.annihilation_maker", 3.0)
    common_open(s, ("entity.warden.emerge", 0.8, 0.6), "smoke", lift=0)
    s.fx(0.2, "ring:smoke"); s.fx(0.5, "implode:ink"); s.fx(0.9, "spiral:smoke")
    s.sound(0.9, "entity.wither.ambient", 0.6)
    for bone, t in (("armor_right_leg", 0.95), ("armor_left_leg", 1.0), ("armor_right_arm", 1.2), ("armor_left_arm", 1.25),
                    ("armor_body", 1.45), ("armor_head", 1.7)):
        s.piece(bone, t, start=(0, -26, 0), travel=0.6, sound=("item.armor.equip_netherite", 0.6), particle="ink", overshoot=0.08)
    s.key("fx_alpha", "scale", 0.0, 0.45, "linear"); s.key("fx_alpha", "scale", 1.8, 1.0)
    s.pop("horn_left", 1.95, "y", 0.4, 0.35); s.pop("horn_right", 1.95, "y", 0.4, 0.35); s.sound(1.95, "entity.evoker.prepare_summon", 0.8)
    s.pop("claws_left", 2.0, "y"); s.pop("claws_right", 2.0, "y"); s.pop("tail", 2.05); s.pop("core", 1.5)
    s.pop("pauldron_left", 1.8); s.pop("pauldron_right", 1.8)
    s.wings(2.2, dur=0.25)
    s.sound(2.3, "entity.wither.spawn", 1.2, 0.35); s.sound(2.3, "entity.warden.roar", 1.0, 0.5)
    s.fx(2.3, "shockwave:smoke"); s.fx(2.31, "flash:dust")
    common_close(s, 2.3, dict(head=(15, 0, 0), body=(-20, 0, 0), right_arm=(30, 0, 50), left_arm=(30, 0, -50),
                              right_leg=(-10, 0, 8), left_leg=(10, 0, -8)), lift=0)
    s.finish()
    return s


def regulus_nemea():
    s = Scene("transform.regulus_nemea", 3.0)
    common_open(s, ("entity.ravager.ambient", 0.8), "dust", lift=2)
    for bone, t in (("armor_body", 0.85), ("armor_right_arm", 1.05), ("armor_left_arm", 1.1), ("armor_right_leg", 1.25),
                    ("armor_left_leg", 1.3), ("armor_head", 1.5)):
        s.piece(bone, t, start=(0, 0, -26), spin=(-90, 0, 0), travel=0.2, sound=("block.anvil.land", 0.8), particle="flame",
                overshoot=0.25)
        s.sound(t, "item.armor.equip_netherite", 1.0)
        s.shake(t, t + 0.12, amp=1.2, step=0.04)
    s.pop("pauldron_left", 1.65); s.pop("pauldron_right", 1.65); s.pop("crest", 1.72, None, 0.4)
    s.pop("claws_left", 1.8, "y"); s.pop("claws_right", 1.8, "y"); s.pop("tail", 1.9); s.pop("core", 0.9)
    s.sound(2.2, "entity.ravager.roar", 1.0); s.fx(2.2, "flash:flame"); s.fx(2.22, "shockwave:dust")
    s.shake(2.2, 2.6, amp=1.0, step=0.05)
    common_close(s, 2.2, dict(head=(25, 0, 0), body=(-15, 0, 0), right_arm=(40, 0, 60), left_arm=(40, 0, -60),
                              right_leg=(-12, 0, 8), left_leg=(12, 0, -8)), lift=2)
    s.finish()
    return s


def twilight_healing():
    s = Scene("transform.twilight_healing", 3.0)
    common_open(s, ("block.amethyst_block.resonate", 1.2), "cherry", lift=6)
    s.sound(0.5, "entity.allay.ambient_with_item", 1.0)
    # the halo descends first
    s.key("halo", "scale", 0.29, 0, "linear"); s.key("halo", "scale", 0.3, 1, "linear")
    s.key("halo", "position", 0.3, (0, 40, 0)); s.key("halo", "position", 1.2, (0, 0, 0))
    s.key("halo", "rotation", 0.3, (0, 0, 0)); s.key("halo", "rotation", 1.2, (0, 360, 0))
    for i, (bone, t) in enumerate((("armor_body", 1.15), ("armor_right_arm", 1.35), ("armor_left_arm", 1.35),
                                   ("armor_right_leg", 1.55), ("armor_left_leg", 1.55), ("armor_head", 1.75))):
        c = CENTER[bone]
        out = (c[0] * 1.5, 4, -6)
        s.piece(bone, t, start=out, travel=0.5, sound=("block.amethyst_block.chime", 1.5), particle="end_rod", overshoot=0.08,
                scale_from=0.0)
    s.pop("core", 1.2)
    s.wings(2.35, dur=0.5, energy=True, flare=False)
    s.sound(2.4, "block.beacon.activate", 1.2); s.sound(2.4, "entity.player.levelup", 1.4, 0.5)
    s.fx(2.4, "flash:end_rod"); s.fx(2.42, "ring:cherry")
    common_close(s, 2.4, dict(head=(10, 0, 0), right_arm=(30, 0, 50), left_arm=(30, 0, -50)), lift=6)
    s.finish()
    return s


def juggernaut(red):
    s = Scene("transform.juggernaut_" + ("red" if red else "white"), 3.5)
    p = "flame" if red else "spark"
    pitch = 1.0 if red else 1.3
    s.hide_all()
    s.rest(0.0)
    s.sound(0.0, "entity.ender_dragon.growl", 0.5 * pitch, 0.6)
    s.fx(0.0, f"implode:{p}")
    # hunched over, trembling
    s.pose(0.35, head=(-20, 0, 0), body=(-35, 0, 0), right_arm=(-25, 0, 20), left_arm=(-25, 0, -20), right_leg=(-15, 0, 4), left_leg=(15, 0, -4))
    s.lift(0.0, 0)
    s.shake(0.3, 1.3, amp=0.7, step=0.05)
    for t in (0.3, 0.6, 0.9):
        s.sound(t, "entity.warden.heartbeat", 0.8 * pitch); s.fx(t, f"sparks:{p}")
    # the armor erupts: oversized, convulsing, then crushing down onto the body
    for i, bone in enumerate(ARMOR):
        s.key(bone, "scale", 0.79, 0, "linear")
        s.key(bone, "scale", 0.8, 1.8, "linear")
        for j, tt in enumerate((0.9, 1.0, 1.1, 1.2)):
            s.key(bone, "scale", tt, 1.5 - 0.1 * j + (0.08 if (i + j) % 2 else -0.08), "linear")
            s.key(bone, "rotation", tt, ((-6 if (i + j) % 2 else 6), 0, (5 if j % 2 else -5)), "linear")
        s.key(bone, "scale", 1.35, 1.0)
        s.key(bone, "rotation", 1.35, (0, 0, 0))
        s.fx(1.35, f"{LOCK_FX[bone]}:{p}")
    s.glow_spikes += [0.8, 1.35]
    s.sound(0.8, "entity.generic.explode", 0.7, 0.6); s.fx(0.8, f"burst:{p}")
    s.sound(1.35, "item.armor.equip_netherite", 0.5)
    s.pop("horn_left", 1.45, "y", 0.5); s.pop("horn_right", 1.45, "y", 0.5); s.pop("claws_left", 1.5, "y"); s.pop("claws_right", 1.5, "y")
    s.pop("pauldron_left", 1.55); s.pop("pauldron_right", 1.55); s.pop("tail", 1.6); s.pop("crest", 1.6, "z"); s.pop("core", 0.85)
    s.wings(1.9, dur=0.12, energy=not red, overshoot=25)
    s.sound(1.9, "entity.ender_dragon.flap", 0.6 * pitch)
    s.fx(1.9, f"shockwave:{p}"); s.fx(1.92, f"flash:{p}"); s.fx(2.0, f"pillar:{p}")
    s.sound(2.0, "entity.ender_dragon.growl", 0.8 * pitch); s.sound(2.0, "entity.lightning_bolt.thunder", 0.8, 0.4)
    # head thrown back: the roar
    roar = dict(head=(40, 0, 0), body=(15, 0, 0), right_arm=(20, 0, 70), left_arm=(20, 0, -70), right_leg=(0, 0, 10), left_leg=(0, 0, -10))
    s.pose(2.0, **roar)
    s.lift(2.0, 4)
    s.shake(2.0, 2.6, amp=0.6, step=0.05)
    s.pose(3.2, **roar)
    s.lift(3.2, 4)
    s.rest(3.5)
    s.lift(3.5, 0)
    s.finish(glow_base=0.7)
    return s


SCENES = {
    "boosted_gear": boosted_gear, "divine_dividing": divine_dividing, "sword_birth": sword_birth,
    "absorption_line": absorption_line, "forbidden_balor_view": forbidden_balor_view, "dimension_lost": dimension_lost,
    "zenith_tempest": zenith_tempest, "annihilation_maker": annihilation_maker, "regulus_nemea": regulus_nemea,
    "twilight_healing": twilight_healing, "juggernaut_red": lambda: juggernaut(True), "juggernaut_white": lambda: juggernaut(False),
}


# ==================================================================== Blockbench project (.bbmodel)

def uid():
    return str(uuidlib.uuid4())


def png_data(img):
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def mannequin_skin():
    """A plain grey mannequin skin (64x64 player layout) so the armor reads clearly in Blockbench."""
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()
    regions = [(0, 0, 32, 16, (190, 170, 150)), (16, 16, 40, 32, (90, 100, 130)), (40, 16, 56, 32, (180, 160, 140)),
               (32, 48, 48, 64, (180, 160, 140)), (0, 16, 16, 32, (60, 60, 80)), (16, 48, 32, 64, (60, 60, 80))]
    for x0, y0, x1, y1, c in regions:
        for y in range(y0, y1):
            for x in range(x0, x1):
                f = 1.0 - 0.15 * ((y - y0) / max(1, y1 - y0))
                px[x, y] = (int(c[0] * f), int(c[1] * f), int(c[2] * f), 255)
    for x, y in ((10, 12), (13, 12)):
        px[x, y] = (40, 40, 60, 255)
    return im


def pad64(img):
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    out.paste(img, (0, 0))
    return out


def box_faces(u, v, w, h, d, tex, mirror=False):
    f = {"north": [u + d, v + d, u + d + w, v + d + h], "east": [u, v + d, u + d, v + d + h],
         "south": [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], "west": [u + d + w, v + d, u + 2 * d + w, v + d + h],
         "up": [u + d + w, v + d, u + d, v], "down": [u + d + 2 * w, v, u + d + w, v + d]}
    if mirror:
        f["east"], f["west"] = f["west"], f["east"]
    return {k: {"uv": val, "texture": tex} for k, val in f.items()}


class BB:
    """Builds Blockbench elements/groups from Java ModelPart definitions (Java -> Blockbench axes)."""

    def __init__(self):
        self.elements, self.groups = [], {}

    def group(self, name, java_pivot, java_rot=(0, 0, 0)):
        g = {"name": name, "origin": [-java_pivot[0], 24 - java_pivot[1], java_pivot[2]],
             "rotation": [-math.degrees(java_rot[0]), -math.degrees(java_rot[1]), math.degrees(java_rot[2])],
             "color": 0, "uuid": uid(), "export": True, "mirror_uv": False, "isOpen": False, "locked": False,
             "visibility": True, "autouv": 0, "children": []}
        g["rotation"] = [round(a, 3) for a in g["rotation"]]
        self.groups[name] = g
        return g

    def cube(self, group, name, java_pivot, box, uv, tex, inflate=0.0, mirror=False):
        bx, by, bz, w, h, d = box
        x0, y0, z0 = java_pivot[0] + bx, java_pivot[1] + by, java_pivot[2] + bz
        e = {"name": name, "box_uv": True, "rescale": False, "locked": False, "render_order": "default",
             "allow_mirror_modeling": True, "from": [-(x0 + w), 24 - (y0 + h), z0], "to": [-x0, 24 - y0, z0 + d],
             "autouv": 0, "color": 0, "inflate": inflate, "origin": list(group["origin"]), "uv_offset": list(uv),
             "mirror_uv": mirror, "faces": box_faces(uv[0], uv[1], w, h, d, tex, mirror), "type": "cube", "uuid": uid()}
        self.elements.append(e)
        group["children"].append(e["uuid"])


def build_bbmodel(scene_key, scene):
    jug = scene_key.startswith("juggernaut")
    armor_name = scene_key if jug else "bb_" + scene_key
    armor_tex = pad64(Image.open(f"{TEX}/armor/{armor_name}.png").convert("RGBA"))
    extras_tex = Image.open(f"{TEX}/armor/gear_extras.png").convert("RGBA")
    inflate = 1.45 if jug else 1.1
    b = BB()
    root = b.group("player_root", (0, 24, 0))
    parts = {  # name: (pivot, box, skin uv, armor uv, mirror)
        "head": ((0, 0, 0), (-4, -8, -4, 8, 8, 8), (0, 0), (0, 0), False),
        "body": ((0, 0, 0), (-4, 0, -2, 8, 12, 4), (16, 16), (16, 16), False),
        "right_arm": ((-5, 2, 0), (-3, -2, -2, 4, 12, 4), (40, 16), (40, 16), False),
        "left_arm": ((5, 2, 0), (-1, -2, -2, 4, 12, 4), (32, 48), (40, 16), True),
        "right_leg": ((-1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (0, 16), (0, 16), False),
        "left_leg": ((1.9, 12, 0), (-2, 0, -2, 4, 12, 4), (16, 48), (0, 16), True),
    }
    for name, (pivot, box, skin_uv, armor_uv, mirror) in parts.items():
        pg = b.group("player_" + name, pivot)
        root["children"].append(pg)
        b.cube(pg, name, pivot, box, skin_uv, 0)
        ag = b.group("armor_" + name, pivot)
        pg["children"].append(ag)
        b.cube(ag, "armor_" + name, pivot, box, armor_uv, 1, inflate=inflate, mirror=mirror)

    def extra(parent, name, pivot, rot, cubes, mirror=False):
        g = b.group(name, pivot, rot)
        b.groups["player_" + parent]["children"].append(g)
        for box, uv in cubes:
            b.cube(g, name, pivot, box, uv, 2, mirror=mirror)

    extra("head", "horn_left", (3.2, -8.4, -1.5), (-0.75, 0, 0.35), [((-1, -7, -1, 2, 7, 2), (0, 0)), ((-0.5, -10, -0.5, 1, 3, 1), (8, 0))])
    extra("head", "horn_right", (-3.2, -8.4, -1.5), (-0.75, 0, -0.35), [((-1, -7, -1, 2, 7, 2), (0, 0)), ((-0.5, -10, -0.5, 1, 3, 1), (8, 0))], True)
    extra("head", "crest", (0, -9, 0.5), (0.15, 0, 0), [((-0.5, -3.5, -6, 1, 4, 11), (12, 0))])
    extra("head", "halo", (0, -13.5, 0), (0, 0, 0), [((-5, 0, -5, 10, 0, 10), (24, 48))])
    extra("body", "core", (0, 0, 0), (0, 0, 0), [((-2.5, 2, 3, 5, 5, 2), (36, 0))])
    extra("body", "wing_left", (1.5, 2, 2.6), (0, -1.35, -0.15), [((0, -1, -0.5, 15, 2, 1), (0, 26)), ((0, 1, 0, 15, 13, 0), (0, 32))])
    extra("body", "wing_right", (-1.5, 2, 2.6), (0, 1.35, 0.15), [((-15, -1, -0.5, 15, 2, 1), (0, 26)), ((-15, 1, 0, 15, 13, 0), (0, 32))], True)
    extra("body", "tail", (0, 10.5, 2.5), (-0.55, 0, 0), [((-1.5, -1.5, 0, 3, 3, 8), (40, 8)), ((-1, -1, 8, 2, 2, 6), (40, 20))])
    extra("right_arm", "pauldron_right", (-5, 2, 0), (0, 0, 0.18), [((-5, -4, -3.5, 7, 4, 7), (0, 10)), ((-5.5, -5.5, -0.5, 3, 2, 1), (28, 10))])
    extra("left_arm", "pauldron_left", (5, 2, 0), (0, 0, -0.18), [((-2, -4, -3.5, 7, 4, 7), (0, 10)), ((2.5, -5.5, -0.5, 3, 2, 1), (28, 10))], True)
    extra("right_arm", "claws_right", (-5, 2, 0), (0, 0, 0), [((-2.6, 10.5, -2.8, 1, 4, 1), (28, 14)), ((-1, 10.5, -2.8, 1, 4, 1), (28, 14)), ((0.6, 10.5, -2.8, 1, 4, 1), (28, 14))])
    extra("left_arm", "claws_left", (5, 2, 0), (0, 0, 0), [((-1.6, 10.5, -2.8, 1, 4, 1), (28, 14)), ((0, 10.5, -2.8, 1, 4, 1), (28, 14)), ((1.6, 10.5, -2.8, 1, 4, 1), (28, 14))])
    for fx in ("fx_glow", "fx_alpha"):
        root["children"].append(b.group(fx, (0, 24, 0)))

    # animation
    animators = {}
    for bone, chans in scene.ch.items():
        g = b.groups.get(bone)
        if g is None:
            continue
        kfs = []
        for cname, keys in chans.items():
            for t, (v, lerp) in sorted(keys.items()):
                kfs.append({"channel": cname, "data_points": [{"x": v[0], "y": v[1], "z": v[2]}], "uuid": uid(), "time": t,
                            "color": -1, "interpolation": lerp})
        animators[g["uuid"]] = {"name": bone, "type": "bone", "keyframes": kfs}
    eff = []
    for t, e in scene.sounds:
        eff.append({"channel": "sound", "data_points": [{"effect": e, "file": ""}], "uuid": uid(), "time": t, "color": -1, "interpolation": "linear"})
    for t, e in scene.particles:
        eff.append({"channel": "particle", "data_points": [{"effect": e, "locator": "", "script": "", "file": ""}], "uuid": uid(),
                    "time": t, "color": -1, "interpolation": "linear"})
    animators["effects"] = {"name": "Effects", "type": "effect", "keyframes": eff}

    def tex_entry(name, img, i):
        return {"path": "", "name": name, "folder": "", "namespace": "", "id": str(i), "width": img.width, "height": img.height,
                "uv_width": 64, "uv_height": 64, "particle": False, "render_mode": "default", "render_sides": "auto",
                "visible": True, "mode": "bitmap", "saved": False, "uuid": uid(), "relative_path": "", "source": png_data(img)}

    return {
        "meta": {"format_version": "4.10", "model_format": "bedrock", "box_uv": True},
        "name": "dxdcraft_" + scene.name.replace(".", "_"),
        "model_identifier": "dxdcraft_transform",
        "visible_box": [2, 3, 1],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 64, "height": 64},
        "elements": b.elements,
        "outliner": [root],
        "textures": [tex_entry("mannequin.png", mannequin_skin(), 0), tex_entry(armor_name + ".png", armor_tex, 1),
                     tex_entry("gear_extras.png", extras_tex, 2)],
        "animations": [{"uuid": uid(), "name": scene.name, "loop": "once", "override": False, "length": scene.length,
                        "snapping": 20, "selected": True, "anim_time_update": "", "blend_weight": "", "start_delay": "",
                        "loop_delay": "", "animators": animators}],
    }


if __name__ == "__main__":
    os.makedirs(ANIM_DIR, exist_ok=True)
    os.makedirs(BB_DIR, exist_ok=True)
    for key, fn in SCENES.items():
        scene = fn()
        with open(f"{ANIM_DIR}/{key}.animation.json", "w") as f:
            json.dump(scene.bedrock(), f, indent=1)
        with open(f"{BB_DIR}/transform_{key}.bbmodel", "w") as f:
            json.dump(build_bbmodel(key, scene), f)
        print(f"{scene.name:34s} {scene.length}s  bones={len(scene.ch):2d}  sounds={len(scene.sounds):2d}  fx={len(scene.particles):2d}")
