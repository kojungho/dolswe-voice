package co.kr.badara.dolswevoice;
import android.app.*;import android.content.*;import android.media.*;import android.os.*;
/** Real emulator Wi-Fi association and real foreground microphone lifecycle, no HA control. */
public final class AndroidWifiTests extends Instrumentation {
 private int checks;private void ok(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 private void waitMic(AudioManager audio,boolean expected)throws Exception{for(int i=0;i<60;i++){if((!audio.getActiveRecordingConfigurations().isEmpty())==expected)return;Thread.sleep(200);}throw new AssertionError("Microphone expected="+expected+" status="+VoiceService.status);}
 private void shell(String command)throws Exception{android.os.ParcelFileDescriptor p=getUiAutomation().executeShellCommand(command);try(java.io.InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(p)){byte[] bytes=new byte[1024];while(in.read(bytes)!=-1){}}}
 public void onStart(){Bundle report=new Bundle();WifiGate gate=null;Context ctx=getTargetContext();Config cfg=new Config(ctx);String old=cfg.ssid();try{
  cfg.p.edit().putString("homeSsid","AndroidWifi").commit();ctx.startForegroundService(new Intent(ctx,VoiceService.class).putExtra("mode","test"));Thread.sleep(1000);String actual=WifiGate.currentSsid(ctx);ok(!actual.isEmpty(),"SSID permission/association: "+actual);cfg.p.edit().putString("homeSsid",actual).commit();
  gate=new WifiGate(ctx,cfg);gate.start();ok(gate.configured(),"configured");ok(gate.inside(),gate.status());
  cfg.p.edit().putString("homeSsid",actual+"-wrong").commit();Thread.sleep(700);ok(!gate.inside(),"Wrong SSID accepted");
  cfg.p.edit().putString("homeSsid",actual).commit();Thread.sleep(700);ok(gate.inside(),"Matching SSID rejected");
  ctx.startForegroundService(new Intent(ctx,VoiceService.class).putExtra("mode","test"));AudioManager audio=ctx.getSystemService(AudioManager.class);waitMic(audio,true);ok(VoiceService.running,"Service did not start");
  cfg.p.edit().putString("homeSsid",actual+"-wrong").commit();waitMic(audio,false);ok(VoiceService.running,"Should keep foreground notification for resuming");
  cfg.p.edit().putString("homeSsid",actual).commit();waitMic(audio,true);ok(VoiceService.running,"Microphone did not resume");
  shell("cmd wifi set-wifi-enabled disabled");waitMic(audio,false);ok(VoiceService.running,"Service stopped on Wi-Fi disconnect");
  shell("cmd wifi set-wifi-enabled enabled");shell("cmd wifi connect-network AndroidWifi open");waitMic(audio,true);ok(VoiceService.running,"Service did not resume after reconnect");
  ctx.stopService(new Intent(ctx,VoiceService.class));waitMic(audio,false);ok(true,"stopped");
  // GPS coordinates are irrelevant and no last-known fix is required.
  cfg.p.edit().putBoolean("homeSet",false).commit();Thread.sleep(600);ok(gate.inside(),"Wi-Fi should not require home coordinates");gate.stop();ok(!gate.inside(),"Stopped gate should deny");gate=null;
  report.putString("stream","ANDROID_WIFI_OK "+checks+" checks; actual SSID="+actual+"; real microphone start/suspend/resume/stop; no HA service calls");finish(Activity.RESULT_OK,report);
 }catch(Throwable e){report.putString("stream","ANDROID_WIFI_FAIL "+e+" status="+VoiceService.status);finish(Activity.RESULT_CANCELED,report);}finally{ctx.stopService(new Intent(ctx,VoiceService.class));if(gate!=null)gate.stop();cfg.p.edit().putString("homeSsid",old).commit();}}
}
