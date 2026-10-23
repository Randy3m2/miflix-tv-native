import com.miflix.native2.data.SportsPolicyRP71;
import com.miflix.native2.data.SportsQualityRP84;
import java.util.*;
public final class SportsRP84Check {
    private static int count;
    private static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        check(SportsPolicyRP71.validManifest(SportsPolicyRP71.DEFAULT),"new manifest valid");
        check(SportsPolicyRP71.configured(null).equals(SportsPolicyRP71.DEFAULT),"new installs");
        check(SportsPolicyRP71.configured(SportsPolicyRP71.PREVIOUS_DEFAULT).equals(SportsPolicyRP71.DEFAULT),"old premium factory migration");
        check(SportsPolicyRP71.configured(SportsPolicyRP71.INTERIM_DEFAULT).equals(SportsPolicyRP71.DEFAULT),"previous suggested variant migration");
        String custom="https://custom.example/abc/manifest.json";
        check(SportsPolicyRP71.configured(custom).equals(custom),"custom source retained");
        check(SportsPolicyRP71.ordersSports(SportsPolicyRP71.DEFAULT),"sports ordering enabled");
        check(!SportsPolicyRP71.ordersSports("https://highfly.to.evil.example/manifest.json"),"lookalike host excluded");
        check(!SportsPolicyRP71.ordersSports("https://nauta.example/manifest.json"),"Nauta ordering unaffected");
        check(SportsQualityRP84.resolution(0,0,"ESPN · 1080p60") ==1080,"1080p60 labels");
        check(SportsQualityRP84.resolution(0,0,"1080i50") ==1080,"interlaced labels");
        check(SportsQualityRP84.resolution(0,0,"4K UHD") ==2160,"4K UHD");
        check(SportsQualityRP84.resolution(0,0,"FHD") ==1080,"FHD");
        check(SportsQualityRP84.resolution(0,0,"FULL HD") ==1080,"Full HD");
        check(SportsQualityRP84.resolution(0,0,"HD") ==720,"HD");
        check(SportsQualityRP84.resolution(0,0,"SD") ==480,"SD");
        check(SportsQualityRP84.resolution(0,0,"1920x1080") ==1080,"dimensions");
        check(SportsQualityRP84.resolution(1920,720,"4K") ==720,"explicit height wins over name");
        check(SportsQualityRP84.resolution(1280,0,"") ==720,"width-only metadata");
        check(SportsQualityRP84.resolution(0,0,"Server 2026") ==0,"unrelated numbers not resolution");
        check(SportsQualityRP84.bitrate("8.5 Mbps") ==8500000L,"Mbps");
        check(SportsQualityRP84.bitrate("8,5 Mbit/s") ==8500000L,"decimal comma");
        check(SportsQualityRP84.bitrate("9500 kbps") ==9500000L,"kbps");
        check(SportsQualityRP84.bitrate("9000000 bps") ==9000000L,"bps");
        check(SportsQualityRP84.bitrate("1.2 GB file") ==0L,"file size is not bitrate");
        check(SportsQualityRP84.bitrate("2026") ==0L,"unlabelled number not bitrate");
        check(SportsQualityRP84.explicitBitrate("8000000") ==8000000L,"numeric bitrate field");
        check(SportsQualityRP84.explicitBitrate("NaN") ==0L,"invalid bitrate rejected");
        check(SportsQualityRP84.compare(2160,4000000,1080,9000000)<0,"resolution first");
        check(SportsQualityRP84.compare(1080,9000000,1080,4000000)<0,"bitrate breaks resolution ties");
        check(SportsQualityRP84.compare(0,9000000,0,4000000)<0,"bitrate-only ordering");
        check(SportsQualityRP84.compare(0,0,0,0)==0,"unknown metadata retains provider order");
        check(SportsPolicyRP71.nextAttempt(3,20)==4 && SportsPolicyRP71.nextAttempt(4,20)==-1,"five source retries retained");
        List<int[]> ranks=new ArrayList<>();
        ranks.add(new int[]{1080,4,0});ranks.add(new int[]{720,9,1});ranks.add(new int[]{2160,3,2});ranks.add(new int[]{1080,8,3});ranks.add(new int[]{0,0,4});ranks.add(new int[]{0,0,5});
        ranks.sort((a,b)->{int q=SportsQualityRP84.compare(a[0],a[1],b[0],b[1]);return q==0?Integer.compare(a[2],b[2]):q;});
        int[] expected={2,3,0,1,4,5};
        for(int i=0;i<expected.length;i++)check(ranks.get(i)[2]==expected[i],"sorted event sources at "+i);
        System.out.println("PASS: "+count+" sports source and quality checks");
    }
}
