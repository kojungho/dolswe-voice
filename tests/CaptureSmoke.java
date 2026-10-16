package co.kr.badara.dolswevoice;
import android.app.*;import android.os.*;import java.io.*;import java.nio.*;import java.util.*;import org.json.*;import com.k2fsa.sherpa.onnx.*;
/** Uses the actual service capture class. No HA connection or service call. */
public final class CaptureSmoke extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 private float[] wav(String name)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContext().getAssets().open(name)){byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}ByteBuffer b=ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);b.position(12);while(b.remaining()>=8){byte[] tag=new byte[4];b.get(tag);int n=b.getInt();if(new String(tag,"US-ASCII").equals("data")){float[] x=new float[n/2];for(int k=0;k<x.length;k++)x[k]=b.getShort()/32768f;return x;}b.position(b.position()+n+(n%2));}throw new IOException("No PCM data");}
 public void onStart(){Bundle report=new Bundle();SpeechEngine engine=null;SpeechCapture capture=null;Vad baseline=null;try{engine=new SpeechEngine(getTargetContext().getAssets());capture=new SpeechCapture(getTargetContext().getAssets());baseline=SpeechEngine.createVad(getTargetContext().getAssets());JSONArray results=new JSONArray();int positives=0,negatives=0,baselineMisses=0;
  for(int pass=0;pass<2;pass++)for(String file:new String[]{"dolswe.wav","imonim.wav","dolswe-slow.wav","imonim-slow.wav","negative.wav","negative-dolswe.wav"}){
   // Reuse/reset the same instances, just like the service after each response.
   capture.reset();baseline.reset();float[] wav=wav(file);String text=null,raw=null;
   for(int offset=0;offset<wav.length+16000;offset+=512){float[] frame=new float[512];if(offset<wav.length)System.arraycopy(wav,offset,frame,0,Math.min(512,wav.length-offset));capture.accept(frame);baseline.acceptWaveform(frame);float[] speech=capture.poll();if(speech!=null&&speech.length>0){if(text!=null)throw new AssertionError("Unexpected multiple segments");text=engine.recognize(speech);}if(!baseline.empty()){raw=engine.recognize(baseline.front().getSamples());baseline.pop();}}
   if(text==null)throw new AssertionError("No segment: "+file);boolean matched=CommandRouter.wake(text,"돌쇠야, 이모님").matched,expected=!file.startsWith("negative");if(matched!=expected)throw new AssertionError("Capture mismatch: "+file+" => "+text);if(expected)positives++;else negatives++;if(expected&&!CommandRouter.wake(raw,"돌쇠야, 이모님").matched)baselineMisses++;
   results.put(new JSONObject().put("file",file).put("reset_pass",pass).put("text",text).put("wake",matched).put("old_crop_text",raw));
  }
  if(baselineMisses<1)throw new AssertionError("Regression fixture no longer reproduces old failure");report.putString("stream","CAPTURE_OK "+new JSONObject().put("positive",positives).put("negative",negatives).put("old_crop_misses",baselineMisses).put("cases",results));finish(Activity.RESULT_OK,report);
 }catch(Throwable error){report.putString("stream","CAPTURE_FAIL "+error);finish(Activity.RESULT_CANCELED,report);}finally{if(capture!=null)capture.close();if(baseline!=null)baseline.release();if(engine!=null)engine.close();}}
}
