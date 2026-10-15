package com.miflix.native2.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/** Pairing/authentication state is kept in memory and discarded on background/network changes. */
public final class LocalCastAccess {
    public interface Clock {long now();}
    private final Clock clock;
    private final String code;
    private final String linkTicket;
    private boolean revoked;
    public final long pairUntil;
    private final Map<String,Session> sessions=new LinkedHashMap<>();
    private final Map<String,long[]> attempts=new LinkedHashMap<>();
    private final Map<String,Long> received=new LinkedHashMap<>();
    private static final class Session {final String origin;final long until;Session(String o,long now){origin=o;until=now+1800000L;}}
    public LocalCastAccess(String code,Clock clock){this.code=code;this.clock=clock;pairUntil=clock.now()+120000L;linkTicket=randomToken();}
    private static String randomToken(){byte[] random=new byte[24];new SecureRandom().nextBytes(random);StringBuilder token=new StringBuilder();for(byte b:random)token.append(String.format(Locale.US,"%02x",b&255));return token.toString();}
    public String getLinkTicket(){return linkTicket;}
    public synchronized boolean validLink(String ticket){return !revoked && clock.now()<pairUntil && MessageDigest.isEqual(linkTicket.getBytes(StandardCharsets.UTF_8),ticket.getBytes(StandardCharsets.UTF_8));}
    public synchronized String connectLink(String ticket,String origin,String ip){return validLink(ticket)?connect(code,origin,ip):null;}
    public synchronized String connect(String given,String origin,String ip){
        long now=clock.now();long[] a=attempts.get(ip);
        if(a==null||now-a[0]>=60000L){a=new long[]{now,0};if(attempts.size()>=64)attempts.clear();attempts.put(ip,a);}
        if(revoked || ++a[1]>5 || now>=pairUntil || !MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8),given.getBytes(StandardCharsets.UTF_8)))return null;
        for(Iterator<Map.Entry<String,Session>> it=sessions.entrySet().iterator();it.hasNext();)if(it.next().getValue().until<=now)it.remove();
        if(sessions.size()>=8)return null;
        byte[] random=new byte[24];new SecureRandom().nextBytes(random);StringBuilder token=new StringBuilder();for(byte b:random)token.append(String.format(Locale.US,"%02x",b&255));
        sessions.put(token.toString(),new Session(origin,now));return token.toString();
    }
    public synchronized boolean authenticated(String token,String origin){Session s=sessions.get(token);return s!=null && s.until>clock.now() && s.origin.equals(origin);}
    public synchronized boolean duplicate(String token,String request){
        long now=clock.now();for(Iterator<Map.Entry<String,Long>> it=received.entrySet().iterator();it.hasNext();)if(now-it.next().getValue()>90000L)it.remove();
        String key=token+":"+request;if(received.containsKey(key))return true;if(received.size()>=256)received.clear();received.put(key,now);return false;
    }
    public synchronized void clear(){revoked=true;sessions.clear();received.clear();attempts.clear();}
}
