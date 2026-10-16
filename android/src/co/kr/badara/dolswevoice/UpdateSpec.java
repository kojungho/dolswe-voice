package co.kr.badara.dolswevoice;
import org.json.*;
final class UpdateSpec {
 static final String REPOSITORY="https://github.com/kojungho/dolswe-voice";
 final long code,size;final String version,url,sha;
 UpdateSpec(String json)throws Exception{JSONObject o=new JSONObject(json);code=o.getLong("versionCode");size=o.getLong("size");version=o.getString("versionName");url=o.getString("apkUrl");sha=o.getString("sha256").toLowerCase(java.util.Locale.ROOT);if(code<1||size<1024||size>536870912||!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||!sha.matches("[0-9a-f]{64}")||!url.equals(REPOSITORY+"/releases/download/v"+version+"/dolswe-voice.apk"))throw new IllegalArgumentException("업데이트 정보가 올바르지 않아요.");}
}
