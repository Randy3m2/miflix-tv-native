import com.miflix.native2.data.SearchQueryRP81;
public final class SearchQueryRP81Check {
    static void check(boolean value,String message) {if(!value) throw new AssertionError(message);}
    static void query(String input,String expected,String role,String type) {
        SearchQueryRP81 q=SearchQueryRP81.parse(input);
        check(q.query.equals(expected),input+" => "+q.query);
        check(q.role.equals(role),"wrong role: "+input);
        check(q.mediaType.equals(type),"wrong type: "+input);
    }
    public static void main(String[] args) {
        query("Tom Hanks","tom hanks","any","all");
        query("Películas de Tom Hanks","tom hanks","any","movie");
        query("Muéstrame las películas del director Christopher Nolan","christopher nolan","director","movie");
        query("Series con la actriz Penélope Cruz","penelope cruz","actor","series");
        query("Películas dirigidas por Guillermo del Toro","guillermo del toro","director","movie");
        query("Directora Sofía Coppola","sofia coppola","director","all");
        query("El director","el director","any","all");
        query("Home Alone","home alone","any","all");
        query("Mi pobre angelito","mi pobre angelito","any","all");
        query("Show me movies directed by Steven Spielberg","steven spielberg","director","movie");
        query("Find movies with the actor Tom Hanks","tom hanks","actor","movie");
        query("Quiero ver películas con Tom Hanks por favor","tom hanks","actor","movie");
        check(SearchQueryRP81.parse("actor Tom Cruise").explicitPerson,"explicit actor missing");
        check(SearchQueryRP81.phoneticName("Christopher Nolan").equals(SearchQueryRP81.phoneticName("Cristofer Nolan")),"Spanish spoken first name");
        check(!SearchQueryRP81.phoneticName("Tom Hanks").equals(SearchQueryRP81.phoneticName("Tom Hardy")),"unrelated names collapse");
        check(SearchQueryRP81.normalize("PENELOPE / PéNéLoPe").equals("penelope penelope"),"accent normalization");
        check(SearchQueryRP81.voiceLocale("es-MX","en").equals("es-MX"),"Spanish override with English UI");
        check(SearchQueryRP81.voiceLocale(null,"es").equals("es-MX"),"Spanish Latin default");
        check(SearchQueryRP81.voiceLocale(null,"en").equals("en-US"),"English default");
        check(SearchQueryRP81.parse("a".repeat(200)).query.length()==120,"unbounded query");
        check(SearchQueryRP81.parse(null).query.isEmpty(),"null input");
        System.out.println("PASS: ES/EN intent, media/role filters, accent/spoken-name normalization and voice language selection");
    }
}
