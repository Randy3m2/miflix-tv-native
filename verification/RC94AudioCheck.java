import com.miflix.native2.data.AudioSourcePolicy;
public class RC94AudioCheck {
 static void equal(int expected,int actual){if(expected!=actual)throw new AssertionError(expected+" != "+actual);}
 public static void main(String[] args) {
  String[] decoder={"audio/mp4a-latm","audio/eac3","audio/flac"};
  equal(0,AudioSourcePolicy.rank("AAC 2.0",decoder));
  equal(0,AudioSourcePolicy.rank("Dolby Digital Plus 5.1",decoder));
  equal(2,AudioSourcePolicy.rank("TrueHD 7.1",decoder));
  equal(0,AudioSourcePolicy.rank("DTS-HD MA / AAC",decoder));
  equal(2,AudioSourcePolicy.rank("DTS-HD MA",new String[]{"audio/vnd.dts"}));
  equal(1,AudioSourcePolicy.rank("Atmos 5.1",decoder));
  equal(1,AudioSourcePolicy.rank("Unknown",decoder));
  System.out.println("PASS: known audio compatibility, dual-audio alternatives, unknown labels and DTS-HD/core distinction");
 }
}
