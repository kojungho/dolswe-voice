package co.kr.badara.dolswevoice;
import android.content.res.AssetManager;import com.k2fsa.sherpa.onnx.*;
/** One model configuration shared by the microphone service and native smoke test. */
public final class SpeechEngine implements AutoCloseable {
 private final OfflineRecognizer recognizer;
 public SpeechEngine(AssetManager assets){
  OfflineModelConfig model=new OfflineModelConfig();
  OfflineSenseVoiceModelConfig sense=new OfflineSenseVoiceModelConfig();sense.setModel("models/model.int8.onnx");sense.setLanguage("ko");sense.setUseInverseTextNormalization(true);model.setSenseVoice(sense);model.setTokens("models/tokens.txt");model.setNumThreads(2);model.setDebug(false);model.setProvider("cpu");OfflineRecognizerConfig rc=new OfflineRecognizerConfig();rc.setModelConfig(model);recognizer=new OfflineRecognizer(assets,rc);
 }
 public String recognize(float[] samples){OfflineStream stream=recognizer.createStream();try{stream.acceptWaveform(samples,16000);recognizer.decode(stream);return recognizer.getResult(stream).getText().trim();}finally{stream.release();}}
 public static Vad createVad(AssetManager assets){SileroVadModelConfig silero=new SileroVadModelConfig();silero.setModel("models/silero_vad.onnx");silero.setThreshold(.5f);silero.setMinSilenceDuration(.45f);silero.setMinSpeechDuration(.18f);silero.setMaxSpeechDuration(10f);VadModelConfig vc=new VadModelConfig();vc.setSileroVadModelConfig(silero);vc.setSampleRate(16000);vc.setNumThreads(1);return new Vad(assets,vc);}
 public void close(){recognizer.release();}
}
