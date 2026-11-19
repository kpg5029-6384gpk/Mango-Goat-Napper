package dev.mangogoatnapper.mixin;

import dev.mangogoatnapper.MangoGoatNapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the local player's rendered pose lying down until sneak wakes them. */
@Mixin(LivingEntityRenderer.class)
abstract class LyingPlayerRendererMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
 private void goatNapperPose(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
  if (entity == Minecraft.getInstance().player && MangoGoatNapper.isLyingDown()) state.pose=Pose.SLEEPING;
 }
}
