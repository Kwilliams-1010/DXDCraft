package com.dxdcraft.mixin;

import com.dxdcraft.client.anim.TransformScenes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After vanilla poses a player model, let a playing transformation scene adjust the pose. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void dxdcraft$transformationPose(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                             float netHeadYaw, float headPitch, CallbackInfo ci) {
        TransformScenes.poseModel((HumanoidModel<?>) (Object) this, entity, ageInTicks);
    }
}
