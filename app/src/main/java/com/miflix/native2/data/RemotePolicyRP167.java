package com.miflix.native2.data;
import java.net.URI;
import java.util.Set;
/** Shared allowlist for the authenticated local remote. */
public final class RemotePolicyRP167 {
 private RemotePolicyRP167() {}
 private static final Set<String> ACTIONS=Set.of("up","down","left","right","ok","back","home","play_pause","volume_up","volume_down","mute","sources","subtitles","subtitles_toggle","text","type","seek_back","seek_forward");
 public static boolean valid(String action,String value) {return action!=null && ACTIONS.contains(action) && value!=null && value.length()<=512 && (!(action.equals("text") || action.equals("type")) || !value.isBlank());}
 public static String endpoint(String raw) {
  String value=raw.trim();if(!value.startsWith("http://"))value="http://"+value;
  try {URI u=new URI(value);String h=u.getHost();if(h==null || !h.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}") || u.getUserInfo()!=null || u.getFragment()!=null || u.getQuery()!=null || !"http".equals(u.getScheme()) || (u.getPort()!=-1 && u.getPort()!=8765) || !(u.getPath().isEmpty() || u.getPath().equals("/")))throw new IllegalArgumentException();
   String[] parts=h.split("\\.");byte[] b=new byte[4];for(int i=0;i<4;i++){int n=Integer.parseInt(parts[i]);if(n>255)throw new IllegalArgumentException();b[i]=(byte)n;}if(!LocalCastServer.isPrivate(b))throw new IllegalArgumentException();return "http://"+h+":8765";
  }catch(Exception e){throw new IllegalArgumentException("Enter the TV's private IPv4 address (for example 192.168.1.20)");}
 }
}
