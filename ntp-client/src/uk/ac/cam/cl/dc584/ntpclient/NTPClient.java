package uk.ac.cam.cl.dc584.ntpclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NTPClient {
    public static long getTime(InetAddress host) throws IOException {
        NTPPacket message = new NTPPacket();

        message.setMode(3);
        message.setVersion(3);
        DatagramPacket sendPacket = message.getDatagramPacket();
        sendPacket.setAddress(host);

        NTPPacket recMessage = new NTPPacket();
        DatagramPacket recPacket = recMessage.getDatagramPacket();

        TimeStamp now = TimeStamp.getCurrentTime();
        message.setTransmitTime(now);

        DatagramSocket socket = new DatagramSocket(123, host);

        socket.send(sendPacket);
        socket.receive(recPacket);

        long returnTime = System.currentTimeMillis();

        long rcvTime = recMessage.getReceiveTimeStamp().getTime();
        long origTime = recMessage.getOriginateTimeStamp().getTime();
        long xmitTime = recMessage.getTransmitTimeStamp().getTime();

        long offset = Long.valueOf(((rcvTime - origTime) + (xmitTime - returnTime)) /2);
        return offset;
    }
}
