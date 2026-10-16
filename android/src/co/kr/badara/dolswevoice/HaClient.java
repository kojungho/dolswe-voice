package co.kr.badara.dolswevoice;
import org.json.*;import java.net.*;import java.io.*;import java.util.*;
public final class HaClient {
 public interface Transport {String request(String method,String path,JSONObject data)throws Exception;}
 public static final class Entity {public String id,name,state,unit;public JSONObject attributes;Entity(JSONObject o){id=o.optString("entity_id");state=o.optString("state");attributes=o.optJSONObject("attributes");if(attributes==null)attributes=new JSONObject();name=attributes.optString("friendly_name",id);unit=attributes.optString("unit_of_measurement","");}}
 private final Transport transport;public HaClient(Transport t){transport=t;}
 public HaClient(String base,String token){this(new Https(base,token));}
 public Set<String> allowed(String label)throws Exception{
  if(label==null||label.trim().isEmpty())throw new IOException("HA 레이블 이름을 입력해주세요.");
  String template="{% set k=label_id("+JSONObject.quote(label.trim())+") %}{% if k %}{% set ns=namespace(ids=label_entities(k)) %}{% for d in label_devices(k) %}{% set ns.ids=ns.ids+device_entities(d) %}{% endfor %}{{ ns.ids|unique|list|to_json }}{% else %}[]{% endif %}";
  JSONArray a=new JSONArray(transport.request("POST","/api/template",new JSONObject().put("template",template)));Set<String> ids=new HashSet<>();for(int i=0;i<a.length();i++){String id=a.getString(i);if(id.matches("[a-z_]+\\.[a-z0-9_]+")&&CommandRouter.DOMAINS.contains(id.split("\\.")[0]))ids.add(id);}return ids;
 }
 public List<Entity> list(String label)throws Exception{Set<String> ids=allowed(label);List<Entity> out=new ArrayList<>();if(ids.isEmpty())return out;JSONArray a=new JSONArray(transport.request("GET","/api/states",null));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);if(ids.contains(o.optString("entity_id"))){Entity e=new Entity(o);JSONArray members=e.attributes.optJSONArray("entity_id");if(members==null||members.length()==0)out.add(e);}}out.sort(Comparator.comparing(e->e.name));return out;}
 public String execute(String label,CommandRouter.Plan p,boolean dryRun)throws Exception{
  if(!allowed(label).contains(p.target.id))throw new IOException("레이블이 제거되었거나 허용되지 않은 장치입니다. 목록을 새로 불러오세요.");
  Entity e=new Entity(new JSONObject(transport.request("GET","/api/states/"+p.target.id,null)));
  JSONArray members=e.attributes.optJSONArray("entity_id");if(members!=null&&members.length()>0)throw new IOException("그룹 대신 레이블이 붙은 개별 장치를 선택해주세요.");
  if(e.state.equals("unavailable")||e.state.equals("unknown"))throw new IOException("장치에 연결할 수 없어요.");
  if(p.query()){if(p.target.domain().equals("climate")&&e.attributes.has("current_temperature"))return p.target.alias+"의 현재 온도는 "+e.attributes.optString("current_temperature")+"도예요.";String state=e.state;Map<String,String> names=new HashMap<>();names.put("on","켜짐");names.put("off","꺼짐");names.put("open","열림");names.put("closed","닫힘");return p.target.alias+"은 "+names.getOrDefault(state,state)+(e.unit.isEmpty()?"":" "+e.unit)+" 상태예요.";}
  if(dryRun)return "시험 모드: "+p.target.alias+"에 "+p.service+" 명령이 연결됐어요. 실제로 조작하지 않았어요.";
  if(p.data.containsKey("brightness_pct")&&(e.attributes.optInt("supported_color_modes",0)==0&&e.attributes.optJSONArray("supported_color_modes")!=null)){JSONArray modes=e.attributes.optJSONArray("supported_color_modes");if(modes.length()==1&&modes.optString(0).equals("onoff"))throw new IOException("이 조명은 밝기를 조절할 수 없어요.");}
  if(p.data.containsKey("temperature")){double n=((Number)p.data.get("temperature")).doubleValue();if(n<e.attributes.optDouble("min_temp",10)||n>e.attributes.optDouble("max_temp",35))throw new IOException("장치가 지원하는 온도 범위를 벗어났어요.");}
  transport.request("POST","/api/services/"+p.target.domain()+"/"+p.service,new JSONObject(p.data));
  return p.target.alias+"에 명령을 보냈어요.";
 }
 private static final class Https implements Transport {
  final String base,token;Https(String b,String t){base=b.replaceAll("/+$","");token=t;}
  public String request(String method,String path,JSONObject data)throws Exception{
   URL u=new URL(base+path);if(!u.getProtocol().equals("https")||u.getUserInfo()!=null)throw new IOException("HA 주소는 https:// 주소로 입력해주세요.");
   HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestMethod(method);c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");
   try{if(data!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(data.toString().getBytes("UTF-8"));}}
    int status=c.getResponseCode();if(status==401)throw new IOException("HA 연결 토큰을 확인해주세요.");if(status==403)throw new IOException("HA 계정에 해당 장치 또는 레이블을 조회할 권한이 없어요.");if(status<200||status>=300)throw new IOException("HA 요청 실패 ("+status+")");
    ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){b.write(buf,0,n);if(b.size()>8*1024*1024)throw new IOException("응답이 너무 큽니다.");}}return b.toString("UTF-8");
   }finally{c.disconnect();}
  }
 }
}
