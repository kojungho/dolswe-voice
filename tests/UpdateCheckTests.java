package co.kr.badara.dolswevoice;
public class UpdateCheckTests {
 static int n;static void ok(boolean value){n++;if(!value)throw new AssertionError("Check "+n);}
 public static void main(String[] args){
  UpdateCheckPolicy p=new UpdateCheckPolicy();
  ok(p.due(0,false,false));ok(!p.due(0,true,false));ok(!p.due(0,false,true));
  p.started(1000);ok(!p.due(1000,false,false));ok(!p.due(60999,false,false));ok(p.due(61000,false,false));
  ok(!p.due(61000,true,false));ok(!p.due(61000,false,true));
  // A failed check/cancelled download must not loop on immediate foreground return.
  p.started(61000);ok(!p.due(61001,false,false));ok(p.due(121000,false,false));
  // Manual checks record their start and suppress duplicate automatic checks too.
  p.started(70000);ok(!p.due(121000,false,false));ok(p.due(130000,false,false));
  System.out.println(n+" checks passed: first launch, foreground retries, boundaries, busy/install suppression, manual-check cooldown.");
 }
}
