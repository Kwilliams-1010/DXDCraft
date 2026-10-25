package com.dxdcraft.client.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One animation in Blockbench's Bedrock format (".animation.json", exported with
 * File > Export > Export Animations). Supports per-bone rotation (degrees), position (pixels) and
 * scale keyframes with linear, catmullrom and step interpolation, plus "sound_effects" and
 * "particle_effects" timelines. Molang expressions are not evaluated (treated as 0).
 */
public final class BedrockAnimation {
    public enum Lerp { LINEAR, CATMULLROM, STEP }

    public record Key(float time, Vector3f pre, Vector3f post, Lerp lerp) {}

    /** Sorted keyframes of one channel. */
    public static final class Channel {
        private final Key[] keys;

        Channel(List<Key> keys) {
            keys.sort(Comparator.comparingDouble(Key::time));
            this.keys = keys.toArray(new Key[0]);
        }

        /** Value at time t (seconds) into {@code out}. */
        public Vector3f sample(float t, Vector3f out) {
            if (keys.length == 0) return out.set(0);
            if (t <= keys[0].time) return out.set(keys[0].pre);
            Key last = keys[keys.length - 1];
            if (t >= last.time) return out.set(last.post);
            int i = 0;
            while (i < keys.length - 1 && keys[i + 1].time <= t) i++;
            Key a = keys[i], b = keys[i + 1];
            if (a.lerp == Lerp.STEP) return out.set(a.post);
            float f = (t - a.time) / Math.max(1e-6F, b.time - a.time);
            if (a.lerp == Lerp.CATMULLROM || b.lerp == Lerp.CATMULLROM) {
                Vector3f p0 = keys[Math.max(0, i - 1)].post, p3 = keys[Math.min(keys.length - 1, i + 2)].pre;
                return out.set(catmull(p0.x, a.post.x, b.pre.x, p3.x, f), catmull(p0.y, a.post.y, b.pre.y, p3.y, f),
                        catmull(p0.z, a.post.z, b.pre.z, p3.z, f));
            }
            return out.set(a.post.x + (b.pre.x - a.post.x) * f, a.post.y + (b.pre.y - a.post.y) * f, a.post.z + (b.pre.z - a.post.z) * f);
        }

        private static float catmull(float p0, float p1, float p2, float p3, float t) {
            float t2 = t * t, t3 = t2 * t;
            return 0.5F * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
        }
    }

    /** Channels of one bone; any may be null (= not animated). */
    public record Bone(Channel rotation, Channel position, Channel scale) {}

    /** A timeline event: a sound id or particle effect name at a time. */
    public record Event(float time, String effect) {}

    private final String name;
    private final float length;
    private final Map<String, Bone> bones;
    private final List<Event> sounds, particles;

    private BedrockAnimation(String name, float length, Map<String, Bone> bones, List<Event> sounds, List<Event> particles) {
        this.name = name;
        this.length = length;
        this.bones = bones;
        this.sounds = sounds;
        this.particles = particles;
    }

    public String name() { return name; }
    public float length() { return length; }
    public Bone bone(String bone) { return bones.get(bone); }
    public List<Event> sounds() { return sounds; }
    public List<Event> particles() { return particles; }

    // ------------------------------------------------------------------ parsing

    public static BedrockAnimation parse(String name, JsonObject json) {
        Map<String, Bone> bones = new HashMap<>();
        float length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : 0.0F;
        if (json.has("bones")) {
            for (Map.Entry<String, JsonElement> b : json.getAsJsonObject("bones").entrySet()) {
                JsonObject o = b.getValue().getAsJsonObject();
                Channel rot = channel(o.get("rotation"), 0.0F), pos = channel(o.get("position"), 0.0F), scl = channel(o.get("scale"), 1.0F);
                bones.put(b.getKey(), new Bone(rot, pos, scl));
                for (Channel c : new Channel[]{rot, pos, scl}) {
                    if (c != null && c.keys.length > 0) length = Math.max(length, c.keys[c.keys.length - 1].time);
                }
            }
        }
        return new BedrockAnimation(name, length, bones, events(json.get("sound_effects")), events(json.get("particle_effects")));
    }

    private static Channel channel(JsonElement e, float fallback) {
        if (e == null) return null;
        List<Key> keys = new ArrayList<>();
        if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> k : e.getAsJsonObject().entrySet()) {
                float time;
                try {
                    time = Float.parseFloat(k.getKey());
                } catch (NumberFormatException ex) {
                    continue;
                }
                JsonElement v = k.getValue();
                if (v.isJsonObject()) {
                    JsonObject ko = v.getAsJsonObject();
                    Vector3f post = vec(ko.has("post") ? ko.get("post") : ko.get("pre"), fallback);
                    Vector3f pre = ko.has("pre") ? vec(ko.get("pre"), fallback) : new Vector3f(post);
                    Lerp lerp = switch (ko.has("lerp_mode") ? ko.get("lerp_mode").getAsString() : "linear") {
                        case "catmullrom" -> Lerp.CATMULLROM;
                        case "step" -> Lerp.STEP;
                        default -> Lerp.LINEAR;
                    };
                    keys.add(new Key(time, pre, post, lerp));
                } else {
                    Vector3f val = vec(v, fallback);
                    keys.add(new Key(time, val, new Vector3f(val), Lerp.LINEAR));
                }
            }
        } else {
            Vector3f val = vec(e, fallback); // constant value
            keys.add(new Key(0.0F, val, new Vector3f(val), Lerp.LINEAR));
        }
        return new Channel(keys);
    }

    private static Vector3f vec(JsonElement e, float fallback) {
        if (e == null) return new Vector3f(fallback);
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            return new Vector3f(num(a.get(0), fallback), num(a.size() > 1 ? a.get(1) : a.get(0), fallback), num(a.size() > 2 ? a.get(2) : a.get(0), fallback));
        }
        float n = num(e, fallback);
        return new Vector3f(n);
    }

    private static float num(JsonElement e, float fallback) {
        try {
            return e.getAsFloat();
        } catch (Exception ex) {
            try {
                return Float.parseFloat(e.getAsString().trim());
            } catch (Exception ignored) {
                return fallback; // molang expression: not supported
            }
        }
    }

    private static List<Event> events(JsonElement e) {
        List<Event> out = new ArrayList<>();
        if (e == null || !e.isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> k : e.getAsJsonObject().entrySet()) {
            float time;
            try {
                time = Float.parseFloat(k.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            JsonElement v = k.getValue();
            List<JsonElement> list = new ArrayList<>();
            if (v.isJsonArray()) v.getAsJsonArray().forEach(list::add);
            else list.add(v);
            for (JsonElement item : list) {
                if (item.isJsonObject() && item.getAsJsonObject().has("effect")) out.add(new Event(time, item.getAsJsonObject().get("effect").getAsString()));
            }
        }
        out.sort(Comparator.comparingDouble(Event::time));
        return out;
    }
}
