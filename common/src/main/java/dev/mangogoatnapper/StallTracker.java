package dev.mangogoatnapper;

/** Detects a stationary target and a pursuer that has stopped closing the gap. */
final class StallTracker {
 private double x,y,z,bestDistance;
 private int stillTicks;
 void reset(double x,double y,double z,double distance) {
  this.x=x; this.y=y; this.z=z; bestDistance=distance; stillTicks=0;
 }
 boolean update(double x,double y,double z,double distance) {
  double dx=x-this.x, dy=y-this.y, dz=z-this.z;
  if(dx*dx+dy*dy+dz*dz>9 || distance<bestDistance-2) {
   reset(x,y,z,distance); return false;
  }
  return ++stillTicks>=160;
 }
}
