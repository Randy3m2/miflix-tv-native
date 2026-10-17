import com.miflix.native2.player.SourceFallbackBudget;

public class SourceFallbackBudgetCheck {
    public static void main(String[] args) {
        // Ordered inputs are [ES1, ES2, ES3, EN1, EN2]; ES1 was already attempted.
        SourceFallbackBudget budget=new SourceFallbackBudget(5);
        for(int i=1;i<5;i++) if(budget.nextIndex()!=i) throw new AssertionError("Repeated/skipped source");
        if(budget.nextIndex()!=-1 || budget.nextIndex()!=-1) throw new AssertionError("Unbounded retries");
        budget=new SourceFallbackBudget(100);
        int count=1;while(budget.nextIndex()!=-1)count++;
        if(count!=5) throw new AssertionError("Budget reset/overflow");
        if(new SourceFallbackBudget(1).nextIndex()!=-1 || new SourceFallbackBudget(0).nextIndex()!=-1) throw new AssertionError("Missing candidates");
        if(!SourceFallbackBudget.sourceFailure(2004) || !SourceFallbackBudget.sourceFailure(3001) || !SourceFallbackBudget.sourceFailure(4001)) throw new AssertionError("Source errors excluded");
        if(SourceFallbackBudget.sourceFailure(5001) || SourceFallbackBudget.sourceFailure(6001) || SourceFallbackBudget.sourceFailure(1000)) throw new AssertionError("Hardware/DRM/unrelated retry");
        System.out.println("PASS: ES1→ES2→ES3→EN1→EN2 ordering, no repeated indices, five total attempts, empty/single source and failure categories.");
    }
}
