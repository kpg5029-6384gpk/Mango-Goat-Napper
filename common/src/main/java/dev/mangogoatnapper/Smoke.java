package dev.mangogoatnapper;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Opt-in synthetic end-to-end check in a new isolated development world. */
final class Smoke {
 static int ticks, worldTicks, poseTicks, wakeTicks, grabTicks, chaseTicks; static volatile boolean started, goatSeen, abducted, awakened; static double x,z; static float health;
 static void tick() {
  var mc=Minecraft.getInstance(); ticks++; mc.options.pauseOnLostFocus=false;
  if(!started && ticks>=100) {
   started=true;
   var settings=new LevelSettings("Goat Napper QA",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT);
   mc.createWorldOpenFlows().createFreshLevel("goatnapper-qa-"+System.currentTimeMillis(),settings,new WorldOptions(42L,false,false),WorldPresets::createNormalWorldDimensions,mc.gui.screen()); return;
  }
  if(mc.player==null || mc.level==null)return;
  worldTicks++;
  if(worldTicks==30) { mc.gui.setScreen(null); inject(0); if(Boolean.getBoolean("mangogoatnapper.underground")) { var server=mc.getSingleplayerServer(); var id=mc.player.getUUID(); server.execute(()->testArena(server,id)); } }
  if(worldTicks==60) inject(1);
  if(goatSeen && MangoGoatNapper.testPhase()==1 && !Boolean.getBoolean("mangogoatnapper.elevated")) {
   chaseTicks++;
   if(chaseTicks==5) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
   if(chaseTicks==15) net.minecraft.client.Screenshot.grab(mc,false);
  }
  if(MangoGoatNapper.testPhase()==4) {
   grabTicks++;
   if(grabTicks==5) mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
   if(grabTicks==12) {
    LogUtils.getLogger().info("GOAT NAPPER CLIENT PASSENGER: {}",mc.player.isPassenger());
    net.minecraft.client.Screenshot.grab(mc,false);
   }
  }
  if(abducted && !awakened) {
   poseTicks++;
   if(poseTicks==40) {
    LogUtils.getLogger().info("GOAT NAPPER CLIENT POSE: {}",mc.player.getPose());
    mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
   }
   if(poseTicks==150) net.minecraft.client.Screenshot.grab(mc,false);
   if(poseTicks==200) { LogUtils.getLogger().info("GOAT NAPPER SIMULATING SNEAK INPUT"); mc.options.keyShift.setDown(true); }
  }
  if(awakened) {
   wakeTicks++;
   if(wakeTicks==10) { mc.options.keyShift.setDown(false); LogUtils.getLogger().info("GOAT NAPPER WAKE CLIENT POSE: {}",mc.player.getPose()); net.minecraft.client.Screenshot.grab(mc,false); inject(2); }
   if(wakeTicks==40) { if(MangoGoatNapper.testPhase()!=0) throw new AssertionError("Cooldown did not suppress the second gift"); LogUtils.getLogger().info("GOAT NAPPER COOLDOWN PASSED"); }
   if(wakeTicks==50) mc.stop();
  }
  if(worldTicks>=80 && worldTicks%10==0) {
   var server=mc.getSingleplayerServer(); var id=mc.player.getUUID();
   server.execute(()->{
    var p=server.getPlayerList().getPlayer(id); if(p==null)return;
    int phase=MangoGoatNapper.testPhase();
    if(phase==1 && !goatSeen) { if(!MangoGoatNapper.testGoatHarmless())throw new AssertionError("Goat must have visual fire only and zero attack damage"); goatSeen=true; if(Boolean.getBoolean("mangogoatnapper.water")) MangoGoatNapper.testWaterArena(p); else if(Boolean.getBoolean("mangogoatnapper.elevated")) MangoGoatNapper.testElevatedArena(p); else if(!Boolean.getBoolean("mangogoatnapper.underground")) MangoGoatNapper.testBringGoat(p); health=p.getHealth(); x=p.getX(); z=p.getZ(); }
    if(phase==3 && !abducted) { abducted=true; if(p.getHealth()!=health || p.getRemainingFireTicks()>0)throw new AssertionError("Goat damaged or ignited player"); double distance=Math.hypot(p.getX()-x,p.getZ()-z); if(distance<99 || distance>152)throw new AssertionError("Wrong destination distance "+distance); LogUtils.getLogger().info("GOAT NAPPER SERVER POSE: {}",p.getPose()); }
    else if(abducted && phase==0 && !awakened) { awakened=true; LogUtils.getLogger().info("GOAT NAPPER SMOKE PASSED: spawn, capture, destination, wake");  }
   });
  }
  if(worldTicks>(Boolean.getBoolean("mangogoatnapper.manual")?6000:2400) && !awakened) { LogUtils.getLogger().error("GOAT NAPPER SMOKE FAILED: goatSeen={} abducted={} phase={}",goatSeen,abducted,MangoGoatNapper.testPhase()); mc.stop(); }
 }
 static void testArena(net.minecraft.server.MinecraftServer server,java.util.UUID id) { var p=server.getPlayerList().getPlayer(id); if(p!=null) MangoGoatNapper.testUndergroundArena(p); }
 static void inject(int seq) {
  var f=new JsonObject(); f.addProperty("session","qa"); f.addProperty("sequence",seq); var events=new JsonArray();
  if(seq>0) { var e=new JsonObject(); e.addProperty("sequence",seq); e.addProperty("created",System.currentTimeMillis()/1000.0); e.addProperty("username","qa_viewer"); e.addProperty("test",true); events.add(e); }
  f.add("events",events); MangoGoatNapper.testFeed(f);
 }
}


