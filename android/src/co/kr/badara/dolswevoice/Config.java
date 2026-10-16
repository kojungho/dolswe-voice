package co.kr.badara.dolswevoice;
import android.content.*;import android.security.keystore.*;import android.util.Base64;import javax.crypto.*;import javax.crypto.spec.GCMParameterSpec;import java.security.KeyStore;import org.json.*;import java.util.*;
final class Config {
 final Context ctx;final android.content.SharedPreferences p;
 Config(Context c){ctx=c.getApplicationContext();p=ctx.getSharedPreferences("voice",Context.MODE_PRIVATE);}
 String base(){return p.getString("base","");}String label(){return p.getString("label","");}String wakes(){return p.getString("wakes","돌쇠야, 이모님");}boolean dry(){return p.getBoolean("dry",true);}
 private javax.crypto.SecretKey key()throws Exception{KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);if(!s.containsAlias("voice_token")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("voice_token",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}return (javax.crypto.SecretKey)s.getKey("voice_token",null);}
 String token()throws Exception{String encrypted=p.getString("token","");if(encrypted.isEmpty())return "";String[] v=encrypted.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(v[0],Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(v[1],Base64.NO_WRAP)),"UTF-8");}
 void save(String base,String label,String wakes,String token,boolean dry)throws Exception{
  java.net.URI u=new java.net.URI(base.trim());if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new Exception("HA 주소는 https://로 시작하는 서버 주소를 입력해주세요.");
  if(label.trim().isEmpty())throw new Exception("HA에서 지정한 레이블 이름을 입력해주세요.");List<String> words=new ArrayList<>();for(String w:wakes.split("[,，\\n]")){w=w.trim();if(w.isEmpty())continue;if(CommandRouter.compact(w).length()<2||w.length()>30)throw new Exception("호출어는 2~30글자로 입력해주세요.");words.add(w);}if(words.isEmpty()||words.size()>10)throw new Exception("호출어를 1~10개 입력해주세요.");
  android.content.SharedPreferences.Editor e=p.edit().putString("base",base.trim().replaceAll("/+$","")).putString("label",label.trim()).putString("wakes",String.join(", ",words)).putBoolean("dry",dry);
  if(!base.trim().replaceAll("/+$", "").equals(base())||!label.trim().equals(label()))e.remove("entities");
  if(!token.trim().isEmpty()){Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());e.putString("token",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(token.trim().getBytes("UTF-8")),Base64.NO_WRAP));}
  if(token.trim().isEmpty()&&token().isEmpty())throw new Exception("HA 장기 액세스 토큰을 입력해주세요.");e.commit();
 }
 JSONArray cache(){try{return new JSONArray(p.getString("entities","[]"));}catch(Exception e){return new JSONArray();}}
 List<CommandRouter.Target> targets(){List<CommandRouter.Target> list=new ArrayList<>();try{JSONObject a=new JSONObject(p.getString("aliases","{}"));for(Iterator<String> it=a.keys();it.hasNext();){String id=it.next();String name=a.optString(id).trim();if(!name.isEmpty())list.add(new CommandRouter.Target(id,name));}}catch(Exception ignored){}return list;}
 String alias(String id){try{return new JSONObject(p.getString("aliases","{}")).optString(id,"");}catch(Exception e){return "";}}
 void saveAliases(JSONObject a)throws Exception{List<CommandRouter.Target> l=new ArrayList<>();for(Iterator<String> it=a.keys();it.hasNext();){String id=it.next();l.add(new CommandRouter.Target(id,a.optString(id)));}String error=CommandRouter.validateAliases(l);if(!error.isEmpty())throw new Exception(error);p.edit().putString("aliases",a.toString()).commit();}
 HaClient client()throws Exception{String t=token();if(t.isEmpty())throw new Exception("설정에서 HA 연결을 먼저 저장해주세요.");return new HaClient(base(),t);}
 void cache(List<HaClient.Entity> entities)throws Exception{JSONArray a=new JSONArray();for(HaClient.Entity e:entities)a.put(new JSONObject().put("id",e.id).put("name",e.name).put("state",e.state));p.edit().putString("entities",a.toString()).commit();}
 void last(String text){p.edit().putString("last",text).apply();}String last(){return p.getString("last","호출 대기를 시작하거나 마이크 버튼을 눌러주세요.");}
}
