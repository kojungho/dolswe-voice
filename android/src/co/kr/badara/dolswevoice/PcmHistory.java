package co.kr.badara.dolswevoice;
/** Bounded original PCM indexed in the same sample clock as the VAD. */
public final class PcmHistory {
 private final float[] data;private long end;
 public PcmHistory(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");data=new float[capacity];}
 public void append(float[] samples){for(float sample:samples){data[(int)(end%data.length)]=sample;end++;}}
 public void reset(){end=0;}
 public float[] segment(int start,float[] fallback){
  long finish=(long)start+fallback.length,oldest=Math.max(0,end-data.length);
  // Never splice a partially overwritten utterance or a segment from a different clock.
  if(start<oldest||finish>end||start<0)return padded(fallback);
  long begin=Math.max(oldest,Math.max(0,(long)start-6400));
  float[] result=new float[3200+(int)(finish-begin)];
  for(long i=begin;i<finish;i++)result[3200+(int)(i-begin)]=data[(int)(i%data.length)];
  return result;
 }
 private static float[] padded(float[] samples){float[] result=new float[3200+samples.length];System.arraycopy(samples,0,result,3200,samples.length);return result;}
}
