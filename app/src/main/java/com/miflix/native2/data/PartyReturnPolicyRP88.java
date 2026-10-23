package com.miflix.native2.data;
/** Playback control events are host-only, recipient-specific, and short lived. */
public final class PartyReturnPolicyRP88 {
    private PartyReturnPolicyRP88() {}
    public static boolean accepts(String author,String host,String body,String recipient,long created,long now) {
        return author!=null && !author.isEmpty() && author.equals(host) && recipient!=null && !recipient.isEmpty()
            && !recipient.equals(host) && ("return:"+recipient).equals(body)
            && created>0 && created<=now+5000L && now-created<=30000L;
    }
}
