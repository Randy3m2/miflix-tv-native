import com.miflix.native2.data.StreamPresentation;
public final class StreamPresentationCheck {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        StreamPresentation.Info a=StreamPresentation.parse("comet.feels.legal","[TB⚡] Comet 2160p","[TB⚡] Comet 2160p\nHEVC • DV • HDR10+ • 10bit | Atmos • Dolby Digital Plus • 5.1\nWEB-DL\n5.4 GB","",0);
        check(a.heading.equals("[TB⚡] Comet · 2160p"),"clean heading without host or repetition");
        check(a.technologies.toString().equals("[Dolby Vision, HDR10+, Dolby Atmos]"),"metadata technologies");
        check(a.video.equals("HEVC · 10-bit") && a.audio.equals("Dolby Digital Plus · 5.1"),"video and audio separated");
        check(a.source.equals("WEB-DL") && a.size.equals("5.4 GB"),"source and fallback size");
        a=StreamPresentation.parse("torrentio.strem.fun","Torrentio\n4k","","Movie.2026.2160p.BluRay.REMUX.HEVC.TrueHD.Atmos.7.1.mkv",1073741824L*5);
        check(a.heading.equals("Torrentio · 2160p") && a.source.equals("Blu-ray Remux"),"Torrentio filename metadata");
        check(a.technologies.contains("Dolby Atmos") && !a.technologies.contains("Dolby Vision"),"do not infer DV from HEVC, Remux or Atmos");
        check(a.size.equals("5.00 GiB") && a.audio.equals("TrueHD · 7.1"),"byte size preferred and audio");
        a=StreamPresentation.parse("comet.feels.legal","Comet 1080p","","Link.Click.S01E01.1080p.Blu-ray.Remux.AVC.Dual-Audio.FLAC.2.0-Mehul.mkv",0);
        check(a.technologies.isEmpty() && a.video.equals("AVC") && a.audio.equals("FLAC · 2.0"),"no invented premium formats for 1080p FLAC source");
        a=StreamPresentation.parse("Torrentio","Torrentio","HDRip H265 DDP5.1","",0);
        check(a.technologies.isEmpty() && a.audio.equals("Dolby Digital Plus · 5.1"),"HDRip is not HDR, compact DDP5.1 parsed");
        check(StreamPresentation.parse("","","","",0).technologies.isEmpty(),"missing metadata stays unknown");
        check(StreamPresentation.parse("","","HDR10","",0).technologies.toString().equals("[HDR10]"),"HDR10 does not imply HDR10+");
        check(StreamPresentation.parse("","","HDR","",0).technologies.toString().equals("[HDR]"),"generic HDR retained");
        System.out.println("PASS: Comet/Torrentio cleanup, source specs, byte size, HDR/DV/Atmos parsing and no inferred capabilities");
    }
}
