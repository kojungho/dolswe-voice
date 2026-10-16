package co.kr.badara.dolswevoice;
/** Foreground checks are rate limited, including failures and cancelled installers. */
final class UpdateCheckPolicy {
 static final long INTERVAL_MS=60000;
 private long last=-1;
 boolean due(long now,boolean busy,boolean installing){return !busy&&!installing&&(last<0||now-last>=INTERVAL_MS);}
 void started(long now){last=now;}
}
