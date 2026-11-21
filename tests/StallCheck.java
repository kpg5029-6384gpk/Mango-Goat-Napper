package dev.mangogoatnapper;
public class StallCheck {
 public static void main(String[] args) {
  var t=new StallTracker(); t.reset(0,0,0,30);
  for(int i=0;i<1000;i++) if(t.update(i*0.1,0,0,30)) throw new AssertionError("moving player triggered fallback");
  t.reset(0,0,0,30);
  for(int i=0;i<200;i++) if(t.update(0,0,0,30-i*0.04)) throw new AssertionError("approaching goat triggered fallback");
  t.reset(0,0,0,30);
  for(int i=0;i<159;i++) if(t.update(0,0,0,30)) throw new AssertionError("early fallback");
  if(!t.update(0,0,0,30)) throw new AssertionError("fallback missing");
  System.out.println("Moving, approaching, and stalled chase checks passed");
 }
}
