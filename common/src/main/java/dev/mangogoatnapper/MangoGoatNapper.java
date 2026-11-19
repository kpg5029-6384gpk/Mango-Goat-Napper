package dev.mangogoatnapper;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;

public final class MangoGoatNapper {
 private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 private static volatile JsonObject feed=new JsonObject();
 private static volatile String status="Disconnected";
 private static String session="";
 private static long sequence, cooldownUntil;
 private static Mob goat;
 private static volatile boolean blackout;
 private static volatile int phase;
 private static int ticks;
 private static boolean wakeArmed;
 private static java.util.UUID target;
 private static BlockPos destination;
 private static double originX, originZ;
 private static final java.util.List<BlockPos> destinationCandidates=new java.util.ArrayList<>();
 private static final java.util.List<CompletableFuture<?>> destinationLoads=new java.util.ArrayList<>();
 static void start() {
  if(Boolean.getBoolean("mangogoatnapper.smoke"))return;
  var executor=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"Goat bridge");t.setDaemon(true);return t;});
  executor.scheduleWithFixedDelay(MangoGoatNapper::poll,0,1,TimeUnit.SECONDS);
 }
 private static void poll() {
  try {
   var credentials=JsonParser.parseString(Files.readString(Path.of(System.getenv("LOCALAPPDATA"),"MangoGoatNapper","connection.json"))).getAsJsonObject();
   var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:18765/status")).timeout(Duration.ofSeconds(3)).header("Authorization","Bearer "+credentials.get("token").getAsString()).build();
   var response=HTTP.send(request,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()!=200)throw new IllegalStateException();
   var root=JsonParser.parseString(response.body()).getAsJsonObject(); status=root.get("status").getAsString(); feed=root.getAsJsonObject("pets");
  } catch(Exception e) { status="Disconnected"; feed=new JsonObject(); }
 }
 static void tick() {
  var mc=Minecraft.getInstance();
  if (phase==3 && mc.player!=null) mc.player.setPose(Pose.SLEEPING);
  if(Boolean.getBoolean("mangogoatnapper.smoke")) Smoke.tick();
  if (blackout && mc.player != null && !(mc.gui.screen() instanceof BlackoutScreen)) mc.gui.setScreen(new BlackoutScreen());
  else if (!blackout && mc.gui.screen() instanceof BlackoutScreen) mc.gui.setScreen(null);
  if(mc.player!=null && mc.getSingleplayerServer()!=null) {
   var server=mc.getSingleplayerServer(); var id=mc.player.getUUID();
   server.execute(()->advance(server,id));
  }
  var f=feed;
  try {
   String id=f.get("session").getAsString(); long latest=f.get("sequence").getAsLong();
   if(!id.equals(session)) { session=id; sequence=latest; return; }
   long before=sequence; sequence=Math.max(sequence,latest);
   if(mc.player==null || mc.getSingleplayerServer()==null)return;
   var server=mc.getSingleplayerServer(); var playerId=mc.player.getUUID(); var dimension=mc.level.dimension();
   double now=System.currentTimeMillis()/1000.0;
   for(var item:f.getAsJsonArray("events")) {
    var e=item.getAsJsonObject(); long n=e.get("sequence").getAsLong(); double age=now-e.get("created").getAsDouble();
    boolean test=e.has("test") && e.get("test").getAsBoolean();
    if((!test && !status.equals("Live")) || n<=before || n>latest || age<0 || age>30)continue;
    server.execute(()->{
     var player=server.getPlayerList().getPlayer(playerId);
     if(player==null || !player.isAlive() || player.level().dimension()!=dimension || phase!=0 || System.currentTimeMillis()<cooldownUntil)return;
     var level=player.level(); var random=ThreadLocalRandom.current();
     boolean underground=player.getY()<level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,player.blockPosition().getX(),player.blockPosition().getZ())-8;
     if(underground) {
      var mob=EntityTypes.GOAT.create(level,EntitySpawnReason.TRIGGERED);
      if(!(mob instanceof Mob candidate))return;
      int[] distances={40,30,20,12,8,5};
      for(int distance:distances) for(int direction=0;direction<32;direction++) {
       double angle=direction*Math.PI/16;
       int x=(int)Math.floor(player.getX()+Math.cos(angle)*distance), z=(int)Math.floor(player.getZ()+Math.sin(angle)*distance);
       if(!level.hasChunk(x>>4,z>>4))continue;
       for(int offset=0;offset<=16;offset++) {
        int y=player.blockPosition().getY()+(offset==0?0:(offset%2==1?(offset+1)/2:-offset/2));
        var pos=new BlockPos(x,y,z);
        if(!level.getBlockState(pos.below()).isSolid() || !level.getFluidState(pos).isEmpty())continue;
        candidate.setPos(x+0.5,y,z+0.5);
        if(!level.noCollision(candidate))continue;
        candidate.setOnGround(true);
        var path=candidate.getNavigation().createPath(player,0);
        if(path==null || !path.canReach())continue;
        spawnGoat(candidate,playerId,player,level); return;
       }
      }
      player.sendSystemMessage(Component.literal("Goat Napper: no reachable spawn nearby.")); return;
     }
     for(int i=0;i<24;i++) {
      double angle=random.nextDouble(Math.PI*2), distance=random.nextDouble(30,51);
      int x=(int)Math.floor(player.getX()+Math.cos(angle)*distance), z=(int)Math.floor(player.getZ()+Math.sin(angle)*distance);
      if(!level.hasChunk(x>>4,z>>4))continue;
      int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
      var mob=EntityTypes.GOAT.create(level,EntitySpawnReason.TRIGGERED);
      if(!(mob instanceof Mob candidate))return;
      candidate.setPos(x+0.5,y,z+0.5);
      if(!level.noCollision(candidate) || !level.getBlockState(new BlockPos(x,y-1,z)).isSolid())continue;
      spawnGoat(candidate,playerId,player,level);
      return;
     }
     player.sendSystemMessage(Component.literal("Goat Napper: no loaded safe spawn nearby."));
    });
   }
  } catch(RuntimeException ignored) { }
 }
 private static void spawnGoat(Mob candidate,java.util.UUID playerId,net.minecraft.server.level.ServerPlayer player,net.minecraft.server.level.ServerLevel level) {
  candidate.finalizeSpawn(level,level.getCurrentDifficultyAt(candidate.blockPosition()),EntitySpawnReason.TRIGGERED,null);
  candidate.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(0); candidate.setPermanentlyInvulnerable(true); candidate.setSharedFlagOnFire(true);
  candidate.setCustomName(Component.literal("The Goat Napper")); candidate.setCustomNameVisible(true);
  level.addFreshEntity(candidate); goat=candidate; target=playerId; phase=1; ticks=0; cooldownUntil=System.currentTimeMillis()+300000;
  player.sendSystemMessage(Component.literal("You hear a scream in the distance..."));
 }
 private static void advance(net.minecraft.server.MinecraftServer server, java.util.UUID playerId) {
  if(phase==0 || !playerId.equals(target))return;
  var player=server.getPlayerList().getPlayer(target);
  if(player==null || !player.isAlive() || (phase!=3 && (goat==null || !goat.isAlive() || goat.level()!=player.level()))) { cleanup(); return; }
  if((phase==2 || phase==4 || phase==5) && player.getVehicle()!=goat && !player.startRiding(goat,true,true)) { cleanup(); return; }
  ticks++;
  if(phase==1) {
   goat.setSharedFlagOnFire(true);
   if(ticks%5==1)goat.getNavigation().moveTo(player,3.7);
   // A goat can leap up to a nearby raised player when pathfinding alone stalls.
   double dx=player.getX()-goat.getX(), dz=player.getZ()-goat.getZ();
   double horizontal=Math.hypot(dx,dz);
   // Land navigation struggles in water; swim directly toward the player while
   // retaining normal collision physics and limiting vertical speed.
   if(goat.isInWater() && horizontal>0.1) {
    double dy=player.getY()-goat.getY();
    goat.setDeltaMovement(dx/horizontal*0.42, Math.max(-0.12,Math.min(0.18,dy*0.12)), dz/horizontal*0.42);
   }
   if(ticks%30==1 && goat.onGround() && horizontal>0.1 && horizontal<10 && player.getY()-goat.getY()>1.5 && player.getY()-goat.getY()<=5) {
    goat.setDeltaMovement(dx/horizontal*0.32,1.05,dz/horizontal*0.32);
   }
   if(ticks==1)server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"execute at "+player.getStringUUID()+" run playsound mangogoatnapper:goat_scream hostile @a ~ ~ ~ 1 0.85 1");
   if(ticks>=100 && goat.distanceTo(player)<2.5) {
    goat.getNavigation().stop();
    if(!player.startRiding(goat,true,true)) { cleanup(); return; }
    prepareDestinations(player);
    phase=4; ticks=0;
   } else if(ticks>1200) { player.sendSystemMessage(Component.literal("The goat lost your trail.")); cleanup(); }
  } else if(phase==4) {
   goat.setSharedFlagOnFire(true);
   if(ticks>=20) { blackout=true; phase=2; ticks=0; }
  } else if(phase==2 && ticks>=40 && ticks%10==0) {
   var level=player.level(); boolean moved=false;
   search: for(var candidate:destinationCandidates) {
    if(level.getChunkSource().getChunkNow(candidate.getX()>>4,candidate.getZ()>>4)==null)continue;
    for(int ox=-4;ox<=4;ox+=2) for(int oz=-4;oz<=4;oz+=2) {
    int x=candidate.getX()+ox, z=candidate.getZ()+oz;
    if((x>>4)!=(candidate.getX()>>4) || (z>>4)!=(candidate.getZ()>>4) || (x&15)==0 || (x&15)==15 || (z&15)==0 || (z&15)==15) continue;
    double distance=Math.hypot(x+0.5-originX,z+0.5-originZ);
    if(distance<100 || distance>150)continue;
    int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
    BlockPos pos=new BlockPos(x,y,z);
    var floor=level.getBlockState(pos.below());
    if(!level.getWorldBorder().isWithinBounds(pos) || !safeFloor(floor) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir())continue;
    boolean stable=true;
    for(int dx=-1;dx<=1 && stable;dx++) for(int dz=-1;dz<=1;dz++) {
     BlockPos nearby=pos.offset(dx,0,dz);
     if(!safeFloor(level.getBlockState(nearby.below())) || !level.getBlockState(nearby).isAir() || !level.getBlockState(nearby.above()).isAir()) { stable=false; break; }
    }
    if(!stable)continue;
    destination=pos; moved=true; break search;
    }
   }
   if(!moved && ticks<300) return;
   if(!moved) { player.stopRiding(); player.sendSystemMessage(Component.literal("Goat Napper: no safe destination found.")); cleanup(); return; }
   phase=5; ticks=0;
  } else if(phase==5) {
   goat.setSharedFlagOnFire(true);
   if(ticks%20==1)goat.getNavigation().moveTo(destination.getX()+0.5,destination.getY(),destination.getZ()+0.5,3.2);
   if(goat.distanceToSqr(destination.getX()+0.5,destination.getY(),destination.getZ()+0.5)<9 || ticks>=240) {
    player.stopRiding(); player.teleportTo(destination.getX()+0.5,destination.getY(),destination.getZ()+0.5);
    goat.discard(); goat=null; blackout=false;
    player.setPose(Pose.SLEEPING); player.sendSystemMessage(Component.literal("Press sneak to wake up.")); wakeArmed=false; phase=3; ticks=0;
   }
  } else if(phase==3) { if(!player.isShiftKeyDown())wakeArmed=true; if((wakeArmed && player.isShiftKeyDown()) || ticks>1200) { player.setPose(Pose.STANDING); cleanup(); } else { player.setPose(Pose.SLEEPING); } }
 }
 private static void prepareDestinations(net.minecraft.server.level.ServerPlayer player) {
  destinationCandidates.clear(); destinationLoads.clear();
  originX=player.getX(); originZ=player.getZ();
  var level=player.level(); var random=ThreadLocalRandom.current();
  for(int i=0;i<48;i++) {
   double a=random.nextDouble(Math.PI*2), d=random.nextDouble(100,151);
   int x=(int)Math.floor(player.getX()+Math.cos(a)*d), z=(int)Math.floor(player.getZ()+Math.sin(a)*d);
   if((x&15)==0 || (x&15)==15 || (z&15)==0 || (z&15)==15)continue;
   var pos=new BlockPos(x,0,z); destinationCandidates.add(pos);
   if(destinationLoads.size()<8 && !level.hasChunk(x>>4,z>>4)) {
    int cx=x>>4, cz=z>>4;
    destinationLoads.add(CompletableFuture.supplyAsync(()->level.getChunkSource().getChunkFuture(cx,cz,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true)).thenCompose(f->f));
   }
  }
 }
 static void testFeed(JsonObject value) { status="Disconnected"; feed=value; }
 public static boolean isLyingDown() { return phase==3; }
 static int testPhase() { return phase; }
 static boolean testGoatHarmless() { return goat != null && goat.getRemainingFireTicks() <= 0 && goat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue() == 0; }
 static void testBringGoat(net.minecraft.server.level.ServerPlayer player) { if(goat!=null)goat.setPos(player.getX(),player.getY(),player.getZ()+3); }
 static void testUndergroundArena(net.minecraft.server.level.ServerPlayer player) {
  var level=player.level(); int x=player.blockPosition().getX(), z=player.blockPosition().getZ(), y=40;
  for(int dx=-45;dx<=45;dx++) for(int dz=-3;dz<=3;dz++) for(int h=0;h<=5;h++) {
   var block=(h==0 || h==5 || Math.abs(dz)==3)?net.minecraft.world.level.block.Blocks.STONE:net.minecraft.world.level.block.Blocks.AIR;
   level.setBlockAndUpdate(new BlockPos(x+dx,y+h,z+dz),block.defaultBlockState());
  }
  player.teleportTo(x+0.5,y+1,z+0.5);
 }
 static void testWaterArena(net.minecraft.server.level.ServerPlayer player) {
  var level=player.level(); int x=player.blockPosition().getX(), z=player.blockPosition().getZ(), y=160;
  for(int dx=-20;dx<=20;dx++) for(int dz=-5;dz<=5;dz++) {
   level.setBlockAndUpdate(new BlockPos(x+dx,y,z+dz),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
   for(int h=1;h<=3;h++) level.setBlockAndUpdate(new BlockPos(x+dx,y+h,z+dz),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
  }
  player.teleportTo(x+12.5,y+3,z+0.5); goat.setPos(x-12.5,y+2,z+0.5); goat.setDeltaMovement(0,0,0);
 }
 static void testElevatedArena(net.minecraft.server.level.ServerPlayer player) {
  var level=player.level(); int x=player.blockPosition().getX(), z=player.blockPosition().getZ(), y=160;
  var stone=net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
  for(int dx=-8;dx<=8;dx++) for(int dz=-8;dz<=8;dz++) level.setBlockAndUpdate(new BlockPos(x+dx,y,z+dz),stone);
  for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) level.setBlockAndUpdate(new BlockPos(x+dx,y+4,z+dz),stone);
  player.teleportTo(x+0.5,y+5,z+0.5); goat.setPos(x+6.5,y+1,z+0.5); goat.setDeltaMovement(0,0,0);
 }

 private static boolean safeFloor(net.minecraft.world.level.block.state.BlockState floor) {
  return floor.isSolid() && !floor.is(net.minecraft.tags.BlockTags.LOGS) && !floor.is(net.minecraft.tags.BlockTags.LEAVES)
   && !floor.is(net.minecraft.world.level.block.Blocks.RED_MUSHROOM_BLOCK) && !floor.is(net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM_BLOCK)
   && !floor.is(net.minecraft.world.level.block.Blocks.MUSHROOM_STEM) && !floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
   && !floor.is(net.minecraft.world.level.block.Blocks.CACTUS) && !floor.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
   && !floor.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE);
 }
 private static void cleanup() { destination=null; destinationCandidates.clear(); destinationLoads.clear(); blackout=false; if(goat!=null)goat.discard(); goat=null; phase=0; target=null; ticks=0; }
}









