package co.kr.badara.dolswevoice;
import android.app.*;import android.content.*;import android.os.*;import java.lang.reflect.*;
/** Production foreground scheduling and actual HTTPS metadata request; no HA calls. */
public final class AndroidUpdateLifecycleTests extends Instrumentation {
 int count;void ok(boolean b,String why){count++;if(!b)throw new AssertionError(why);}
 Object get(Object o,String f)throws Exception{Field x=o.getClass().getDeclaredField(f);x.setAccessible(true);return x.get(o);}
 void set(Object o,String f,Object value)throws Exception{Field x=o.getClass().getDeclaredField(f);x.setAccessible(true);x.set(o,value);}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle report=new Bundle();MainActivity a=null;try{
  Context c=getTargetContext();Config cfg=new Config(c);boolean old=cfg.p.getBoolean("autoUpdate",true);cfg.p.edit().putBoolean("autoUpdate",false).commit();
  a=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final MainActivity activity=a;AppUpdates u=(AppUpdates)get(a,"updates");UpdateCheckPolicy p=(UpdateCheckPolicy)get(u,"policy");
  Thread.sleep(1400);ok((Long)get(p,"last")==-1,"disabled auto check ran");
  runOnMainSync(()->{u.resume(true);u.pause();});Thread.sleep(1300);ok((Long)get(p,"last")==-1,"background did not cancel delayed check");
  cfg.p.edit().putBoolean("autoUpdate",true).commit();runOnMainSync(()->{callActivityOnPause(activity);callActivityOnResume(activity);});
  for(int i=0;i<40&&(Long)get(p,"last")==-1;i++)Thread.sleep(100);ok((Long)get(p,"last")>=0,"foreground did not schedule check");long checked=(Long)get(p,"last");
  for(int i=0;i<400&&(Boolean)get(u,"busy");i++)Thread.sleep(100);ok(!(Boolean)get(u,"busy"),"HTTPS check timed out");ok(u.status().contains("최신 버전"),"metadata request: "+u.status());
  runOnMainSync(()->{callActivityOnPause(activity);callActivityOnResume(activity);});Thread.sleep(1300);ok((Long)get(p,"last")==checked,"rapid return repeated request");
  // Force a due interval without a minute wait, then test the actual resume path again.
  set(p,"last",SystemClock.elapsedRealtime()-60001);runOnMainSync(()->{callActivityOnPause(activity);callActivityOnResume(activity);});Thread.sleep(1500);ok((Long)get(p,"last")>checked,"later return did not check again");
  runOnMainSync(()->u.close());set(p,"last",-1L);runOnMainSync(()->u.resume(true));Thread.sleep(1200);ok((Long)get(p,"last")==-1,"closed activity started request");
  cfg.p.edit().putBoolean("autoUpdate",old).commit();report.putString("stream","ANDROID_UPDATE_LIFECYCLE_OK "+count+" checks; launch-disabled, pause-cancel, foreground HTTPS metadata, repeat suppression, later retry, closed cancellation");finish(Activity.RESULT_OK,report);
 }catch(Throwable e){report.putString("stream","ANDROID_UPDATE_LIFECYCLE_FAIL "+e);finish(Activity.RESULT_CANCELED,report);}finally{if(a!=null){final Activity done=a;runOnMainSync(()->done.finish());}}}
}
