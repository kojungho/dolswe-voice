package co.kr.badara.dolswevoice;
import android.content.*;import android.location.*;import android.os.*;
final class HomeGate implements LocationListener {
 private final Config cfg;private final LocationManager manager;private volatile Location fix;private volatile String error="집 위치를 확인하고 있어요";private final Object changed=new Object();
 HomeGate(Context ctx,Config cfg){this.cfg=cfg;manager=ctx.getSystemService(LocationManager.class);}
 boolean enabled(){return cfg.p.getBoolean("homeOnly",true);}
 boolean configured(){return cfg.p.getBoolean("homeSet",false)&&HomeRegion.valid(lat(),lon(),radius());}
 double lat(){return Double.longBitsToDouble(cfg.p.getLong("homeLat",0));}double lon(){return Double.longBitsToDouble(cfg.p.getLong("homeLon",0));}int radius(){return cfg.p.getInt("homeRadius",200);}
 void start(){if(!enabled())return;if(!configured()){error="설정에서 집 위치와 반경을 먼저 저장해주세요";return;}try{
   if(!manager.isLocationEnabled()){error="휴대폰 위치 기능이 꺼져 있어요";return;}
   if(!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){error="네트워크 위치 기능을 켜주세요";return;}
   Location cached=manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);if(cached!=null)fix=new Location(cached);
   manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,120000,25,this,Looper.getMainLooper());
   manager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,120000,25,this,Looper.getMainLooper());
  }catch(Exception e){error="위치 권한과 휴대폰 위치 기능을 확인해주세요";fix=null;}}
 boolean inside(){if(!enabled())return true;Location x=fix;if(x==null||!manager.isLocationEnabled())return false;return configured()&&x.hasAccuracy()&&HomeRegion.inside(lat(),lon(),radius(),x.getLatitude(),x.getLongitude(),x.getAccuracy(),(SystemClock.elapsedRealtimeNanos()-x.getElapsedRealtimeNanos())/1000000);}
 String status(){Location x=fix;if(x==null)return error+" · 마이크 중지";if(!manager.isLocationEnabled())return "휴대폰 위치 꺼짐 · 마이크 중지";long age=(SystemClock.elapsedRealtimeNanos()-x.getElapsedRealtimeNanos())/1000000;if(age>HomeRegion.MAX_AGE_MS)return "최근 위치 확인 대기 · 마이크 중지";double distance=HomeRegion.distance(x.getLatitude(),x.getLongitude(),lat(),lon());return inside()?"집 안 · 호출 대기":"집 밖 또는 위치 오차 범위 · 마이크 중지 (약 "+Math.round(distance)+"m)";}
 public void onLocationChanged(Location location){Location current=fix;if(current==null||location.getElapsedRealtimeNanos()>current.getElapsedRealtimeNanos()){fix=new Location(location);synchronized(changed){changed.notifyAll();}}}
 public void onProviderDisabled(String provider){fix=null;error="위치 제공 기능이 꺼져 있어요";synchronized(changed){changed.notifyAll();}}
 public void onProviderEnabled(String provider){if(provider.equals(LocationManager.NETWORK_PROVIDER)){stop();start();}}
 public void onStatusChanged(String provider,int status,Bundle extras){}
 void await()throws InterruptedException{synchronized(changed){changed.wait(15000);}}
 void stop(){try{manager.removeUpdates(this);}catch(Exception ignored){}synchronized(changed){changed.notifyAll();}}
}
