package co.kr.badara.dolswevoice;
import android.content.*;import android.net.*;import android.net.wifi.*;import android.location.LocationManager;import android.os.*;import java.io.*;import java.util.*;
/** Uses existing Wi-Fi association, never requests GPS fixes or connects to another network. */
final class WifiGate {
 private final Context ctx;private final Config cfg;private final ConnectivityManager cm;private final Object changed=new Object();private ConnectivityManager.NetworkCallback callback;private volatile boolean stopped=true;private BroadcastReceiver clockReceiver;private HandlerThread monitor;private Handler handler;
 /** Publish a completed decision atomically; readers never see a refresh's intermediate state. */
 private static final class State {final Network network;final String reason;final long checked;State(Network n,String r){network=n;reason=r;checked=SystemClock.elapsedRealtime();}}
 private volatile State state=new State(null,"집 와이파이 확인 중");
 // Callback capabilities are authoritative for that network; global WifiInfo can describe another connection.
 private final Map<Network,NetworkCapabilities> capabilities=new HashMap<>();private final Set<Network> lost=new HashSet<>();
 private final Runnable recheck=()->{refresh();synchronized(changed){changed.notifyAll();}};
 private android.app.PendingIntent alarm(){return android.app.PendingIntent.getBroadcast(ctx,71,new Intent(ctx,QuietHoursReceiver.class),android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE);}
 private void schedule(){android.app.AlarmManager alarms=ctx.getSystemService(android.app.AlarmManager.class);alarms.cancel(alarm());if(cfg.quietEnabled()){long next=QuietHours.nextBoundary(System.currentTimeMillis(),java.time.ZoneId.systemDefault(),cfg.quietStart(),cfg.quietEnd());if(next>0)alarms.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP,next,alarm());}}
 WifiGate(Context context,Config config){ctx=context.getApplicationContext();cfg=config;cm=ctx.getSystemService(ConnectivityManager.class);}
 boolean configured(){return !cfg.ssid().isEmpty();}
 private synchronized void refresh(){if(!stopped)state=inspect();}
 private State denied(String reason){return new State(null,reason);}
 private State inspect(){
  if(cfg.quietNow())return denied("음성인식 금지 시간 · "+QuietHours.format(cfg.quietStart())+"~"+QuietHours.format(cfg.quietEnd()));
  if(!configured())return denied("설정에서 집 와이파이를 저장해주세요");
  try{
   if(ctx.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return denied("와이파이 이름 확인을 위해 정확한 위치 권한을 허용해주세요");
   LocationManager lm=ctx.getSystemService(LocationManager.class);if(!lm.isLocationEnabled())return denied("와이파이 이름 확인을 위해 휴대폰 위치 기능을 켜주세요");
   WifiManager wm=ctx.getSystemService(WifiManager.class);if(!wm.isWifiEnabled())return denied("집 와이파이 미연결");
   String legacy=WifiPolicy.ssid(wm.getConnectionInfo().getSSID());boolean identified=false;
   for(Network n:cm.getAllNetworks()){
    if(lost.contains(n))continue;
    NetworkCapabilities cap=capabilities.get(n);if(cap==null)cap=cm.getNetworkCapabilities(n);
    if(cap==null||!cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN))continue;
    String current="";if(cap.getTransportInfo() instanceof WifiInfo)current=WifiPolicy.ssid(((WifiInfo)cap.getTransportInfo()).getSSID());
    if(current.isEmpty())current=legacy;if(current.isEmpty())continue;identified=true;
    // Local Wi-Fi is sufficient: internet validation/default cellular route is irrelevant to offline recognition.
    if(WifiPolicy.matches(cfg.ssid(),current))return new State(n,"집 와이파이 연결 · "+cfg.ssid());
   }
   return denied(identified?"집 와이파이 미연결":legacy.isEmpty()?"와이파이 이름을 확인할 수 없어요":"와이파이 연결 대기");
  }catch(Exception error){return denied("와이파이 연결과 권한을 확인해주세요");}
 }
 private synchronized void queueRefresh(){if(stopped||handler==null)return;handler.removeCallbacks(recheck);handler.post(recheck);}
 void start(){stopped=false;monitor=new HandlerThread("voice-wifi");monitor.start();handler=new Handler(monitor.getLooper());refresh();schedule();clockReceiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(stopped)return;queueRefresh();schedule();}};IntentFilter times=new IntentFilter(QuietHoursReceiver.ACTION);times.addAction(Intent.ACTION_TIME_CHANGED);times.addAction(Intent.ACTION_TIMEZONE_CHANGED);if(Build.VERSION.SDK_INT>=33)ctx.registerReceiver(clockReceiver,times,Context.RECEIVER_NOT_EXPORTED);else ctx.registerReceiver(clockReceiver,times);
  callback=new ConnectivityManager.NetworkCallback(){
   public void onAvailable(Network n){synchronized(WifiGate.this){lost.remove(n);}} // Wait for onCapabilitiesChanged before evaluating.
   public void onCapabilitiesChanged(Network n,NetworkCapabilities cap){synchronized(WifiGate.this){if(stopped)return;lost.remove(n);capabilities.put(n,cap);}queueRefresh();}
   public void onLost(Network n){synchronized(WifiGate.this){if(stopped)return;capabilities.remove(n);lost.add(n);if(n.equals(state.network))state=denied("집 와이파이 미연결");}queueRefresh();}
  };
  if(Build.VERSION.SDK_INT>=31){final ConnectivityManager.NetworkCallback events=callback;callback=new ConnectivityManager.NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO){public void onAvailable(Network n){events.onAvailable(n);}public void onCapabilitiesChanged(Network n,NetworkCapabilities cap){events.onCapabilitiesChanged(n,cap);}public void onLost(Network n){events.onLost(n);}};}
  try{cm.registerNetworkCallback(new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),callback);}catch(Exception e){state=denied("와이파이 상태 감시를 시작하지 못했어요");}
 }
 boolean inside(){if(stopped||cfg.quietNow())return false;if(SystemClock.elapsedRealtime()-state.checked>500)refresh();return !stopped&&state.network!=null;}
 String status(){if(cfg.quietNow())return "음성인식 금지 시간 · "+QuietHours.format(cfg.quietStart())+"~"+QuietHours.format(cfg.quietEnd())+" · 마이크 중지";inside();State s=state;return s.reason+(stopped||s.network==null?" · 마이크 중지":" · 호출 대기");}
 void await()throws InterruptedException{synchronized(changed){changed.wait(1000);}}
 synchronized void stop(){stopped=true;state=denied("대기 중지");if(handler!=null)handler.removeCallbacksAndMessages(null);if(monitor!=null)monitor.quitSafely();handler=null;monitor=null;capabilities.clear();lost.clear();ctx.getSystemService(android.app.AlarmManager.class).cancel(alarm());if(clockReceiver!=null)try{ctx.unregisterReceiver(clockReceiver);}catch(Exception ignored){}clockReceiver=null;if(callback!=null)try{cm.unregisterNetworkCallback(callback);}catch(Exception ignored){}callback=null;synchronized(changed){changed.notifyAll();}}
 Network requireNetwork()throws IOException{refresh();State s=state;if(stopped||s.network==null)throw new IOException(s.reason+". 집 와이파이에서만 HA에 연결해요.");return s.network;}
 static String currentSsid(Context c){try{return WifiPolicy.ssid(c.getSystemService(WifiManager.class).getConnectionInfo().getSSID());}catch(Exception e){return "";}}
}
