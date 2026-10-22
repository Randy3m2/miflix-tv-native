import com.miflix.native2.data.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.*;
public class SportsQrRP71Check {
 public static void main(String[] args)throws Exception{
  Inet4Address lan=null;int prefix=24;
  for(NetworkInterface network:Collections.list(NetworkInterface.getNetworkInterfaces()))for(InterfaceAddress entry:network.getInterfaceAddresses())if(entry.getAddress() instanceof Inet4Address && LocalCastServer.isPrivate(entry.getAddress().getAddress())){lan=(Inet4Address)entry.getAddress();prefix=entry.getNetworkPrefixLength();break;}
  if(lan==null){System.out.println("SKIP: QR socket integration needs a private IPv4 LAN interface");return;}
  AtomicReference<String> saved=new AtomicReference<>();
  try(SportsManifestServerRP71 server=new SportsManifestServerRP71(lan,prefix,"Sports",SportsPolicyRP71.DEFAULT,(name,url)->{saved.set(url);return true;})){
   HttpClient client=HttpClient.newHttpClient();
   HttpResponse<String> form=client.send(HttpRequest.newBuilder(URI.create(server.getUrl())).GET().build(),HttpResponse.BodyHandlers.ofString());
   if(form.statusCode()!=200||!form.body().contains("Manifest HTTPS")||form.body().contains("Optional name"))throw new AssertionError("Sports form");
   Matcher match=Pattern.compile("name='csrf' value='([^']+)'").matcher(form.body());if(!match.find())throw new AssertionError("CSRF absent");
   String csrf=match.group(1),target="https://custom.example/manifest.json",body="name=Sports&csrf="+csrf+"&url="+URLEncoder.encode(target,"UTF-8");
   String saveUrl=server.getUrl().replace("/add/","/save/");
   HttpResponse<String> result=client.send(HttpRequest.newBuilder(URI.create(saveUrl)).header("Content-Type","application/x-www-form-urlencoded").header("Origin",server.getBase()).POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
   if(result.statusCode()!=200||!target.equals(saved.get()))throw new AssertionError("Save failed: "+result.statusCode());
   HttpResponse<String> rejected=client.send(HttpRequest.newBuilder(URI.create(saveUrl)).header("Content-Type","application/x-www-form-urlencoded").header("Origin","https://external.example").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
   if(rejected.statusCode()!=403)throw new AssertionError("Cross-origin form accepted");
  }
  System.out.println("PASS: LAN QR form, sports-only fields, saved callback, foreign origin rejected, clean server shutdown");
 }
}
