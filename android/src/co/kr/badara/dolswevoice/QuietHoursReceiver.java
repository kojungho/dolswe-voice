package co.kr.badara.dolswevoice;
import android.content.*;
/** Wake a running gate at a schedule boundary; never starts a stopped voice service. */
public final class QuietHoursReceiver extends BroadcastReceiver {
 static final String ACTION="co.kr.badara.dolswevoice.CHECK_QUIET_HOURS";
 public void onReceive(Context ctx,Intent intent){if(VoiceService.running)ctx.sendBroadcast(new Intent(ACTION).setPackage(ctx.getPackageName()));}
}
