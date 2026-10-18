import com.miflix.native2.data.DiscoveryMood;
public final class DiscoveryMoodCheck {
    static void equal(String want,String actual) {if(!want.equals(actual)) throw new AssertionError(want+" != "+actual);}
    public static void main(String[] args) {
        equal("comfort",DiscoveryMood.intent("algo para sentirme bien después de un día pesado"));
        equal("comfort",DiscoveryMood.intent("something to feel good after a hard day"));
        equal("twist",DiscoveryMood.intent("un thriller con final inesperado"));
        equal("twist",DiscoveryMood.intent("a thriller with an unexpected ending"));
        equal("family",DiscoveryMood.intent("algo que mi mamá y yo podamos ver juntas"));
        equal("family",DiscoveryMood.intent("something my mom and I can watch together"));
        equal("quick",DiscoveryMood.intent("quiero ver algo rápido de 30 minutos"));
        equal("marathon",DiscoveryMood.intent("something for a marathon"));
        equal("meaningful",DiscoveryMood.intent("algo con sentido"));
        equal("background",DiscoveryMood.intent("something for background noise"));
        for(String title:new String[]{"Mom","Family Guy","Background","Something Borrowed","Marathon Man","Quick","Meaningful","The Thriller",""}) equal("",DiscoveryMood.intent(title));
        equal("christmas",DiscoveryMood.context(23,12,24,false));
        equal("christmas",DiscoveryMood.context(8,11,20,false));
        equal("halloween",DiscoveryMood.context(10,10,18,true));
        equal("background",DiscoveryMood.context(23,7,10,true));
        equal("background",DiscoveryMood.context(2,7,10,false));
        equal("quick",DiscoveryMood.context(9,7,10,false));
        equal("marathon",DiscoveryMood.context(19,7,10,true));
        equal("comfort",DiscoveryMood.context(19,7,10,false));
        equal("comfort",DiscoveryMood.context(14,7,10,true));
        equal("",DiscoveryMood.intent(null));
        System.out.println("PASS: bilingual mood intents, accents, title protection and time/holiday boundaries");
    }
}
