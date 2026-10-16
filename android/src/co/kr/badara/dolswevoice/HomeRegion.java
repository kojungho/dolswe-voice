package co.kr.badara.dolswevoice;
/** Conservative boundary: the entire reported accuracy circle must be at home. */
final class HomeRegion {
 static final long MAX_AGE_MS=300000;
 static boolean valid(double lat,double lon,double radius){return Double.isFinite(lat)&&Double.isFinite(lon)&&Double.isFinite(radius)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180&&radius>=50&&radius<=3000;}
 static double distance(double lat,double lon,double homeLat,double homeLon){double a=Math.toRadians(lat),b=Math.toRadians(homeLat),dl=Math.toRadians(homeLat-lat),dn=Math.toRadians(homeLon-lon);double h=Math.sin(dl/2)*Math.sin(dl/2)+Math.cos(a)*Math.cos(b)*Math.sin(dn/2)*Math.sin(dn/2);return 6371000*2*Math.atan2(Math.sqrt(Math.min(1,h)),Math.sqrt(Math.max(0,1-h)));}
 static boolean inside(double homeLat,double homeLon,double radius,double lat,double lon,float accuracy,long ageMs){return valid(homeLat,homeLon,radius)&&Double.isFinite(lat)&&Double.isFinite(lon)&&lat>=-90&&lat<=90&&lon>=-180&&lon<=180&&Float.isFinite(accuracy)&&accuracy>=0&&ageMs>=0&&ageMs<=MAX_AGE_MS&&distance(lat,lon,homeLat,homeLon)+accuracy<=radius;}
}
