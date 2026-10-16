package co.kr.badara.dolswevoice;
import android.content.res.AssetManager;import com.k2fsa.sherpa.onnx.*;
/** The shared microphone/test path retains 400 ms before VAD onset and 200 ms quiet context. */
public final class SpeechCapture implements AutoCloseable {
 private final Vad vad;private final PcmHistory history=new PcmHistory(16000*16);
 public SpeechCapture(AssetManager assets){vad=SpeechEngine.createVad(assets);}
 public void accept(float[] samples){history.append(samples);vad.acceptWaveform(samples);}
 public float[] poll(){if(vad.empty())return null;SpeechSegment segment=vad.front();float[] samples=segment.getSamples();float[] result=samples.length<3200?new float[0]:history.segment(segment.getStart(),samples);vad.pop();return result;}
 public boolean detecting(){return vad.isSpeechDetected();}
 public void reset(){vad.reset();history.reset();}
 public void close(){vad.release();}
}
