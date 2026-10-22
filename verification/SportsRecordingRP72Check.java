import com.miflix.native2.data.SportsRecordingPolicyRP72;
public class SportsRecordingRP72Check {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(SportsRecordingPolicyRP72.recording("Sport Recaps","Basketball"));
  check(SportsRecordingPolicyRP72.recording("sports_recaps"));
  check(SportsRecordingPolicyRP72.recording("SportRecaps"));
  check(SportsRecordingPolicyRP72.recording("MLB Replays","Yesterday"));
  check(SportsRecordingPolicyRP72.recording("Repetición del partido"));
  check(SportsRecordingPolicyRP72.recording("Grabaciones","NBA"));
  check(!SportsRecordingPolicyRP72.recording("Live Now","NBA Finals","live.ts"));
  check(!SportsRecordingPolicyRP72.recording("Others","ESPN HD"));
  check(!SportsRecordingPolicyRP72.recording((String)null));
  System.out.println("PASS: recording metadata in EN/ES/camelCase, live events excluded, file extensions do not imply VOD");
 }
}
