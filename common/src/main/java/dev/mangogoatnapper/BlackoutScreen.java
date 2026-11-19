package dev.mangogoatnapper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A brief, non-pausing full-screen blackout during the abduction. */
final class BlackoutScreen extends Screen {
 BlackoutScreen() { super(Component.literal("")); }
 @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
  graphics.fill(0, 0, width, height, 0xff000000);
 }
 @Override public boolean isPauseScreen() { return false; }
 @Override public void onClose() { }
}
