package co.kr.badara.dolswevoice;
import java.util.Arrays;
public final class PcmHistoryTests {
 private static int checks;private static void check(boolean b){checks++;if(!b)throw new AssertionError("PCM check "+checks);}
 private static float[] ramp(int start,int n){float[] a=new float[n];for(int i=0;i<n;i++)a[i]=start+i;return a;}
 public static void main(String[] args){
  PcmHistory h=new PcmHistory(256000);h.append(ramp(0,10000));float[] a=h.segment(7000,ramp(7000,1000));
  check(a.length==10600);check(a[3199]==0);check(a[3200]==600);check(a[a.length-1]==7999);
  a=h.segment(1000,ramp(1000,1000));check(a.length==5200);check(a[4200]==1000);
  // Circular storage must keep the correct original clock after wrapping.
  h=new PcmHistory(20000);for(int i=0;i<60;i++)h.append(ramp(i*512,512));a=h.segment(25000,ramp(25000,4000));
  check(a.length==13600);check(a[3200]==18600);check(a[a.length-1]==28999);
  // An overwritten utterance must use the given VAD samples instead of unrelated PCM.
  a=h.segment(500,ramp(500,1000));check(a.length==4200);check(a[3200]==500);
  h.reset();h.append(ramp(99,512));a=h.segment(100,ramp(199,200));check(a.length==3500);check(a[3200]==99);check(a[a.length-1]==398);
  a=h.segment(400,new float[]{7,8});check(a[3600]==499); // retained PCM, not the fallback
  a=h.segment(510,new float[]{7,8,9});check(a.length==3203);check(a[3200]==7); // future endpoint fallback
  h.reset();h.append(new float[]{1,2,3});a=h.segment(0,new float[]{1,2,3});check(a.length==3203);check(a[3200]==1);
  check(!CommandRouter.wake("글쎄야","돌쇠야, 이모님").matched);check(!CommandRouter.wake("벌쎄야","돌쇠야, 이모님").matched);check(!CommandRouter.wake("벌 수야","돌쇠야, 이모님").matched);
  System.out.println("PCM_HISTORY_OK "+checks);
 }
}
