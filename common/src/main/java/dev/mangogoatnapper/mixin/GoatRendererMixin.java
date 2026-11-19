package dev.mangogoatnapper.mixin;
import dev.mangogoatnapper.NapperRenderState;

import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.goat.Goat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GoatRenderer.class)
abstract class GoatRendererMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/animal/goat/Goat;Lnet/minecraft/client/renderer/entity/state/GoatRenderState;F)V",at=@At("TAIL"))
 private void markNapper(Goat goat, GoatRenderState state, float delta, CallbackInfo ci) {
  ((NapperRenderState)state).mangogoatnapper$setNapper(goat.hasCustomName() && goat.getName().getString().equals("The Goat Napper"));
 }
 @Inject(method="getTextureLocation(Lnet/minecraft/client/renderer/entity/state/GoatRenderState;)Lnet/minecraft/resources/Identifier;",at=@At("HEAD"),cancellable=true)
 private void napperTexture(GoatRenderState state, CallbackInfoReturnable<Identifier> result) {
  if (((NapperRenderState)state).mangogoatnapper$isNapper())
   result.setReturnValue(Identifier.fromNamespaceAndPath("mangogoatnapper", "textures/entity/napper_goat.png"));
 }
}

