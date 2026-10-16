package co.kr.badara.dolswevoice;
/** Monotonic follow-up window. No network, microphone or Android dependency. */
public final class Conversation {
 private long until;
 public boolean armed(long now){return now<until;}
 public void begin(long now,int seconds){until=now+1000L*Math.max(5,Math.min(60,seconds));}
 public void replied(long now,boolean continuous,int seconds){if(continuous)begin(now,seconds);else clear();}
 public void clear(){until=0;}
 public static boolean cancel(String text){return CommandRouter.compact(text).matches("(그만|그만해|그만해줘|그만들어|대화종료|종료|취소|됐어|이제됐어)[요]*");}
}
