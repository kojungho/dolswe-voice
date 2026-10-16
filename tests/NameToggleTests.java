package co.kr.badara.dolswevoice;
import java.util.*;import org.json.*;
public final class NameToggleTests {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("Name toggle check "+checks);}interface Task{void run()throws Exception;}static void fails(Task t)throws Exception{boolean fail=false;try{t.run();}catch(Exception e){fail=true;}ok(fail);}
 static class Fixture implements HaClient.Transport {
  String id,state,path;boolean allowed=true,group,offline;int posts,gets;JSONObject payload;
  Fixture(String id,String state){this.id=id;this.state=state;}
  public String request(String method,String path,JSONObject body)throws Exception {
   if(offline)throw new java.io.IOException("offline");if(path.equals("/api/template"))return allowed?new JSONArray().put(id).toString():"[]";
   if(path.startsWith("/api/states/")){gets++;JSONObject a=new JSONObject();if(group)a.put("entity_id",new JSONArray().put("light.hidden"));return new JSONObject().put("entity_id",id).put("state",state).put("attributes",a).toString();}
   if(path.startsWith("/api/services/")){posts++;this.path=path;payload=body;return "[]";}throw new java.io.IOException("Unexpected request");
  }
 }
 static CommandRouter.Plan bare(String id){return CommandRouter.plan("테스트 장치",Arrays.asList(new CommandRouter.Target(id,"테스트 장치")));}
 public static void main(String[] args)throws Exception{
  for(String domain:Arrays.asList("light","switch","input_boolean","fan")){
   Fixture f=new Fixture(domain+".target","off");HaClient client=new HaClient(f);CommandRouter.Plan p=bare(f.id);ok(p.service.equals("reverse"));
   String response=client.execute("voicecall",p,false);ok(f.posts==1&&f.path.equals("/api/services/"+domain+"/turn_on"));ok(response.contains("켜기"));
   f.state="on";client.execute("voicecall",p,false);ok(f.posts==2&&f.gets==2&&f.path.endsWith("/turn_off"));ok(p.service.equals("reverse"));
   ok(f.payload.length()==1&&f.payload.getString("entity_id").equals(f.id));
  }
  for(String[] row:new String[][]{{"cover","closed","open_cover"},{"cover","open","close_cover"},{"lock","locked","unlock"},{"lock","unlocked","lock"},{"climate","off","turn_on"},{"climate","heat","turn_off"},{"climate","cool","turn_off"},{"climate","heat_cool","turn_off"},{"climate","auto","turn_off"},{"climate","dry","turn_off"},{"climate","fan_only","turn_off"},{"media_player","off","turn_on"},{"media_player","on","turn_off"},{"media_player","idle","turn_off"},{"media_player","playing","turn_off"},{"media_player","paused","turn_off"},{"media_player","buffering","turn_off"}}){Fixture f=new Fixture(row[0]+".target",row[1]);new HaClient(f).execute("voicecall",bare(f.id),false,()->true,true,"");ok(f.posts==1&&f.path.equals("/api/services/"+row[0]+"/"+row[2]));}
  for(String[] row:new String[][]{{"cover","opening"},{"cover","closing"},{"lock","locking"},{"lock","unlocking"},{"lock","jammed"},{"light","unknown"},{"switch","unavailable"},{"fan","other"},{"climate","unknown"},{"media_player","unknown"}}){Fixture f=new Fixture(row[0]+".target",row[1]);fails(()->new HaClient(f).execute("voicecall",bare(f.id),false,()->true,true,""));ok(f.posts==0);}
  Fixture lock=new Fixture("lock.front","locked");HaClient client=new HaClient(lock);fails(()->client.execute("voicecall",bare(lock.id),false));ok(lock.posts==0);ok(client.execute("voicecall",bare(lock.id),true).contains("잠금 해제"));ok(lock.posts==0);client.execute("voicecall",bare(lock.id),false,()->true,true,"0123");ok(lock.posts==1&&lock.payload.getString("code").equals("0123"));
  Fixture f=new Fixture("light.target","on");HaClient c=new HaClient(f);CommandRouter.Plan plan=bare(f.id);ok(c.execute("voicecall",plan,true).contains("끄기"));ok(f.posts==0);
  f.allowed=false;fails(()->c.execute("voicecall",plan,false));ok(f.posts==0);f.allowed=true;f.group=true;fails(()->c.execute("voicecall",plan,false));ok(f.posts==0);f.group=false;f.offline=true;fails(()->c.execute("voicecall",plan,false));ok(f.posts==0);f.offline=false;
  fails(()->c.execute("voicecall",plan,false,()->false));ok(f.posts==0);final int[] gate={0};fails(()->c.execute("voicecall",plan,false,()->++gate[0]<3));ok(f.posts==0);
  Fixture b=new Fixture("button.target","unknown");new HaClient(b).execute("voicecall",bare(b.id),false);ok(b.posts==1&&b.path.equals("/api/services/button/press"));
  for(String domain:Arrays.asList("sensor","binary_sensor")){Fixture sensor=new Fixture(domain+".target","on");ok(bare(sensor.id).query());new HaClient(sensor).execute("voicecall",bare(sensor.id),false);ok(sensor.posts==0);}
  List<CommandRouter.Target> targets=Arrays.asList(new CommandRouter.Target("light.living","거실 불, 메인 조명"),new CommandRouter.Target("fan.bed","안방 선풍기"));
  CommandRouter.Wake wake=CommandRouter.wake("이모님거실등","돌쇠야, 이모님");ok(wake.matched&&CommandRouter.plan(wake.remaining,targets).service.equals("reverse"));ok(CommandRouter.plan("메인 조명",targets).service.equals("reverse"));ok(CommandRouter.plan("거실 불 상태 알려줘",targets).query());ok(CommandRouter.plan("거실 불 켜줘",targets).service.equals("turn_on"));
  for(String text:Arrays.asList("거실 불 좀","거실 불을","거실 불 안방 선풍기","거실 불 하지마","거실 불 취소","거실 불 꺼 켜"))fails(()->CommandRouter.plan(text,targets));
  ok(Conversation.awaitsTail("이모님", "돌쇠야, 이모님",true,false,targets));
  ok(Conversation.awaitsTail("이모님 거실 불", "돌쇠야, 이모님",true,false,targets));
  ok(Conversation.awaitsTail("거실 등", "돌쇠야, 이모님",true,true,targets));
  ok(!Conversation.awaitsTail("거실 등", "돌쇠야, 이모님",true,false,targets));
  ok(!Conversation.awaitsTail("이모님 거실 불 켜줘", "돌쇠야, 이모님",true,false,targets));
  ok(!Conversation.awaitsTail("이모님 거실 불 상태 알려줘", "돌쇠야, 이모님",true,false,targets));
  ok(!Conversation.awaitsTail("거실 불 좀", "돌쇠야, 이모님",true,true,targets));
  ok(!Conversation.awaitsTail("거실 불 안방 선풍기", "돌쇠야, 이모님",true,true,targets));
  System.out.println(checks+" name-toggle checks passed: fresh-state reverse actions, immutable plans, lock opt-in/code, moving/unknown rejection, dry-run, label/group/home gating, button press and sensor query.");
 }
}
