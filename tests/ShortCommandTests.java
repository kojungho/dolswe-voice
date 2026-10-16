package co.kr.badara.dolswevoice;
import java.util.*;
public class ShortCommandTests {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("Short command check "+checks);}interface Task{void run()throws Exception;}static void fails(Task t)throws Exception{boolean bad=false;try{t.run();}catch(Exception e){bad=true;}ok(bad);}
 public static void main(String[] args)throws Exception{
  List<CommandRouter.Target> t=Arrays.asList(new CommandRouter.Target("lock.front","현관문"),new CommandRouter.Target("cover.living","거실 커튼"),new CommandRouter.Target("light.hall","복도 등"));
  for(String word:Arrays.asList("켜","켜 줘","켜주세요","닫아","닫아 줘","닫아주세요"))ok(CommandRouter.plan("현관문 "+word,t).service.equals("lock"));
  for(String word:Arrays.asList("꺼","꺼 줘","꺼주세요","열어","열어 줘","열어주세요"))ok(CommandRouter.plan("현관문 "+word,t).service.equals("unlock"));
  for(String word:Arrays.asList("켜","켜 줘","열어","열어 줘"))ok(CommandRouter.plan("거실 커튼 "+word,t).service.equals("open_cover"));
  for(String word:Arrays.asList("꺼","꺼 줘","닫아","닫아 줘"))ok(CommandRouter.plan("거실 커튼 "+word,t).service.equals("close_cover"));
  ok(CommandRouter.plan("복도 등 켜",t).service.equals("turn_on"));ok(CommandRouter.plan("복도 등 꺼",t).service.equals("turn_off"));
  for(String command:Arrays.asList("현관문 꺼 켜","현관문 열어 닫아","현관문 안 꺼","현관문 닫지마","현관문 켜지마","거실 커튼 열지마","현관문 꺼 복도 등 켜","꺼"))fails(()->CommandRouter.plan(command,t));
  LockTests.Fixture f=new LockTests.Fixture();HaClient client=new HaClient(f);CommandRouter.Plan unlock=CommandRouter.plan("현관문 꺼",t);fails(()->client.execute("voicecall",unlock,false));ok(f.posts==0);client.execute("voicecall",unlock,false,()->true,true,"");ok(f.path.equals("/api/services/lock/unlock")&&f.posts==1);
  CommandRouter.Wake wake=CommandRouter.wake("돌쇠야 현관문 열어","돌쇠야, 이모님");ok(wake.matched&&CommandRouter.plan(wake.remaining,t).service.equals("unlock"));
  System.out.println(checks+" short command checks passed: lock/cover mappings, existing light commands, negation/mixed-command rejection, unlock opt-in.");
 }
}
