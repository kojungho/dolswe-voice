package co.kr.badara.dolswevoice;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;import android.net.*;import java.lang.reflect.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Real Wi-Fi, concurrent refresh and real microphone continuity; no HA control. */
public final class AndroidWifiStabilityTests extends Instrumentation {
 int count;void ok(boolean b,String why){count++;if(!b)throw new AssertionError(why);}
 Object field(Object obj,String name)throws Exception{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle report=new Bundle();Context ctx=getTargetContext();Config cfg=new Config(ctx);String old=cfg.ssid();boolean quiet=cfg.quietEnabled();WifiGate gate=null;ExecutorService workers=Executors.newFixedThreadPool(5);try{
  cfg.p.edit().putString("homeSsid","AndroidWifi").putBoolean("quietEnabled",false).commit();ctx.startForegroundService(new Intent(ctx,VoiceService.class).putExtra("mode","test"));Thread.sleep(1200);
  String ssid=WifiGate.currentSsid(ctx);ok(!ssid.isEmpty(),"Wi-Fi SSID missing");cfg.p.edit().putString("homeSsid",ssid).putBoolean("quietEnabled",false).commit();
  gate=new WifiGate(ctx,cfg);gate.start();ok(gate.inside(),gate.status());
  Method refresh=WifiGate.class.getDeclaredMethod("refresh");refresh.setAccessible(true);final WifiGate g=gate;
  // Four readers observe while a fifth thread repeatedly performs real Android binder queries.
  AtomicInteger denied=new AtomicInteger(),reads=new AtomicInteger();AtomicBoolean run=new AtomicBoolean(true);java.util.List<Future<?>> jobs=new java.util.ArrayList<>();
  for(int t=0;t<4;t++)jobs.add(workers.submit(()->{while(run.get()){if(!g.inside())denied.incrementAndGet();reads.incrementAndGet();Thread.yield();}}));
  Future<?> probe=workers.submit(()->{try{for(int i=0;i<500;i++)refresh.invoke(g);}catch(Exception e){throw new RuntimeException(e);}finally{run.set(false);}});probe.get(40,TimeUnit.SECONDS);for(Future<?> job:jobs)job.get(5,TimeUnit.SECONDS);
  ok(reads.get()>1000,"Insufficient concurrent reads "+reads.get());ok(denied.get()==0,"False disconnects during refresh: "+denied.get()+" / "+reads.get());
  ConnectivityManager cm=ctx.getSystemService(ConnectivityManager.class);Network network=g.requireNetwork();NetworkCapabilities cap=cm.getNetworkCapabilities(network);ConnectivityManager.NetworkCallback callback=(ConnectivityManager.NetworkCallback)field(g,"callback");
  // Losing another Wi-Fi network must not invalidate the active home connection.
  Network unrelated=Network.fromNetworkHandle((9999L<<32)|0xcafed00dL);callback.onLost(unrelated);ok(g.inside(),"Unrelated lost network invalidated home");
  ctx.startForegroundService(new Intent(ctx,VoiceService.class).putExtra("mode","test"));AudioManager audio=ctx.getSystemService(AudioManager.class);
  for(int i=0;i<100&&audio.getActiveRecordingConfigurations().isEmpty();i++)Thread.sleep(200);ok(!audio.getActiveRecordingConfigurations().isEmpty(),"Mic did not start: "+VoiceService.status);
  int session=audio.getActiveRecordingConfigurations().get(0).getClientAudioSessionId();boolean continuous=true;
  for(int i=0;i<120;i++){callback.onCapabilitiesChanged(network,cap);java.util.List<AudioRecordingConfiguration> active=audio.getActiveRecordingConfigurations();if(active.isEmpty()||active.get(0).getClientAudioSessionId()!=session)continuous=false;Thread.sleep(100);}
  ok(continuous,"Microphone restarted during stable Wi-Fi");ok(VoiceService.running,"Service stopped during stable Wi-Fi");
  // Actual selected-network loss closes the gate immediately, not a grace-period allow.
  callback.onLost(network);ok(!g.inside(),"Actual Wi-Fi loss accepted");callback.onCapabilitiesChanged(network,cap);Thread.sleep(800);ok(g.inside(),"Known home Wi-Fi did not recover");
  cfg.p.edit().putString("homeSsid",ssid+"-other").commit();Thread.sleep(650);ok(!g.inside(),"Different SSID accepted");cfg.p.edit().putString("homeSsid",ssid).commit();Thread.sleep(650);ok(g.inside(),"SSID restore failed");
  g.stop();ok(!g.inside(),"Stopped gate accepted");callback.onCapabilitiesChanged(network,cap);Thread.sleep(650);ok(!g.inside(),"Late callback reopened stopped gate");gate=null;
  report.putString("stream","ANDROID_WIFI_STABILITY_OK "+count+" checks; 500 real refreshes / "+reads.get()+" concurrent reads, zero false disconnects; 12-second microphone session continuity; unrelated loss, actual loss/recovery, wrong SSID, late callbacks");finish(Activity.RESULT_OK,report);
 }catch(Throwable e){report.putString("stream","ANDROID_WIFI_STABILITY_FAIL "+e+" voice="+VoiceService.status);finish(Activity.RESULT_CANCELED,report);}finally{workers.shutdownNow();ctx.stopService(new Intent(ctx,VoiceService.class));if(gate!=null)gate.stop();cfg.p.edit().putString("homeSsid",old).putBoolean("quietEnabled",quiet).commit();}}
}
