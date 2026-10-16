package co.kr.badara.dolswevoice;
import java.util.*;
import java.util.regex.*;
import java.text.Normalizer;
/** Deliberately narrow: no generic service, wildcard, group or script execution. */
public final class CommandRouter {
 public static final Set<String> DOMAINS=new HashSet<>(Arrays.asList("light","switch","input_boolean","fan","cover","climate","media_player","sensor","binary_sensor","lock","button"));
 public static String compact(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}%]","");}
 public static final class Target {
  public final String id,alias,configuredAliases;public final List<String> aliases;
  public Target(String id,String names){this.id=id;this.configuredAliases=names==null?"":names;List<String> values=new ArrayList<>();for(String name:(names==null?"":names).split("[,，\\n]+")){name=name.trim();if(!name.isEmpty())values.add(name);}aliases=Collections.unmodifiableList(values);alias=values.isEmpty()?"":values.get(0);}
  public String domain(){return id.split("\\.")[0];}
 }
 private static Set<String> names(Target target){Set<String> out=new LinkedHashSet<>();for(String alias:target.aliases){String name=compact(alias);if(name.length()<2)continue;out.add(name);String domain=target.domain();boolean lighting=domain.equals("light")||((domain.equals("switch")||domain.equals("input_boolean"))&&name.matches(".+(?:전등|조명|불|등)$"));if(lighting){String base=name.replaceFirst("(?:전등|조명|불|등)$","");if(!base.isEmpty())for(String suffix:new String[]{"불","등","전등","조명"})out.add(base+suffix);}}return out;}
 public static final class Plan {public final Target target;public final String service;public final Map<String,Object> data=new LinkedHashMap<>();Plan(Target t,String s){target=t;service=s;data.put("entity_id",t.id);}public boolean query(){return service.isEmpty();}}
 public static String validateAliases(List<Target> list){Map<String,String> owners=new HashMap<>();for(Target target:list){if(!target.configuredAliases.trim().isEmpty()&&target.aliases.isEmpty())return "쉼표 사이에 부를 이름을 입력해주세요.";if(target.aliases.size()>8)return "한 장치의 별칭은 8개까지 입력해주세요.";for(String alias:target.aliases){if(compact(alias).length()<2||alias.length()>40)return "별칭은 각각 2~40글자로 입력해주세요.";}for(String name:names(target)){String previous=owners.put(name,target.id);if(previous!=null&&!previous.equals(target.id))return "별칭 또는 불·등·조명으로 바꿔 부르는 이름이 겹칩니다: "+name+". 장치 이름을 더 구체적으로 입력해주세요.";}}return "";}
 public static String describe(Plan plan){switch(plan.service){case "":return "상태 조회";case "press":return "버튼 누르기";case "lock":return "잠금";case "unlock":return "잠금 해제";case "open_cover":return "열기";case "close_cover":return "닫기";case "stop_cover":return "중지";case "turn_on":return "켜기";case "turn_off":return "끄기";default:return "설정 변경";}}
 public static Plan plan(String text,List<Target> targets){
  String s=compact(text);if(s.isEmpty())throw new IllegalArgumentException("명령을 다시 말씀해주세요.");
  if(s.matches(".*(하지마|하지말|켜지마|켜지말|끄지마|끄지말|열지마|열지말|닫지마|닫지말|안켜|안꺼|안열|안닫|잠그지마|잠그지말|잠가주지마|풀지마|풀지말|안잠가|안풀어|취소).*"))throw new IllegalArgumentException("취소했어요.");
  Map<String,Target> matches=new LinkedHashMap<>();int longest=0;for(Target target:targets)for(String name:names(target)){if(s.startsWith(name)){if(name.length()>longest){matches.clear();longest=name.length();}if(name.length()==longest)matches.put(target.id,target);}}
  if(matches.size()!=1)throw new IllegalArgumentException(matches.isEmpty()?"등록한 장치 별칭을 먼저 말씀해주세요.":"별칭이 겹칩니다. 장치 이름을 다르게 설정해주세요.");
  Target t=matches.values().iterator().next();String rest=s.substring(longest);for(Target other:targets)if(!other.id.equals(t.id))for(String name:names(other))if(rest.contains(name))throw new IllegalArgumentException("한 번에 한 장치씩 말씀해주세요.");
  rest=rest.replaceFirst("^(으로|에서|에게|은|는|이|가|을|를)","").replaceFirst("^(좀|한번만|한번)","");String d=t.domain();
  if(!DOMAINS.contains(d))throw new IllegalArgumentException("지원하지 않는 장치 종류입니다.");
  if(rest.matches("(상태|상태알려줘|상태알려주세요|어때|온도|온도알려줘|습도|습도알려줘|확인|확인해줘|몇도야|몇도|켜져있어|꺼져있어|열려있어|닫혀있어)[요까]*"))return new Plan(t,"");
  if(d.equals("button")){
   if(rest.matches("(?:버튼)?(눌러|눌러줘|눌러주세요|누르기|누름|누르세요|실행|실행해|실행해줘|실행해주세요|작동|작동시켜줘)[요]*"))return new Plan(t,"press");
   throw new IllegalArgumentException("버튼은 눌러줘, 실행해줘, 상태 알려줘로 말씀해주세요.");
  }
  if(d.equals("lock")){
   if(rest.matches("(잠금상태|잠금상태알려줘|잠금상태알려주세요|잠겼어|잠겨있어|잠겨있니|잠겼니|잠금해제됐어)[요까]*"))return new Plan(t,"");
   if(rest.matches("(잠가|잠가줘|잠가주세요|잠궈|잠궈줘|잠궈주세요|잠금|잠금해줘|잠금설정|잠금설정해줘|켜|켜줘|켜주세요|전원켜|전원켜줘|닫아|닫아줘|닫아주세요)[요]*"))return new Plan(t,"lock");
   if(rest.matches("(잠금해제|잠금해제해줘|잠금해제해|잠금해제해주세요|잠금풀어|잠금풀어줘|잠금풀어주세요|풀어|풀어줘|풀어주세요|열어|열어줘|열어주세요|꺼|꺼줘|꺼주세요|전원꺼|전원꺼줘)[요]*"))return new Plan(t,"unlock");
   throw new IllegalArgumentException("도어록은 켜·닫아·잠가줘, 꺼·열어·잠금 해제해줘, 상태 알려줘로 말씀해주세요.");
  }
  boolean on=rest.matches("(켜|켜줘|켜주세요|켜라|켜줄래|켜주어|켜죠|작동|작동시켜줘|전원켜|전원켜줘|전원켜주세요)[요]*"),off=rest.matches("(꺼|꺼줘|꺼주세요|꺼라|꺼줄래|꺼주어|꺼죠|전원꺼|전원꺼줘|전원꺼주세요)[요]*");
  if(on||off){if(Arrays.asList("light","switch","input_boolean","fan","climate","media_player").contains(d))return new Plan(t,on?"turn_on":"turn_off");}
  if(d.equals("cover")){if(on)return new Plan(t,"open_cover");if(off)return new Plan(t,"close_cover");if(rest.matches("(열어|열어줘|열어주세요|올려|올려줘)[요]*"))return new Plan(t,"open_cover");if(rest.matches("(닫아|닫아줘|닫아주세요|내려|내려줘)[요]*"))return new Plan(t,"close_cover");if(rest.matches("(멈춰|멈춰줘|정지)[요]*"))return new Plan(t,"stop_cover");}
  Matcher percent=Pattern.compile("(?:밝기|속도|볼륨|음량|위치)?(\\d{1,3})(?:퍼센트|프로|퍼|%)(?:로)?(?:설정|맞춰|맞춰줘|해줘|해|설정해줘|설정해)?(?:요)?").matcher(rest);
  if(percent.matches()){int n=Integer.parseInt(percent.group(1));if(n<0||n>100)throw new IllegalArgumentException("0에서 100퍼센트로 말씀해주세요.");Plan p;
   if(d.equals("light")){p=new Plan(t,"turn_on");p.data.put("brightness_pct",n);}else if(d.equals("fan")){p=new Plan(t,"set_percentage");p.data.put("percentage",n);}else if(d.equals("media_player")){p=new Plan(t,"volume_set");p.data.put("volume_level",n/100.0);}else if(d.equals("cover")){p=new Plan(t,"set_cover_position");p.data.put("position",n);}else throw new IllegalArgumentException("이 장치는 퍼센트 설정을 지원하지 않아요.");return p;}
  Matcher temp=Pattern.compile("(?:온도)?(\\d{1,2})(?:도)(?:로)?(?:설정|맞춰|맞춰줘|해줘|해|설정해줘)?(?:요)?").matcher(rest);
  if(d.equals("climate")&&temp.matches()){int n=Integer.parseInt(temp.group(1));if(n<10||n>35)throw new IllegalArgumentException("온도는 10도에서 35도로 말씀해주세요.");Plan p=new Plan(t,"set_temperature");p.data.put("temperature",n);return p;}
  throw new IllegalArgumentException("별칭 뒤에 켜줘, 꺼줘, 상태 알려줘처럼 말씀해주세요.");
 }
 public static final class Wake {public final boolean matched;public final String remaining;Wake(boolean m,String s){matched=m;remaining=s;}}
 public static Wake wake(String text,String words){return wake(text,words,true);}
 private static int initial(int n){int l=n/588;return l==1?0:l==4?3:l==8?7:l==10?9:l==13?12:l;}
 private static boolean near(String a,String b){if(a.length()!=b.length()||a.length()<3)return false;int vowels=0;for(int i=0;i<a.length();i++){int x=a.charAt(i)-0xac00,y=b.charAt(i)-0xac00;if(x<0||x>=11172||y<0||y>=11172||initial(x)!=initial(y)||x%28!=y%28)return false;if((x%588)/28!=(y%588)/28)vowels++;}return vowels<=1;}
 public static Wake wake(String text,String words,boolean tolerate){String n=compact(text);String best="";for(String word:words.split("[,，\\n]")){String w=compact(word);if(w.length()<2||w.length()<=best.length()||n.length()<w.length())continue;if(n.startsWith(w)||(tolerate&&near(n.substring(0,w.length()),w)))best=w;}return new Wake(!best.isEmpty(),best.isEmpty()?n:n.substring(best.length()));}
}
