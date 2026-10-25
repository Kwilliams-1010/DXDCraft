package com.dxdcraft.client;

import com.dxdcraft.DxDCraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Development-only visual check. Does nothing unless the game is started with -Ddxdcraft.renderTest=true:
 * then it cycles every Balance Breaker / Juggernaut form and worn gear, screenshots each from the
 * front and back, and quits. Needs an op player on a server running DxD Craft.
 */
@EventBusSubscriber(modid = DxDCraft.MODID, value = Dist.CLIENT)
public final class RenderTest {
    private static final boolean ENABLED = Boolean.getBoolean("dxdcraft.renderTest");
    private static final String[] GEARS = {"boosted_gear", "divine_dividing", "sword_birth", "absorption_line", "forbidden_balor_view",
            "dimension_lost", "zenith_tempest", "annihilation_maker", "regulus_nemea", "twilight_healing"};
    private record Step(int delay, Runnable action) {}
    private static final List<Step> STEPS = new ArrayList<>();
    private static int inWorld, index, wait;

    private RenderTest() {}

    private static void cmd(String c) {
        Minecraft.getInstance().player.connection.sendCommand(c);
    }

    private static void camera(CameraType t) {
        Minecraft.getInstance().options.setCameraType(t);
        // Face south, level, with the body aligned to the head so front/back shots are square-on.
        var p = Minecraft.getInstance().player;
        p.setYRot(0.0F);
        p.setXRot(15.0F);
        p.yHeadRot = p.yHeadRotO = 0.0F;
        p.yBodyRot = p.yBodyRotO = 0.0F;
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "dxd_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
    }

    private static void build() {
        STEPS.add(new Step(1, () -> {
            Minecraft.getInstance().options.hideGui = true;
            cmd("time set noon");
            cmd("weather clear");
            cmd("gamemode creative");
            cmd("effect give @s minecraft:night_vision infinite 0 true");
        }));
        for (String g : GEARS) {
            STEPS.add(new Step(5, () -> { cmd("dxd balancebreaker start " + g); camera(CameraType.THIRD_PERSON_FRONT); }));
            STEPS.add(new Step(12, () -> shot("assemble_" + g)));            // mid-transformation
            STEPS.add(new Step(40, () -> shot("bb_" + g + "_front")));
            STEPS.add(new Step(2, () -> camera(CameraType.THIRD_PERSON_BACK)));
            STEPS.add(new Step(8, () -> shot("bb_" + g + "_back")));
            STEPS.add(new Step(2, () -> cmd("dxd balancebreaker stop")));
        }
        for (String mode : new String[]{"incomplete", "complete"}) {
            for (String dragon : new String[]{"red", "white"}) {
                STEPS.add(new Step(25, () -> { cmd("dxd juggernaut start " + dragon + " " + mode); camera(CameraType.THIRD_PERSON_FRONT); }));
                STEPS.add(new Step(45, () -> shot("jd_" + dragon + "_" + mode + "_front")));
                STEPS.add(new Step(2, () -> camera(CameraType.THIRD_PERSON_BACK)));
                STEPS.add(new Step(8, () -> shot("jd_" + dragon + "_" + mode + "_back")));
                STEPS.add(new Step(2, () -> cmd("dxd juggernaut stop")));
            }
        }
        // HUD: survival, first person, mid Juggernaut Drive (extra hearts) - checks the health/armor bars.
        STEPS.add(new Step(20, () -> {
            cmd("gamemode survival");
            cmd("item replace entity @s armor.chest with dxdcraft:dragonite_chestplate");
            cmd("item replace entity @s armor.head with dxdcraft:dragonite_helmet");
            cmd("dxd juggernaut start red complete");
            camera(CameraType.FIRST_PERSON);
            Minecraft.getInstance().options.hideGui = false;
        }));
        STEPS.add(new Step(40, () -> shot("hud_juggernaut")));
        STEPS.add(new Step(2, () -> {
            Minecraft.getInstance().options.hideGui = true;
            cmd("dxd juggernaut stop");
            cmd("clear @s");
            cmd("gamemode creative");
        }));
        // Worn in the Sacred Gear Curios slot.
        for (String g : GEARS) {
            STEPS.add(new Step(20, () -> { cmd("curios replace sacred_gear 0 @s with dxdcraft:" + g); camera(CameraType.THIRD_PERSON_FRONT); }));
            STEPS.add(new Step(25, () -> shot("worn_" + g + "_front")));
            STEPS.add(new Step(2, () -> camera(CameraType.THIRD_PERSON_BACK)));
            STEPS.add(new Step(8, () -> shot("worn_" + g + "_back")));
        }
        STEPS.add(new Step(10, () -> Minecraft.getInstance().stop()));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (++inWorld < 100) return; // let chunks load
        if (!mc.player.hasPermissions(2)) return; // wait until the test harness has opped us
        if (STEPS.isEmpty() && index == 0) build();
        if (index >= STEPS.size()) return;
        if (++wait < STEPS.get(index).delay) return;
        wait = 0;
        STEPS.get(index++).action().run();
    }
}
