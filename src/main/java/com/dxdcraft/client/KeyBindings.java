package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import com.dxdcraft.logic.Abilities;
import com.dxdcraft.network.AbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** G = Ultimate, H = Balance Breaker, V = Racial Ability, J = Juggernaut Drive, R = Element (Sneak+R = second element). Rebindable under Options > Controls > DxD Craft. */
public final class KeyBindings {
    public static final String CATEGORY = "key.categories.dxdcraft";
    public static final KeyMapping ULTIMATE = new KeyMapping("key.dxdcraft.ultimate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping JUGGERNAUT = new KeyMapping("key.dxdcraft.juggernaut", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping ELEMENT = new KeyMapping("key.dxdcraft.element", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping BALANCE_BREAKER = new KeyMapping("key.dxdcraft.balance_breaker", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping ABILITY = new KeyMapping("key.dxdcraft.ability", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    private KeyBindings() {}

    @EventBusSubscriber(modid = DxDCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) {
            event.register(ULTIMATE);
            event.register(ABILITY);
            event.register(JUGGERNAUT);
            event.register(ELEMENT);
            event.register(BALANCE_BREAKER);
        }
    }

    @EventBusSubscriber(modid = DxDCraft.MODID, value = Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().player == null) return;
            while (ULTIMATE.consumeClick()) {
                PacketDistributor.sendToServer(new AbilityPayload(Abilities.ACTION_ULTIMATE));
            }
            while (JUGGERNAUT.consumeClick()) {
                PacketDistributor.sendToServer(new AbilityPayload(Abilities.ACTION_JUGGERNAUT));
            }
            while (ELEMENT.consumeClick()) {
                boolean second = Minecraft.getInstance().player.isShiftKeyDown();
                PacketDistributor.sendToServer(new AbilityPayload(second ? Abilities.ACTION_ELEMENT_2 : Abilities.ACTION_ELEMENT_1));
            }
            while (BALANCE_BREAKER.consumeClick()) {
                PacketDistributor.sendToServer(new AbilityPayload(Abilities.ACTION_BALANCE_BREAKER));
            }
            while (ABILITY.consumeClick()) {
                PacketDistributor.sendToServer(new AbilityPayload(Abilities.ACTION_RACE_ABILITY));
            }
        }
    }
}
