package uk.ac.cam.cl.dc584.ntpclient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws Exception {
        try {
            InetAddress addr = InetAddress.getByName("ntp0.cl.cam.ac.uk");
            addr = InetAddress.getByName("128.232.103.138");
            long offset = NTPClient.getTime(addr);
            System.out.println(offset);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host");
        }

    }
}
