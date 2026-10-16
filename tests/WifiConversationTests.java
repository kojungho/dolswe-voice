package co.kr.badara.dolswevoice;
public final class WifiConversationTests {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("Wi-Fi/conversation check "+checks);}
 public static void main(String[] args){
  ok(WifiPolicy.matches("우리집","\"우리집\""));ok(WifiPolicy.matches("Home 5G","Home 5G"));ok(!WifiPolicy.matches("우리집","우리집-guest"));ok(!WifiPolicy.matches("Home","home"));ok(!WifiPolicy.matches("",""));ok(!WifiPolicy.matches("우리집",null));ok(!WifiPolicy.matches("우리집","<unknown ssid>"));ok(WifiPolicy.ssid(null).isEmpty());ok(WifiPolicy.ssid("\"A B\"").equals("A B"));ok(!WifiPolicy.matches("우리집","다른 집"));
  Conversation c=new Conversation();ok(!c.armed(1000));c.begin(1000,20);ok(c.armed(1001));ok(c.armed(20999));ok(!c.armed(21000));
  // The follow-up clock starts after the response, even when HA/TTS took a long time.
  c.replied(30000,true,20);ok(c.armed(49999));ok(!c.armed(50000));c.replied(45000,true,20);ok(c.armed(60000));ok(!c.armed(65000));c.replied(66000,false,20);ok(!c.armed(66001));
  c.begin(100000,20);c.clear();ok(!c.armed(100001)); // disconnect/cancel discards the session
  c.begin(100000,1);ok(c.armed(104999));ok(!c.armed(105000));c.begin(200000,100);ok(c.armed(259999));ok(!c.armed(260000));
  for(String command:new String[]{"그만","그만해","대화 종료","취소","됐어","이제 됐어요"})ok(Conversation.cancel(command));
  for(String command:new String[]{"거실 불 켜줘","안방 선풍기 꺼줘","청정 버튼 눌러줘","현관문 잠가줘","이모님"})ok(!Conversation.cancel(command));
  CommandRouter.Wake wake=CommandRouter.wake("이모님거실불켜줘","돌쇠야, 이모님");ok(wake.matched&&wake.remaining.equals("거실불켜줘"));
  System.out.println(checks+" Wi-Fi/conversation checks passed: exact SSID gating, response-based follow-up expiry, cancellation, disconnect reset, one-utterance wake+command.");
 }
}
