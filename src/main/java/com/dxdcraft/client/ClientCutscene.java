package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.cutscene.CutsceneScripts;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * Client side of the awakening cutscene: cinematic letterbox, fade from black, typewriter dialogue,
 * a white flash and big title card at the climax, a slow orbiting third-person camera, and the normal
 * HUD hidden for the duration. Sneak (after 2 seconds) skips; the server ends it.
 */
public final class ClientCutscene {
    public static final ResourceLocation LAYER = DxDCraft.id("cutscene");

    private static CutsceneScripts.Script script;
    private static int tick;
    private static CameraType previousCamera;

    private ClientCutscene() {}

    public static boolean active() {
        return script != null;
    }

    public static void start(String gearId) {
        CutsceneScripts.Script s = CutsceneScripts.get(gearId);
        if (s == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (script == null) previousCamera = mc.options.getCameraType();
        script = s;
        tick = 0;
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    }

    public static void stop() {
        if (script == null) return;
        script = null;
        tick = 0;
        Minecraft mc = Minecraft.getInstance();
        if (previousCamera != null) mc.options.setCameraType(previousCamera);
        previousCamera = null;
    }

    // ------------------------------------------------------------------ events

    @EventBusSubscriber(modid = DxDCraft.MODID, value = Dist.CLIENT)
    public static final class GameEvents {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            if (script == null) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                stop();
                return;
            }
            tick++;
            // slow orbit: in front-facing third person, turning the player swings the camera around them
            float speed = tick < CutsceneScripts.CLIMAX ? 1.2F : 0.6F;
            mc.player.setYRot(mc.player.getYRot() + speed);
            mc.player.setXRot(Mth.lerp(0.05F, mc.player.getXRot(), tick < CutsceneScripts.CLIMAX ? 10.0F : -5.0F));
            if (tick > CutsceneScripts.DURATION + 40) stop(); // safety if the server's end message is lost
        }

        /** Hide every other HUD layer while the cutscene plays. */
        @SubscribeEvent
        public static void onLayer(RenderGuiLayerEvent.Pre event) {
            if (script != null && !LAYER.equals(event.getName())) event.setCanceled(true);
        }
    }

    @EventBusSubscriber(modid = DxDCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegisterLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(LAYER, ClientCutscene::render);
        }
    }

    // ------------------------------------------------------------------ rendering

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (script == null) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float t = tick + delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth();
        int h = g.guiHeight();
        int dur = CutsceneScripts.DURATION;

        // letterbox bars slide in over the first second, out over the last
        float barIn = Mth.clamp(t / 20F, 0F, 1F);
        float barOut = Mth.clamp((dur - t) / 15F, 0F, 1F);
        int bar = (int) (h * 0.13F * Math.min(barIn, barOut));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);

        // fade in from black
        if (t < 20) g.fill(0, 0, w, h, alpha(1F - t / 20F) | 0x000000);

        // climax: white flash
        float sinceClimax = t - CutsceneScripts.CLIMAX;
        if (sinceClimax >= 0 && sinceClimax < 12) g.fill(0, 0, w, h, alpha(1F - sinceClimax / 12F) | 0xFFFFFF);

        // title card
        if (sinceClimax >= 0 && t < 175) {
            float a = Mth.clamp(sinceClimax / 8F, 0F, 1F) * Mth.clamp((175 - t) / 15F, 0F, 1F);
            if (a > 0.02F) {
                int col = alpha(a) | (script.color() & 0xFFFFFF);
                g.pose().pushPose();
                float scale = 3.0F + Math.max(0F, (10F - sinceClimax) * 0.08F);
                g.pose().translate(w / 2F, h * 0.36F, 0);
                g.pose().scale(scale, scale, 1F);
                g.drawCenteredString(font, script.title(), 0, 0, col);
                g.pose().popPose();
                g.drawCenteredString(font, script.tagline(), w / 2, (int) (h * 0.36F) + 34, alpha(a) | 0xDDDDDD);
            }
        }

        // dialogue, typed out
        for (CutsceneScripts.Line line : script.lines()) {
            if (t < line.start() || t > line.end()) continue;
            int chars = (int) Math.min(line.text().length(), (t - line.start()) * 1.6F);
            float a = Mth.clamp((line.end() - t) / 6F, 0F, 1F);
            if (a < 0.03F) continue; // fonts treat near-zero alpha as opaque
            String speaker = line.speaker() == null ? mc.player.getName().getString() : line.speaker();
            int baseY = h - bar + Math.max(4, (bar - 22) / 2);
            if (bar < 26) baseY = h - 30;
            g.drawCenteredString(font, speaker, w / 2, baseY, alpha(a) | (line.color() & 0xFFFFFF));
            g.drawCenteredString(font, line.text().substring(0, chars), w / 2, baseY + 12, alpha(a) | 0xFFFFFF);
        }

        // skip hint
        if (t > CutsceneScripts.SKIPPABLE_AFTER && t < dur - 15) {
            g.drawString(font, "Sneak to skip", w - font.width("Sneak to skip") - 6, Math.max(4, bar / 2 - 4), 0x88FFFFFF);
        }
    }

    private static int alpha(float a) {
        return ((int) (Mth.clamp(a, 0F, 1F) * 255) & 0xFF) << 24;
    }
}
