package co.kr.badara.dolswevoice;
/** Exact SSID matching: no nearby scans, substring matches or GPS coordinates. */
public final class WifiPolicy {
 public static String ssid(String value){if(value==null)return "";if(value.length()>=2&&value.startsWith("\"")&&value.endsWith("\""))value=value.substring(1,value.length()-1);return value.equals("<unknown ssid>")?"":value;}
 public static boolean matches(String saved,String current){return saved!=null&&!saved.isEmpty()&&saved.equals(ssid(current));}
}
