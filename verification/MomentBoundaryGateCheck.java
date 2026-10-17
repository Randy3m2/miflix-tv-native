import com.miflix.native2.player.MomentBoundaryGate;

public final class MomentBoundaryGateCheck {
    public static void main(String[] args) {
        MomentBoundaryGate gate=new MomentBoundaryGate(20000);
        if(!gate.claimEnd()) throw new AssertionError("First completion missing");
        gate.movedTo(20000);
        if(gate.claimEnd()) throw new AssertionError("End seek recursively completes");
        gate.movedTo(25000);
        if(gate.claimEnd()) throw new AssertionError("Forward seek duplicates completion");
        gate.movedTo(10000);
        if(!gate.claimEnd() || gate.claimEnd()) throw new AssertionError("Replay not rearmed exactly once");
        boolean invalid=false;try {new MomentBoundaryGate(0);}catch(IllegalArgumentException e){invalid=true;}
        if(!invalid) throw new AssertionError("Invalid end accepted");
        System.out.println("PASS: one end delivery, recursive/forward-seek guard, replay rearming and invalid range.");
    }
}
