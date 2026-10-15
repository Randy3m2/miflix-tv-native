import com.miflix.native2.data.UniquePageBuffer;
import java.util.ArrayList;
import java.util.List;

public final class GenrePageBufferCheck {
    static void check(boolean ok,String message) {if(!ok) throw new AssertionError(message);}
    public static void main(String[] args) {
        UniquePageBuffer<String> buffer=new UniquePageBuffer<>();
        List<String> page=new ArrayList<>();for(int i=0;i<40;i++) page.add("movie:"+i);
        buffer.offer(page,x->x);check(buffer.take(25).size()==25 && buffer.pendingCount()==15,"Keep first-page overflow");
        page.clear();for(int i=30;i<70;i++) page.add("movie:"+i);
        buffer.offer(page,x->x);check(buffer.pendingCount()==45,"Deduplicate across provider pages");
        check(buffer.take(25).size()==25 && buffer.pendingCount()==20,"Second exact batch");
        buffer.offer(List.of("movie:0","movie:69","series:0"),x->x);
        check(buffer.take(25).size()==21 && buffer.pendingCount()==0,"Final partial batch; movie and series IDs remain distinct");
        buffer.clear();buffer.offer(List.of("movie:0"),x->x);check(buffer.take(25).equals(List.of("movie:0")),"Genre reset resets dedupe");
        System.out.println("PASS: 25-result batches, retained overflow, duplicate pages, distinct media IDs, exhaustion and genre reset");
    }
}
