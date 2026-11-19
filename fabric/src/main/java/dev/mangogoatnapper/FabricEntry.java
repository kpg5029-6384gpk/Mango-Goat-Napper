package dev.mangogoatnapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public final class FabricEntry implements ClientModInitializer {
 public void onInitializeClient() { MangoGoatNapper.start(); ClientTickEvents.END_CLIENT_TICK.register(mc -> MangoGoatNapper.tick()); }
}
