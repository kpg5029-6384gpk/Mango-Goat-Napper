package dev.mangogoatnapper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@Mod(value="mangogoatnapper",dist=Dist.CLIENT)
public final class NeoEntry {
 public NeoEntry() { MangoGoatNapper.start(); NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> MangoGoatNapper.tick()); }
}
