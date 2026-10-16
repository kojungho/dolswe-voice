package co.kr.badara.dolswevoice;
import java.util.*;import org.json.*;
public final class ButtonTests {
 static int checks;static void ok(boolean x){checks++;if(!x)throw new AssertionError("Button check "+checks);}interface Task{void run()throws Exception;}static void fails(Task t)throws Exception{boolean fail=false;try{t.run();}catch(Exception e){fail=true;}ok(fail);}
 static class Fixture implements HaClient.Transport {
  boolean allowed=true,group=false,offline=false;String state="unknown",path="";int presses;JSONObject payload;
  public String request(String method,String path,JSONObject body)throws Exception {
   if(offline)throw new java.io.IOException("offline");
   if(path.equals("/api/template"))return allowed?"[\"button.cleaner\",\"script.hidden\"]":"[]";
   JSONObject attrs=new JSONObject().put("friendly_name","공기청정기 버튼");if(group)attrs.put("entity_id",new JSONArray().put("button.other"));
   JSONObject entity=new JSONObject().put("entity_id","button.cleaner").put("state",state).put("attributes",attrs);
   if(path.equals("/api/states"))return new JSONArray().put(entity).put(new JSONObject().put("entity_id","button.hidden").put("state","unknown")).toString();
   if(path.equals("/api/states/button.cleaner"))return entity.toString();
   if(path.startsWith("/api/services/")){presses++;this.path=path;payload=body;return "[]";}
   throw new java.io.IOException("unexpected request "+path);
  }
 }
 public static void main(String[] args)throws Exception {
  List<CommandRouter.Target> targets=Arrays.asList(new CommandRouter.Target("button.cleaner","공기청정기 버튼, 청정 버튼"),new CommandRouter.Target("light.hall","복도 등"));
  for(String word:Arrays.asList("눌러","눌러 줘","눌러주세요","누르기","실행","실행해","실행해 줘","실행해주세요","작동","작동시켜줘"))ok(CommandRouter.plan("공기청정기 버튼 "+word,targets).service.equals("press"));
  ok(CommandRouter.plan("청정 버튼 눌러줘",targets).target.id.equals("button.cleaner"));
  CommandRouter.Wake wake=CommandRouter.wake("이모님청정버튼실행해줘","돌쇠야, 이모님");ok(wake.matched&&CommandRouter.plan(wake.remaining,targets).service.equals("press"));
  for(String word:Arrays.asList("켜줘","꺼줘","열어줘","닫아줘","누르지마","안 눌러","실행하지마","실행 취소","눌러줘 복도 등 켜줘","눌러줘 눌러줘"))fails(()->CommandRouter.plan("공기청정기 버튼 "+word,targets));
  fails(()->CommandRouter.plan("복도 등 눌러줘",targets));
  CommandRouter.Plan press=CommandRouter.plan("청정 버튼 눌러줘",targets),query=CommandRouter.plan("청정 버튼 상태 알려줘",targets);
  Fixture f=new Fixture();HaClient client=new HaClient(f);ok(client.list("voicecall").size()==1);ok(client.allowed("voicecall").equals(Collections.singleton("button.cleaner")));
  ok(client.execute("voicecall",query,false).contains("아직 누른 기록"));ok(f.presses==0);
  ok(client.execute("voicecall",press,true).contains("버튼 누르기"));ok(f.presses==0);
  // 'unknown' means never pressed, not disconnected. Send exactly one button.press.
  client.execute("voicecall",press,false);ok(f.presses==1);ok(f.path.equals("/api/services/button/press"));ok(f.payload.length()==1&&f.payload.getString("entity_id").equals("button.cleaner"));
  f.state="2026-10-16T10:00:00+00:00";ok(client.execute("voicecall",query,false).contains("마지막 누름 기록"));ok(f.presses==1);
  f.state="unavailable";fails(()->client.execute("voicecall",press,false));ok(f.presses==1);f.state="unknown";
  f.allowed=false;fails(()->client.execute("voicecall",press,false));ok(f.presses==1);f.allowed=true;
  f.group=true;ok(client.list("voicecall").isEmpty());fails(()->client.execute("voicecall",press,false));ok(f.presses==1);f.group=false;
  fails(()->client.execute("voicecall",press,false,()->false));ok(f.presses==1);
  final int[] calls={0};fails(()->client.execute("voicecall",press,false,()->++calls[0]<3));ok(f.presses==1);
  f.offline=true;fails(()->client.execute("voicecall",press,false));ok(f.presses==1);f.offline=false;
  fails(()->client.execute("voicecall",new CommandRouter.Plan(targets.get(0),"turn_on"),false));ok(f.presses==1);
  System.out.println(checks+" button checks passed: press routing, label discovery, never-pressed unknown, dry-run, state query, single POST, cancellation and home/label/group gating.");
 }
}
