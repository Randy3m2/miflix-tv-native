package com.miflix.native2.data;

import java.net.*;
import java.util.*;

/** Local interface fallback; no SSID, band or internet connection is required. */
public final class LocalQrNetworkRP129 {
    private LocalQrNetworkRP129() {}
    public static final class Endpoint {
        public final Inet4Address address;
        public final int prefixLength;
        public Endpoint(Inet4Address address,int prefixLength) {this.address=address;this.prefixLength=prefixLength;}
    }
    public static boolean acceptsPeer(byte[] ip) {
        return ip!=null && ip.length==4 && ((ip[0]&255)==10 || (ip[0]&255)==172 && (ip[1]&255)>=16 && (ip[1]&255)<=31 || (ip[0]&255)==192 && (ip[1]&255)==168);
    }
    static boolean eligible(String name,boolean up,boolean loopback,boolean virtual,byte[] address,int prefix) {
        if(!up||loopback||virtual||prefix<8||prefix>30||!acceptsPeer(address)) return false;
        String n=name.toLowerCase(Locale.ROOT);
        return !(n.startsWith("tun")||n.startsWith("tap")||n.startsWith("ppp")||n.startsWith("wg")||n.startsWith("vpn")||n.startsWith("ipsec")||n.startsWith("utun"));
    }
    public static List<Endpoint> find() {
        List<Endpoint> result=new ArrayList<>();
        try {
            List<NetworkInterface> interfaces=Collections.list(NetworkInterface.getNetworkInterfaces());
            interfaces.sort(Comparator.comparingInt(n->n.getName().startsWith("eth")?0:n.getName().startsWith("wlan")?1:2));
            for(NetworkInterface network:interfaces) try {
                for(InterfaceAddress link:network.getInterfaceAddresses()) {
                    InetAddress address=link.getAddress();int prefix=link.getNetworkPrefixLength();
                    if(address instanceof Inet4Address && eligible(network.getName(),network.isUp(),network.isLoopback(),network.isVirtual(),address.getAddress(),prefix))
                        result.add(new Endpoint((Inet4Address)address,prefix));
                }
            } catch(SocketException ignored) { }
        } catch(Exception ignored) { }
        return result;
    }
}
