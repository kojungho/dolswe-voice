package co.kr.badara.dolswevoice;
import android.content.*;import android.net.*;import android.net.wifi.*;import android.location.LocationManager;import android.os.*;import java.net.*;import java.io.*;
/** Uses existing Wi-Fi association, never requests GPS fixes or connects to another network. */
final class WifiGate {
 private final Context ctx;private final Config cfg;private final ConnectivityManager cm;private final Object changed=new Object();private ConnectivityManager.NetworkCallback callback;private volatile Network network;private volatile String reason="집 와이파이 확인 중";private volatile long checked;private volatile boolean stopped;private BroadcastReceiver clockReceiver;
 private android.app.PendingIntent alarm(){return android.app.PendingIntent.getBroadcast(ctx,71,new Intent(ctx,QuietHoursReceiver.class),android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE);}
 private void schedule(){android.app.AlarmManager alarms=ctx.getSystemService(android.app.AlarmManager.class);alarms.cancel(alarm());if(cfg.quietEnabled()){long next=QuietHours.nextBoundary(System.currentTimeMillis(),java.time.ZoneId.systemDefault(),cfg.quietStart(),cfg.quietEnd());if(next>0)alarms.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP,next,alarm());}}
 WifiGate(Context context,Config config){ctx=context.getApplicationContext();cfg=config;cm=ctx.getSystemService(ConnectivityManager.class);}
 boolean configured(){return !cfg.ssid().isEmpty();}
 private synchronized void refresh(){checked=SystemClock.elapsedRealtime();network=null;
  if(cfg.quietNow()){reason="음성인식 금지 시간 · "+QuietHours.format(cfg.quietStart())+"~"+QuietHours.format(cfg.quietEnd());return;}
  if(!configured()){reason="설정에서 집 와이파이를 저장해주세요";return;}
  try{if(ctx.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){reason="와이파이 이름 확인을 위해 정확한 위치 권한을 허용해주세요";return;}
   LocationManager lm=ctx.getSystemService(LocationManager.class);if(!lm.isLocationEnabled()){reason="와이파이 이름 확인을 위해 휴대폰 위치 기능을 켜주세요";return;}
   WifiManager wm=ctx.getSystemService(WifiManager.class);String current=WifiPolicy.ssid(wm.getConnectionInfo().getSSID());
   if(current.isEmpty()){reason="와이파이 이름을 확인할 수 없어요";return;}
   if(!WifiPolicy.matches(cfg.ssid(),current)){reason="집 와이파이 미연결";return;}
   for(Network n:cm.getAllNetworks()){NetworkCapabilities cap=cm.getNetworkCapabilities(n);android.net.NetworkInfo info=cm.getNetworkInfo(n);if(cap!=null&&cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&!cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN)&&info!=null&&info.isConnected()){network=n;reason="집 와이파이 연결 · "+cfg.ssid();return;}}
   reason="와이파이 연결 대기";
  }catch(Exception error){reason="와이파이 연결과 권한을 확인해주세요";}
 }
 void start(){stopped=false;refresh();schedule();clockReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(stopped)return;refresh();schedule();synchronized(changed){changed.notifyAll();}}};IntentFilter times=new IntentFilter(QuietHoursReceiver.ACTION);times.addAction(Intent.ACTION_TIME_CHANGED);times.addAction(Intent.ACTION_TIMEZONE_CHANGED);if(Build.VERSION.SDK_INT>=33)ctx.registerReceiver(clockReceiver,times,Context.RECEIVER_NOT_EXPORTED);else ctx.registerReceiver(clockReceiver,times);callback=new ConnectivityManager.NetworkCallback(){public void onAvailable(Network n){update();}public void onCapabilitiesChanged(Network n,NetworkCapabilities c){update();}public void onLost(Network n){network=null;update();}private void update(){refresh();synchronized(changed){changed.notifyAll();}}};try{cm.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),callback);}catch(Exception e){reason="와이파이 상태 감시를 시작하지 못했어요";network=null;}}
 boolean inside(){if(stopped)return false;if(cfg.quietNow())return false;if(SystemClock.elapsedRealtime()-checked>500)refresh();return network!=null;}
 String status(){if(cfg.quietNow())return "음성인식 금지 시간 · "+QuietHours.format(cfg.quietStart())+"~"+QuietHours.format(cfg.quietEnd())+" · 마이크 중지";inside();return reason+(network==null?" · 마이크 중지":" · 호출 대기");}
 void await()throws InterruptedException{synchronized(changed){changed.wait(1000);}}
 void stop(){stopped=true;network=null;ctx.getSystemService(android.app.AlarmManager.class).cancel(alarm());if(clockReceiver!=null)try{ctx.unregisterReceiver(clockReceiver);}catch(Exception ignored){}clockReceiver=null;if(callback!=null)try{cm.unregisterNetworkCallback(callback);}catch(Exception ignored){}callback=null;synchronized(changed){changed.notifyAll();}}
 Network requireNetwork()throws IOException{refresh();if(network==null)throw new IOException(reason+". 집 와이파이에서만 HA에 연결해요.");return network;}
 static String currentSsid(Context c){try{return WifiPolicy.ssid(c.getSystemService(WifiManager.class).getConnectionInfo().getSSID());}catch(Exception e){return "";}}
}
