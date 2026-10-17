import com.miflix.native2.data.LidomUrlPolicy;
public class LidomUrlPolicyCheck {
    static void expect(boolean v,String text){if(!v)throw new AssertionError(text);}
    public static void main(String[] args){
        String id="abcdefghI_1";
        for(String u:new String[]{"https://youtube.com/watch?v="+id,"https://www.youtube.com/live/"+id,"https://youtu.be/"+id+"?t=20","https://m.youtube.com/shorts/"+id,"https://www.youtube-nocookie.com/embed/"+id})expect(id.equals(LidomUrlPolicy.videoId(u)),u);
        for(String u:new String[]{"https://youtube.com.evil.test/watch?v="+id,"https://evil.test/?v="+id,"javascript:alert(1)","file:///sdcard/video","https://youtube.com/watch?v=short","https://user:pass@youtube.com/watch?v="+id,"https://youtube.com/watch?v="+id+"\n"})expect(LidomUrlPolicy.videoId(u)==null,u);
        expect(LidomUrlPolicy.channelUrl("https://www.youtube.com/@ÁguilasCibaeñasoficial"),"unicode official handle");
        expect("UCabcdefghijklmnopqrstuv".equals(LidomUrlPolicy.channelId("https://youtube.com/channel/UCabcdefghijklmnopqrstuv")),"channel ID");
        expect(LidomUrlPolicy.channelId("https://evil.test/channel/UCabcdefghijklmnopqrstuv")==null,"no foreign RSS host");
        expect(LidomUrlPolicy.httpUrl("http://media.example/stream.m3u8?token=private"),"keep HTTP and signed query");
        for(String u:new String[]{"ftp://example.com/a","https://user:pass@example.com/x","https://bad host/x","https://example.com/a\r\nHeader:x"})expect(!LidomUrlPolicy.httpUrl(u),u);
        expect(LidomUrlPolicy.matchesStation("Digital15 HD","digital 15"),"digit normalization");
        expect(!LidomUrlPolicy.matchesStation("Digital 150","digital 15"),"avoid wrong station");
        expect(LidomUrlPolicy.matchesStation("Telemedios 8 (720p)","telemedios 8"),"exact station alias");
        expect(!LidomUrlPolicy.matchesStation("Telemedios 25","telemedios 8"),"avoid wrong station number");
        System.out.println("PASS: YouTube URL variants, exact host, channel IDs, unicode handle, credential/control injection rejection, signed HTTP URL preservation and station alias isolation.");
    }
}
