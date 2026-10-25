package com.miflix.native2.data;
import org.junit.Test;
import static org.junit.Assert.*;
import java.net.InetAddress;

public class HomeSettingsNetworkRP129Test {
    @Test public void reservesContinueBeforeAsyncResultsArrive() {
        assertTrue(HomeStartupPolicyRP129.reserveContinue(false,false,true));
        assertTrue(HomeStartupPolicyRP129.reserveContinue(true,true,true));
    }
    @Test public void emptyCompletedHistoryDoesNotCreateFakeContent() {
        assertFalse(HomeStartupPolicyRP129.reserveContinue(true,false,true));
        assertFalse(HomeStartupPolicyRP129.reserveContinue(false,false,false));
        assertTrue(HomeStartupPolicyRP129.reserveContinue(true,true,false));
    }
    @Test public void revealsClippedHeadingForEarlyControl() {
        assertTrue(HomeStartupPolicyRP129.revealHeader(-70,25,0,12,160));
    }
    @Test public void doesNotReanchorVisibleCardOrDeepControls() {
        assertFalse(HomeStartupPolicyRP129.revealHeader(12,70,0,12,160));
        assertFalse(HomeStartupPolicyRP129.revealHeader(-300,30,0,12,160));
    }
    @Test public void ethernetAndWifiUseSamePrivateNetworkRule() throws Exception {
        byte[] ip=InetAddress.getByName("192.168.2.15").getAddress();
        assertTrue(LocalQrNetworkRP129.eligible("eth0",true,false,false,ip,24));
        assertTrue(LocalQrNetworkRP129.eligible("wlan0",true,false,false,ip,24));
    }
    @Test public void routesAcrossPrivateSubnetsAreAccepted() throws Exception {
        assertTrue(LocalQrNetworkRP129.acceptsPeer(InetAddress.getByName("192.168.1.3").getAddress()));
        assertTrue(LocalQrNetworkRP129.acceptsPeer(InetAddress.getByName("192.168.50.3").getAddress()));
        assertTrue(LocalQrNetworkRP129.acceptsPeer(InetAddress.getByName("10.20.30.4").getAddress()));
        assertFalse(LocalQrNetworkRP129.acceptsPeer(InetAddress.getByName("8.8.8.8").getAddress()));
    }
    @Test public void ignoresVpnLoopbackAndDownAdapters() throws Exception {
        byte[] ip=InetAddress.getByName("10.0.0.2").getAddress();
        assertFalse(LocalQrNetworkRP129.eligible("tun0",true,false,false,ip,24));
        assertFalse(LocalQrNetworkRP129.eligible("eth0",false,false,false,ip,24));
        assertFalse(LocalQrNetworkRP129.eligible("lo",true,true,false,ip,24));
        assertFalse(LocalQrNetworkRP129.eligible("veth0",true,false,true,ip,24));
    }
}
