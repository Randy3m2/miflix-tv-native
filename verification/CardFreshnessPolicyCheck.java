import com.miflix.native2.data.CardFreshnessPolicy;
import java.time.Instant;
public class CardFreshnessPolicyCheck {
    static void check(boolean value) {if(!value) throw new AssertionError();}
    public static void main(String[] args) {
        long now=Instant.parse("2026-10-17T19:00:00Z").toEpochMilli();
        check(!CardFreshnessPolicy.justAdded(0,now));
        check(CardFreshnessPolicy.justAdded(now,now));
        check(CardFreshnessPolicy.justAdded(now-13L*86400000,now));
        check(!CardFreshnessPolicy.justAdded(now-14L*86400000,now));
        check(!CardFreshnessPolicy.justAdded(now+1,now));
        check(CardFreshnessPolicy.newEpisode("2026-10-17",now));
        check(CardFreshnessPolicy.newEpisode("2026-10-11",now));
        check(CardFreshnessPolicy.newEpisode("2026-10-10",now));
        check(CardFreshnessPolicy.newEpisode("2026-10-04",now));
        check(!CardFreshnessPolicy.newEpisode("2026-10-03",now));
        check(CardFreshnessPolicy.recentRelease("2026-10-04",now));
        check(!CardFreshnessPolicy.recentRelease("2026-10-03",now));
        check(!CardFreshnessPolicy.recentRelease("2026-10-18",now));
        check(!CardFreshnessPolicy.newEpisode("2026-10-18",now));
        check(!CardFreshnessPolicy.newEpisode("2026-02-30",now));
        check(!CardFreshnessPolicy.newEpisode(null,now));
        check(!CardFreshnessPolicy.newEpisode("",now));
        long earlyUtc=Instant.parse("2026-10-18T02:00:00Z").toEpochMilli();
        check(!CardFreshnessPolicy.newEpisode("2026-10-18",earlyUtc));
        check(CardFreshnessPolicy.newEpisode("2026-10-17",earlyUtc));
        System.out.println("PASS: 14-day release/episode windows, future/invalid/missing dates and Dominican midnight.");
    }
}
