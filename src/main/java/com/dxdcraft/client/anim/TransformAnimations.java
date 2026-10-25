package com.dxdcraft.client.anim;

import com.dxdcraft.DxDCraft;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads every Bedrock animation in assets/<ns>/animations/transform/*.animation.json (resource packs can
 * override them; F3+T reloads). Animations are looked up by name, e.g. "transform.boosted_gear".
 *
 * Bone conventions (Blockbench units: degrees, pixels, y up):
 *   player_root            whole body offset/rotation (levitation, spins)
 *   player_head/body/right_arm/left_arm/right_leg/left_leg   the player's pose
 *   armor_head/body/right_arm/left_arm/right_leg/left_leg    armor pieces (position = fly-in offset, scale 0 = hidden)
 *   wing_left, wing_right, horns, crest, halo, tail, pauldrons, claws, core   armor add-ons
 *   fx_glow (scale x = glow brightness), fx_alpha (scale x = armor opacity)
 */
@EventBusSubscriber(modid = DxDCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TransformAnimations extends SimplePreparableReloadListener<Map<String, BedrockAnimation>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Map<String, BedrockAnimation> animations = Map.of();

    public static BedrockAnimation get(String name) {
        return animations.get(name);
    }

    public static int count() {
        return animations.size();
    }

    @SubscribeEvent
    public static void onRegister(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new TransformAnimations());
    }

    @Override
    protected Map<String, BedrockAnimation> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, BedrockAnimation> out = new HashMap<>();
        Map<ResourceLocation, Resource> files = manager.listResources("animations/transform", rl -> rl.getPath().endsWith(".animation.json"));
        for (Map.Entry<ResourceLocation, Resource> f : files.entrySet()) {
            try (Reader reader = f.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> a : root.getAsJsonObject("animations").entrySet()) {
                    out.put(a.getKey(), BedrockAnimation.parse(a.getKey(), a.getValue().getAsJsonObject()));
                }
            } catch (Exception e) {
                LOGGER.error("DxD Craft: could not read transformation animation {}", f.getKey(), e);
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<String, BedrockAnimation> loaded, ResourceManager manager, ProfilerFiller profiler) {
        animations = Map.copyOf(loaded);
        LOGGER.info("DxD Craft: loaded {} transformation animations", animations.size());
    }

    // ------------------------------------------------------------------ applying bones to model parts

    private static final Vector3f TMP = new Vector3f();

    /** Adds the bone's rotation/position at time t to the part (Bedrock -> Java axes) and multiplies its scale. */
    public static void apply(BedrockAnimation anim, String bone, float t, ModelPart part) {
        BedrockAnimation.Bone b = anim.bone(bone);
        if (b == null) return;
        if (b.rotation() != null) {
            b.rotation().sample(t, TMP);
            part.xRot -= TMP.x * Mth.DEG_TO_RAD;
            part.yRot -= TMP.y * Mth.DEG_TO_RAD;
            part.zRot += TMP.z * Mth.DEG_TO_RAD;
        }
        if (b.position() != null) {
            b.position().sample(t, TMP);
            part.x -= TMP.x;
            part.y -= TMP.y;
            part.z += TMP.z;
        }
        if (b.scale() != null) {
            b.scale().sample(t, TMP);
            part.xScale *= TMP.x;
            part.yScale *= TMP.y;
            part.zScale *= TMP.z;
        }
    }

    /** Scale x of a bone at time t (for fx_glow / fx_alpha and visibility checks), or the default if not animated. */
    public static float scalar(BedrockAnimation anim, String bone, float t, float fallback) {
        BedrockAnimation.Bone b = anim.bone(bone);
        if (b == null || b.scale() == null) return fallback;
        return b.scale().sample(t, TMP).x;
    }

    /** Position of a bone at time t in Bedrock units (pixels, y up), or zero. */
    public static Vector3f position(BedrockAnimation anim, String bone, float t, Vector3f out) {
        BedrockAnimation.Bone b = anim.bone(bone);
        if (b == null || b.position() == null) return out.set(0);
        return b.position().sample(t, out);
    }

    /** Rotation of a bone at time t in degrees, or zero. */
    public static Vector3f rotation(BedrockAnimation anim, String bone, float t, Vector3f out) {
        BedrockAnimation.Bone b = anim.bone(bone);
        if (b == null || b.rotation() == null) return out.set(0);
        return b.rotation().sample(t, out);
    }
}
