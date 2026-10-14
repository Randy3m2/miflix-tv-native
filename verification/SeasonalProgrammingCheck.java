import com.miflix.native2.data.SeasonalProgramming;
import java.time.LocalDate;

public final class SeasonalProgrammingCheck {
    private static void check(int month,int day,String expected) {
        String actual=SeasonalProgramming.theme(month,day);
        if(!expected.equals(actual)) throw new AssertionError(month+"/"+day+": "+actual);
    }
    public static void main(String[] args) {
        check(9,30,"discovery");check(10,1,"halloween");check(10,31,"halloween");
        check(11,1,"discovery");check(11,19,"discovery");check(11,20,"christmas");
        check(12,31,"christmas");check(1,1,"christmas");check(1,6,"christmas");check(1,7,"discovery");
        check(2,14,"romance");check(2,29,"romance");check(3,1,"discovery");
        check(5,31,"discovery");check(6,1,"adventure");check(8,31,"adventure");check(9,1,"discovery");
        int checked=0;
        for(LocalDate d=LocalDate.of(2024,1,1);d.isBefore(LocalDate.of(2028,1,1));d=d.plusDays(1)) {
            String t=SeasonalProgramming.theme(d.getMonthValue(),d.getDayOfMonth());
            if(SeasonalProgramming.genre(t)<=0) throw new AssertionError("Missing fallback genre");
            checked++;
        }
        try {SeasonalProgramming.theme(0,1);throw new AssertionError("Invalid month accepted");}
        catch(IllegalArgumentException expected) {}
        System.out.println("PASS: season boundaries, leap day, New Year and "+checked+" calendar dates");
    }
}
