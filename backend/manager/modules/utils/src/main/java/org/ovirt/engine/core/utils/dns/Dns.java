package org.ovirt.engine.core.utils.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ovirt.engine.core.utils.IPAddress;

public class Dns {

    public static IPAddress[] GetHostAddresses(String hostName) {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            byte[] ipAddr = addr.getAddress();
            IPAddress[] ipAddresses = new IPAddress[1];
            ipAddresses[0] = new IPAddress(ipAddr);
            return ipAddresses;
        } catch (UnknownHostException ex) {
            RuntimeException newEx = new RuntimeException(ex.getMessage());
            newEx.setStackTrace(ex.getStackTrace());
            throw newEx;
        }
    }
}
