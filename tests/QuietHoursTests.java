package co.kr.badara.dolswevoice;
import java.time.*;
public final class QuietHoursTests {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("Quiet hours check "+checks);}
 static long utc(String s){return Instant.parse(s).toEpochMilli();}
 public static void main(String[] args){
  int start=22*60,end=7*60;
  ok(!QuietHours.active(false,start,end,23*60));ok(QuietHours.valid(start,end));ok(!QuietHours.valid(0,0));ok(!QuietHours.valid(-1,400));ok(!QuietHours.valid(400,1440));
  for(int minute:new int[]{1320,1439,0,419})ok(QuietHours.active(true,start,end,minute));
  for(int minute:new int[]{420,421,1000,1319})ok(!QuietHours.active(true,start,end,minute));
  ok(!QuietHours.active(true,60,180,59));ok(QuietHours.active(true,60,180,60));ok(QuietHours.active(true,60,180,179));ok(!QuietHours.active(true,60,180,180));
  ok(QuietHours.active(true,0,0,100));ok(QuietHours.active(true,-1,100,200));ok(!QuietHours.active(false,-1,100,-1));
  int overnight=0,daytime=0;for(int m=0;m<1440;m++){if(QuietHours.active(true,start,end,m))overnight++;if(QuietHours.active(true,60,180,m))daytime++;}ok(overnight==540);ok(daytime==120);
  ok(QuietHours.format(start).equals("22:00"));ok(QuietHours.format(end).equals("07:00"));ok(QuietHours.format(0).equals("00:00"));ok(QuietHours.format(1439).equals("23:59"));
  ZoneId seoul=ZoneId.of("Asia/Seoul");
  ok(QuietHours.nextBoundary(utc("2026-10-16T12:59:59Z"),seoul,start,end)==utc("2026-10-16T13:00:00Z"));
  ok(QuietHours.nextBoundary(utc("2026-10-16T13:00:00Z"),seoul,start,end)==utc("2026-10-16T22:00:00Z"));
  ok(QuietHours.nextBoundary(utc("2026-10-16T21:59:59Z"),seoul,start,end)==utc("2026-10-16T22:00:00Z"));
  ok(QuietHours.nextBoundary(utc("2026-10-16T22:00:00Z"),seoul,start,end)==utc("2026-10-17T13:00:00Z"));
  ok(QuietHours.nextBoundary(utc("2026-12-31T14:59:59Z"),seoul,start,end)==utc("2026-12-31T22:00:00Z"));
  ok(QuietHours.nextBoundary(utc("2026-10-16T12:59:59Z"),ZoneId.of("UTC"),start,end)==utc("2026-10-16T22:00:00Z"));
  ok(QuietHours.nextBoundary(utc("2026-10-16T13:00:00Z"),seoul,0,0)==0);
  // A real DST change must follow local 22:00/07:00, not a fixed 9-hour duration.
  ZoneId ny=ZoneId.of("America/New_York");ok(QuietHours.nextBoundary(utc("2026-03-08T03:00:00Z"),ny,start,end)==utc("2026-03-08T11:00:00Z"));
  System.out.println(checks+" quiet-hours checks passed: overnight/daytime bounds, full-day minute coverage, disabled/invalid config, local time and DST scheduling.");
 }
}
