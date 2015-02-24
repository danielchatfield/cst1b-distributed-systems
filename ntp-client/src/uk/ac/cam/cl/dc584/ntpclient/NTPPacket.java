package uk.ac.cam.cl.dc584.ntpclient;

import java.net.DatagramPacket;

public class NTPPacket {
    private static final int MODE_INDEX = 0;
    private static final int MODE_SHIFT = 0;

    private static final int VERSION_INDEX = 0;
    private static final int VERSION_SHIFT = 3;

    private static final int LI_INDEX = 0;
    private static final int LI_SHIFT = 6;

    private static final int STRATUM_INDEX = 1;
    private static final int POLL_INDEX = 2;
    private static final int PRECISION_INDEX = 3;

    private static final int ROOT_DELAY_INDEX = 4;
    private static final int ROOT_DISPERSION_INDEX = 8;
    private static final int REFERENCE_ID_INDEX = 12;

    private static final int REFERENCE_TIMESTAMP_INDEX = 16;
    private static final int ORIGINATE_TIMESTAMP_INDEX = 24;
    private static final int RECEIVE_TIMESTAMP_INDEX = 32;
    private static final int TRANSMIT_TIMESTAMP_INDEX = 40;

    private byte[] buf = new byte[48];
    private DatagramPacket dp;

    public void NTPPacket() {
    }

    public int getMode() {
        return (ui(buf[MODE_INDEX]) >> MODE_SHIFT) & 0x7;
    }

    public void setMode(int mode) {
        buf[MODE_INDEX] = (byte) (buf[MODE_INDEX] & 0xF8 | mode & 0x7);
    }

    public int getLeapIndicator() {
        return (ui(buf[LI_INDEX]) >> LI_SHIFT) & 0x3;
    }

    public void setLeapIndicator(int li) {
        buf[LI_INDEX] = (byte) (buf[LI_INDEX] & 0x3F | ((li & 0x3) << LI_SHIFT));
    }

    public int getPoll() {
        return buf[POLL_INDEX];
    }

    public void setPoll(int poll) {
        buf[POLL_INDEX] = (byte) (poll & 0xFF);
    }

    public int getPrecision() {
        return buf[PRECISION_INDEX];
    }

    public void setPrecision(int precision) {
        buf[PRECISION_INDEX] = (byte) (precision & 0xFF);
    }

    public int getVersion() {
        return (ui(buf[VERSION_INDEX]) >> VERSION_SHIFT) & 0x7;
    }

    public void setVersion(int version) {
        buf[VERSION_INDEX] = (byte) (buf[VERSION_INDEX] & 0xC7 | ((version & 0x7) << VERSION_SHIFT));
    }

    public int getStratum() {
        return ui(buf[STRATUM_INDEX]);
    }

    public void setStratum(int stratum) {
        buf[STRATUM_INDEX] = (byte) (stratum & 0xFF);
    }

    public int getRootDelay() {
        return getInt(ROOT_DELAY_INDEX);
    }

    public void setRootDelay(int delay) {
        setInt(ROOT_DELAY_INDEX, delay);
    }

    public double getRootDelayInMillisDouble() {
        double l = getRootDelay();
        return l / 65.536;
    }

    public int getRootDispersion() {
        return getInt(ROOT_DISPERSION_INDEX);
    }

    public void setRootDispersion(int dispersion) {
        setInt(ROOT_DISPERSION_INDEX, dispersion);
    }

    public long getRootDispersionInMillis() {
        long l = getRootDispersion();
        return (l * 1000) / 65536L;
    }

    public double getRootDispersionInMillisDouble() {
        double l = getRootDispersion();
        return l / 65.536;
    }

    public void setReferenceId(int refId) {
        setInt(REFERENCE_ID_INDEX, refId);
    }

    public int getReferenceId() {
        return getInt(REFERENCE_ID_INDEX);
    }

    public TimeStamp getTransmitTimeStamp() {
        return getTimestamp(TRANSMIT_TIMESTAMP_INDEX);
    }

    public void setTransmitTime(TimeStamp ts) {
        setTimestamp(TRANSMIT_TIMESTAMP_INDEX, ts);
    }


    public void setOriginateTimeStamp(TimeStamp ts) {
        setTimestamp(ORIGINATE_TIMESTAMP_INDEX, ts);
    }

    public TimeStamp getOriginateTimeStamp() {
        return getTimestamp(ORIGINATE_TIMESTAMP_INDEX);
    }

    public TimeStamp getReferenceTimeStamp() {
        return getTimestamp(REFERENCE_TIMESTAMP_INDEX);
    }

    public void setReferenceTime(TimeStamp ts) {
        setTimestamp(REFERENCE_TIMESTAMP_INDEX, ts);
    }

    public TimeStamp getReceiveTimeStamp() {
        return getTimestamp(RECEIVE_TIMESTAMP_INDEX);
    }

    public void setReceiveTimeStamp(TimeStamp ts) {
        setTimestamp(RECEIVE_TIMESTAMP_INDEX, ts);
    }

    private int getInt(int index) {
        int i = ui(buf[index]) << 24 |
                ui(buf[index + 1]) << 16 |
                ui(buf[index + 2]) << 8 |
                ui(buf[index + 3]);

        return i;
    }

    private void setInt(int idx, int value) {
        for (int i=3; i >= 0; i--) {
            buf[idx + i] = (byte) (value & 0xff);
            value >>>= 8; // shift right one-byte
        }
    }

    private TimeStamp getTimestamp(int index) {
        return new TimeStamp(getLong(index));
    }


    private long getLong(int index) {
        long i = ul(buf[index]) << 56 |
                ul(buf[index + 1]) << 48 |
                ul(buf[index + 2]) << 40 |
                ul(buf[index + 3]) << 32 |
                ul(buf[index + 4]) << 24 |
                ul(buf[index + 5]) << 16 |
                ul(buf[index + 6]) << 8 |
                ul(buf[index + 7]);
        return i;
    }

    private void setTimestamp(int index, TimeStamp t) {
        long ntpTime = (t == null) ? 0 : t.ntpValue();
        // copy 64-bits from Long value into 8 x 8-bit bytes of array
        // one byte at a time shifting 8-bits for each position.
        for (int i = 7; i >= 0; i--) {
            buf[index + i] = (byte) (ntpTime & 0xFF);
            ntpTime >>>= 8; // shift to next byte
        }
        // buf[index] |= 0x80;  // only set if 1900 baseline....
    }

    public synchronized DatagramPacket getDatagramPacket() {
        if (dp == null) {
            dp = new DatagramPacket(buf, buf.length);
            dp.setPort(123);
        }
        return dp;
    }

    public void setDatagramPacket(DatagramPacket srcDp) {
        if (srcDp == null || srcDp.getLength() < buf.length) {
            throw new IllegalArgumentException();
        }
        byte[] incomingBuf = srcDp.getData();
        int len = srcDp.getLength();
        if (len > buf.length) {
            len = buf.length;
        }
        System.arraycopy(incomingBuf, 0, buf, 0, len);
        DatagramPacket dp = getDatagramPacket();
        dp.setAddress(srcDp.getAddress());
        int port = srcDp.getPort();
        dp.setPort(port > 0 ? port : 123);
        dp.setData(buf);
    }


    protected final static int ui(byte b) {
        int i = b & 0xFF;
        return i;
    }

    protected final static long ul(byte b) {
        long i = b & 0xFF;
        return i;
    }
}
