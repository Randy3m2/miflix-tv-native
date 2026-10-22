import com.miflix.native2.data.SportsPolicyRP71;
import com.miflix.native2.data.SportsManifestServerRP71;
public class SportsRP71Check {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(SportsPolicyRP71.configured(null).equals(SportsPolicyRP71.DEFAULT));
  check(SportsPolicyRP71.configured("https://sportsfree-us2.highfly.to/manifest.json").equals(SportsPolicyRP71.DEFAULT));
  String custom="https://custom.example/token/manifest.json";check(SportsPolicyRP71.configured(custom).equals(custom));
  check(SportsPolicyRP71.validManifest(custom));check(!SportsPolicyRP71.validManifest("http://custom.example/manifest.json"));check(!SportsPolicyRP71.validManifest("https://user:pass@custom.example/manifest.json"));check(!SportsPolicyRP71.validManifest("https://custom.example:444/manifest.json"));check(!SportsPolicyRP71.validManifest("https://custom.example/manifest.json#x"));
  int count=1,index=0;while((index=SportsPolicyRP71.nextAttempt(index,10))>=0)count++;check(count==5);
  check(SportsPolicyRP71.nextAttempt(0,1)==-1);check(SportsPolicyRP71.nextAttempt(-1,8)==-1);
  check(SportsManifestServerRP71.acceptsForm("http://192.168.1.2:9000","http://192.168.1.2:9000","token","token"));
  check(!SportsManifestServerRP71.acceptsForm("http://192.168.1.2:9000","https://external.example","token","token"));
  check(!SportsManifestServerRP71.acceptsForm("http://192.168.1.2:9000",null,"token","wrong"));
  System.out.println("PASS: premium default, legacy migration, custom settings preserved, HTTPS validation, five-attempt budget, QR origin/CSRF guards");
 }
}
