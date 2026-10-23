import com.miflix.native2.data.BilingualInputRP85;
import java.util.*;
public final class BilingualRP85Check {
    static int checks;
    static void check(boolean result,String message){checks++;if(!result)throw new AssertionError(message);}
    public static void main(String[] args){
        String keys=BilingualInputRP85.keyboard();
        for(char c='a';c<='z';c++)check(keys.indexOf(c)>=0,"English letter "+c);
        for(char c: "ñáéíóúü".toCharArray())check(keys.indexOf(c)>=0,"Spanish key "+c);
        for(char c: "ñÑáÁéÉíÍóÓúÚüÜ Spider-Man 2: The Movie".toCharArray())check(BilingualInputRP85.accepts(c),"Supported input "+c);
        check(!BilingualInputRP85.accepts(0x4E00),"Unrelated script not keyboard input");
        check(!BilingualInputRP85.accepts(-1),"Invalid codepoint");
        check(!BilingualInputRP85.accepts('\n'),"No multiline injection");
        check(BilingualInputRP85.fallbackLocale("es","en").equals("es-MX"),"Spanish playback fallback");
        check(BilingualInputRP85.fallbackLocale("en","es").equals("en-US"),"English playback fallback");
        check(BilingualInputRP85.fallbackLocale("ko","es").equals("es-MX"),"Other playback language still uses Spanish/English keyboard");
        check(BilingualInputRP85.fallbackLocale(null,"en").equals("en-US"),"English interface fallback");
        check(Arrays.equals(BilingualInputRP85.voiceLanguages(),new String[]{"es-MX","en-US"}),"Only Spanish/English requested");
        System.out.println("PASS: "+checks+" bilingual input checks");
    }
}
