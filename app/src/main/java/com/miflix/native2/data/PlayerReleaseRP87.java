package com.miflix.native2.data;

/** Main-thread handoff: a decoder lease is closed exactly once before its replacement. */
public final class PlayerReleaseRP87 {
    public interface Cleanup { void close(boolean replacing); }
    public static final class Lease {
        private final Cleanup cleanup;
        private boolean closed;
        private Lease(Cleanup cleanup) {this.cleanup=cleanup;}
        public boolean isClosed() {return closed;}
        private void close(boolean replacing) {
            if(closed) return;
            closed=true;
            cleanup.close(replacing);
        }
    }
    private Lease active;
    public Lease bind(Cleanup cleanup) {beforeReplace();active=new Lease(cleanup);return active;}
    public void beforeReplace() {beforeReplace(true);}
    public void beforeReplace(boolean replacing) {Lease old=active;active=null;if(old!=null) old.close(replacing);}
    public void dispose(Lease lease) {if(active==lease) active=null;lease.close(false);}
}
