package co.kr.badara.dolswevoice;
import java.util.*;
public class ConnectedCommandTests {
 static int count;static void ok(boolean b){count++;if(!b)throw new AssertionError("Connected check "+count);}interface Task{void run()throws Exception;}static void fails(Task task)throws Exception{boolean bad=false;try{task.run();}catch(Exception e){bad=true;}ok(bad);}
 static CommandRouter.Plan route(String text,List<CommandRouter.Target> targets){CommandRouter.Wake wake=CommandRouter.wake(text,"돌쇠야, 이모님");if(!wake.matched)throw new IllegalArgumentException("Missing wake");return CommandRouter.plan(wake.remaining,targets);}
 public static void main(String[] args)throws Exception{
  List<CommandRouter.Target> t=Arrays.asList(new CommandRouter.Target("light.living","거실 불, 메인 조명"),new CommandRouter.Target("light.bed","안방 조명"),new CommandRouter.Target("cover.living","거실 커튼"),new CommandRouter.Target("lock.front","현관문, 현관 도어록"));
  ok(CommandRouter.validateAliases(t).isEmpty());ok(t.get(0).alias.equals("거실 불"));
  for(String phrase:Arrays.asList("이모님거실불켜줘","이모님거실등켜줘","이모님거실전등켜줘","이모님거실조명켜줘","돌쇠야메인조명켜줘","이 모 님 거 실 등 켜 줘","이모님거실등을켜줘","이모님거실불좀켜줘")){CommandRouter.Plan p=route(phrase,t);ok(p.target.id.equals("light.living")&&p.service.equals("turn_on"));}
  for(String phrase:Arrays.asList("돌쇠야거실등꺼줘","이모님메인불꺼줘","이모님거실불을꺼주세요"))ok(route(phrase,t).service.equals("turn_off"));
  for(String phrase:Arrays.asList("이모님안방불켜줘","이모님안방등켜줘","이모님안방전등켜줘"))ok(route(phrase,t).target.id.equals("light.bed"));
  ok(route("이모님거실커튼닫아",t).service.equals("close_cover"));ok(route("돌쇠야현관도어록열어",t).service.equals("unlock"));
  List<CommandRouter.Target> collision=Arrays.asList(new CommandRouter.Target("light.a","거실 불"),new CommandRouter.Target("light.b","거실 등"));ok(!CommandRouter.validateAliases(collision).isEmpty());fails(()->route("이모님거실등켜줘",collision));
  List<CommandRouter.Target> same=Arrays.asList(new CommandRouter.Target("light.a","거실불,거실등,거실조명"));ok(CommandRouter.validateAliases(same).isEmpty());ok(route("이모님거실등켜줘",same).target.id.equals("light.a"));
  for(String phrase:Arrays.asList("이모님거실등켜지마","이모님거실불켜줘안방등꺼줘","이모님메인불꺼줘현관문열어","이모님거실등켜줘꺼줘","다른말이모님거실불켜줘","이모님가실불켜줘"))fails(()->route(phrase,t));
  ok(!CommandRouter.validateAliases(Arrays.asList(new CommandRouter.Target("light.a",",,"))).isEmpty());ok(!CommandRouter.validateAliases(Arrays.asList(new CommandRouter.Target("light.a","가,거실불"))).isEmpty());ok(!CommandRouter.validateAliases(Arrays.asList(new CommandRouter.Target("light.a","별칭1,별칭2,별칭3,별칭4,별칭5,별칭6,별칭7,별칭8,별칭9"))).isEmpty());
  List<CommandRouter.Target> plug=Arrays.asList(new CommandRouter.Target("switch.lamp","서재 전등"));ok(route("이모님서재불켜줘",plug).target.id.equals("switch.lamp"));
  LockTests.Fixture fixture=new LockTests.Fixture();HaClient client=new HaClient(fixture);CommandRouter.Plan door=route("이모님현관문열어",t);fails(()->client.execute("voicecall",door,false));ok(fixture.posts==0);
  System.out.println(count+" connected-command checks passed: one-utterance wake+alias+action, lamp name variants, multiple aliases, collision/negation rejection, unlock policy.");
 }
}
