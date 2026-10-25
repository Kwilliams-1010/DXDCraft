package com.dxdcraft.mixin;

import com.dxdcraft.client.anim.TransformScenes;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** During your own transformation scene the camera sweeps around you (see TransformScenes.camera). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;

    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void dxdcraft$transformationCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse,
                                               float partialTick, CallbackInfo ci) {
        double[] c = TransformScenes.camera(entity, partialTick);
        if (c == null) return;
        this.detached = true;
        setRotation((float) c[3], (float) c[4]);
        setPosition(c[0], c[1], c[2]);
    }
}
