import com.miflix.native2.data.RandomEpisodePolicy;
public final class RandomEpisodeCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  String today="2026-10-15";
  check(RandomEpisodePolicy.aired("2026-10-15",today),"release day included");
  check(!RandomEpisodePolicy.aired("2026-10-16",today),"future episode excluded");
  check(!RandomEpisodePolicy.aired("",today)&&!RandomEpisodePolicy.aired(null,today),"unknown air dates excluded");
  check(!RandomEpisodePolicy.season(0,10,"2020-01-01",today),"specials excluded");
  check(!RandomEpisodePolicy.season(1,0,"2020-01-01",today),"empty season excluded");
  int[] counts={3,0,7};int[] selected={0,0,0};for(int draw=0;draw<10;draw++)selected[RandomEpisodePolicy.indexForDraw(counts,draw)]++;
  check(selected[0]==3 && selected[1]==0 && selected[2]==7,"weighted season selection follows episode counts");
  try{RandomEpisodePolicy.indexForDraw(counts,10);throw new AssertionError("out of range accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: release dates, unknown/future exclusion, specials, empty seasons and weighted selection");
 }
}
