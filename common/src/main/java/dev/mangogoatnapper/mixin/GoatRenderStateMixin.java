package dev.mangogoatnapper.mixin;
import dev.mangogoatnapper.NapperRenderState;

import net.minecraft.client.renderer.entity.state.GoatRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GoatRenderState.class)
abstract class GoatRenderStateMixin implements NapperRenderState {
 @Unique private boolean mangogoatnapper$napper;
 public boolean mangogoatnapper$isNapper() { return mangogoatnapper$napper; }
 public void mangogoatnapper$setNapper(boolean value) { mangogoatnapper$napper=value; }
}

