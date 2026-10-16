package co.kr.badara.dolswevoice;
import java.time.*;import java.util.Locale;
/** Daily local-clock schedule, start inclusive and end exclusive. */
public final class QuietHours {
 public static boolean valid(int start,int end){return start>=0&&start<1440&&end>=0&&end<1440&&start!=end;}
 public static boolean active(boolean enabled,int start,int end,int minute){if(!enabled)return false;if(!valid(start,end)||minute<0||minute>=1440)return true;return start<end?minute>=start&&minute<end:minute>=start||minute<end;}
 public static String format(int minute){return String.format(Locale.US,"%02d:%02d",minute/60,minute%60);}
 public static int minute(){ZonedDateTime now=ZonedDateTime.now();return now.getHour()*60+now.getMinute();}
 public static long nextBoundary(long now,ZoneId zone,int start,int end){if(!valid(start,end))return 0;LocalDate date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();long next=Long.MAX_VALUE;for(int day=0;day<3;day++)for(int m:new int[]{start,end}){long candidate=date.plusDays(day).atTime(m/60,m%60).atZone(zone).toInstant().toEpochMilli();if(candidate>now)next=Math.min(next,candidate);}return next;}
}
