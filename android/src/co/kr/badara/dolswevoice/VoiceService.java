package co.kr.badara.dolswevoice;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;import android.speech.tts.*;import java.util.*;import java.util.concurrent.*;import com.k2fsa.sherpa.onnx.*;
public final class VoiceService extends Service {
 public static final String EVENT="co.kr.badara.dolswevoice.STATUS";public static volatile boolean running=false;public static volatile String status="대기 중지";
 private volatile boolean active=false,speaking=false,ttsReady=false;private final Conversation conversation=new Conversation();private volatile long quietUntil;private volatile String mode="wake";private Thread worker;private volatile AudioRecord mic;private TextToSpeech tts;private PowerManager.WakeLock wakeLock;private Config cfg;private WifiGate home;private final Handler main=new Handler(Looper.getMainLooper());
 public android.os.IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();cfg=new Config(this);home=new WifiGate(this,cfg);NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("voice","음성 호출 대기",NotificationManager.IMPORTANCE_LOW));
  tts=new TextToSpeech(this,result->{if(result==TextToSpeech.SUCCESS){tts.setLanguage(Locale.KOREAN);Set<Voice> voices=tts.getVoices();if(voices!=null)for(Voice v:voices)if(v.getLocale().getLanguage().equals("ko")&&!v.isNetworkConnectionRequired()&&(v.getFeatures()==null||!v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))){tts.setVoice(v);ttsReady=true;break;}}});
  tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){speaking=true;}public void onDone(String id){speaking=false;}public void onError(String id){speaking=false;}});
 }
 public int onStartCommand(Intent i,int flags,int startId){mode=i==null?"wake":i.getStringExtra("mode");if(mode==null)mode="wake";if(mode.equals("command"))conversation.begin(SystemClock.elapsedRealtime(),20);else conversation.clear();
  try{int types=android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;types|=android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;startForeground(41,notification("호출 대기 준비 중…"),types);}catch(Exception e){publish("마이크·와이파이 이름 확인 권한을 허용하고 앱에서 다시 시작해주세요.");stopSelf();return START_NOT_STICKY;}
  if(active){publish(home.inside()?mode.equals("test")?"호출어 테스트: 말씀해주세요.":mode.equals("command")?"명령을 말씀해주세요.":"호출 대기 중":home.status());return START_NOT_STICKY;}
  active=true;running=true;wakeLock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"dolswe:voice");wakeLock.setReferenceCounted(false);home.start();worker=new Thread(this::listen,"voice-audio");worker.start();return START_NOT_STICKY;
 }
 private Notification notification(String text){PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent stop=PendingIntent.getBroadcast(this,1,new Intent(this,StopReceiver.class),PendingIntent.FLAG_IMMUTABLE);return new Notification.Builder(this,"voice").setSmallIcon(co.kr.badara.dolswevoice.R.drawable.voice_icon).setContentTitle("돌쇠 음성비서").setContentText(text).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"중지",stop).build()).build();}
 private void publish(String text){if(text.equals(status))return;status=text;cfg.last(text);sendBroadcast(new Intent(EVENT).setPackage(getPackageName()).putExtra("text",text));getSystemService(NotificationManager.class).notify(41,notification(text));}
 private void speak(String text){if(!active||!home.inside())return;publish(text);if(!ttsReady)return;speaking=true;tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"reply");long deadline=SystemClock.elapsedRealtime()+15000;while(active&&speaking&&home.inside()&&SystemClock.elapsedRealtime()<deadline){try{Thread.sleep(30);}catch(InterruptedException e){break;}}if(!home.inside())tts.stop();speaking=false;quietUntil=SystemClock.elapsedRealtime()+250;}
 private void releaseWake(){if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}
 private void handle(String text){
  if(!active||!home.inside()||text.isEmpty())return;
  CommandRouter.Wake wake=CommandRouter.wake(text,cfg.wakes(),cfg.p.getBoolean("nearWake",true));
  if(mode.equals("test")){
   String response="인식: "+text+". "+(wake.matched?"호출어가 일치해요.":"설정된 호출어와 달라요.");
   if(wake.matched&&!wake.remaining.isEmpty()&&!cfg.targets().isEmpty())try{CommandRouter.Plan p=plan(wake.remaining);response+=" 연결: "+p.target.alias+" "+CommandRouter.describe(p)+". 테스트 중에는 실행하지 않아요.";}catch(Exception e){response+=" "+message(e);}speak(response);return;
  }
  if(!wake.matched&&!conversation.armed(SystemClock.elapsedRealtime())){publish("호출 대기 중 · "+cfg.wakes());return;}
  String command=wake.matched?wake.remaining:text;
  if(Conversation.cancel(command)){conversation.clear();speak("대화를 마쳤어요.");return;}
  if(command.isEmpty()){speak("네, 말씀하세요.");conversation.begin(SystemClock.elapsedRealtime(),cfg.followSeconds());return;}
  conversation.clear();
  try{CommandRouter.Plan p=plan(command);speak(cfg.client().execute(cfg.label(),p,cfg.dry(),()->active&&home.inside(),cfg.p.getBoolean("allowLockUnlock",false),p.target.domain().equals("lock")?cfg.lockCode(p.target.id):""));}
  catch(Exception e){speak(message(e));}
  if(active&&home.inside())conversation.replied(SystemClock.elapsedRealtime(),cfg.continuous(),cfg.followSeconds());
  if(mode.equals("command"))mode="wake";
  if(conversation.armed(SystemClock.elapsedRealtime()))publish("추가 명령 대기 · "+cfg.followSeconds()+"초 · ‘그만’으로 종료");
 }
 private CommandRouter.Plan plan(String command)throws Exception{List<CommandRouter.Target> targets=cfg.targets();String invalid=CommandRouter.validateAliases(targets);if(!invalid.isEmpty())throw new Exception(invalid);return CommandRouter.plan(command,targets);}
 /** Audio capture runs while recognition and HA requests are processing. TTS echo is excluded. */
 private final class AudioPipe implements AutoCloseable {
  final ArrayBlockingQueue<float[]> queue=new ArrayBlockingQueue<>(4);final SpeechCapture capture;
  final AudioRecord record;final Thread reader;volatile boolean collecting=true,detecting;volatile Throwable error;
  AudioPipe()throws Exception{
   int minimum=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);if(minimum<0)throw new Exception("마이크 형식을 지원하지 않아요.");
   record=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(minimum,64000));
   if(record.getState()!=AudioRecord.STATE_INITIALIZED){record.release();throw new Exception("마이크를 초기화할 수 없어요.");}
   try{capture=new SpeechCapture(getAssets());}catch(Throwable e){record.release();throw e;}
   mic=record;reader=new Thread(this::read,"voice-capture");reader.start();
  }
  void read(){boolean discarded=false;try{record.startRecording();short[] pcm=new short[512];while(active&&collecting&&home.inside()){
    int n=record.read(pcm,0,pcm.length,AudioRecord.READ_BLOCKING);if(!active||!collecting)break;if(n<0)throw new Exception("마이크를 다른 앱에서 사용하는지 확인해주세요.");if(n==0)continue;
    if(speaking||SystemClock.elapsedRealtime()<quietUntil){if(!discarded)capture.reset();discarded=true;detecting=false;continue;}
    if(discarded){capture.reset();discarded=false;}
    float[] samples=new float[n];for(int k=0;k<n;k++)samples[k]=pcm[k]/32768f;capture.accept(samples);detecting=capture.detecting();float[] speech;
    while((speech=capture.poll())!=null)if(speech.length>0)queue.offer(speech);
   }}catch(Throwable e){if(active&&collecting)error=e;}finally{collecting=false;detecting=false;try{record.stop();}catch(Exception ignored){}record.release();if(mic==record)mic=null;capture.close();}
  }
  float[] poll(long millis)throws Exception{if(error!=null)throw new Exception(message(error));return queue.poll(millis,TimeUnit.MILLISECONDS);}
  public void close(){collecting=false;queue.clear();try{record.stop();}catch(Exception ignored){}reader.interrupt();try{reader.join(1500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
 }
 private String joinWakeTail(String text,AudioPipe pipe,SpeechEngine engine)throws Exception{
  CommandRouter.Wake wake=CommandRouter.wake(text,cfg.wakes(),cfg.p.getBoolean("nearWake",true));
  if(mode.equals("test")||!wake.matched||!wake.remaining.isEmpty())return text;
  // A short pause after the name must not cause our reply to cover the following command.
  long quietDeadline=SystemClock.elapsedRealtime()+1200,hardDeadline=SystemClock.elapsedRealtime()+10000;
  while(active&&home.inside()&&SystemClock.elapsedRealtime()<hardDeadline){
   float[] next=pipe.poll(100);if(next!=null){String tail=engine.recognize(next);if(tail.isEmpty())continue;CommandRouter.Wake second=CommandRouter.wake(tail,cfg.wakes(),cfg.p.getBoolean("nearWake",true));return second.matched?tail:text+" "+tail;}
   if(SystemClock.elapsedRealtime()>quietDeadline&&!pipe.detecting)break;
  }return text;
 }
 private void listen(){SpeechEngine engine=null;AudioPipe pipe=null;try{while(active){
   if(!home.inside()){
    if(pipe!=null){pipe.close();pipe=null;}if(engine!=null){engine.close();engine=null;}releaseWake();conversation.clear();if(mode.equals("command"))mode="wake";publish(home.status());home.await();continue;
   }
   if(engine==null){wakeLock.acquire();publish("음성 모델 준비 중…");engine=new SpeechEngine(getAssets());if(!active||!home.inside())continue;pipe=new AudioPipe();publish(mode.equals("test")?"호출어 테스트: 말씀해주세요.":"호출 대기 중 · "+cfg.wakes());}
   if(pipe==null){if(engine!=null){engine.close();engine=null;}continue;}
   float[] speech=pipe.poll(200);if(speech==null){if(!pipe.collecting&&home.inside()){pipe.close();pipe=new AudioPipe();}if(!conversation.armed(SystemClock.elapsedRealtime())&&status.startsWith("추가 명령 대기"))publish("호출 대기 중 · "+cfg.wakes());continue;}
   String text=joinWakeTail(engine.recognize(speech),pipe,engine);handle(text);
  }}catch(Throwable e){if(active)publish("중지됨 · "+message(e));}finally{active=false;running=false;if(pipe!=null)pipe.close();if(engine!=null)engine.close();releaseWake();main.post(()->stopSelf());}
 }
 public static String message(Throwable e){String s=e.getMessage();return s==null||s.trim().isEmpty()?"작업을 완료하지 못했어요. 연결과 권한을 확인해주세요.":s;}
 public void onDestroy(){active=false;running=false;if(home!=null)home.stop();AudioRecord current=mic;if(current!=null)try{current.stop();}catch(Exception ignored){}conversation.clear();if(worker!=null)worker.interrupt();if(tts!=null){tts.stop();tts.shutdown();}releaseWake();stopForeground(STOP_FOREGROUND_REMOVE);status="대기 중지";sendBroadcast(new Intent(EVENT).setPackage(getPackageName()).putExtra("text",status));super.onDestroy();}
}
