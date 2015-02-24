package uk.ac.cam.cl.dc584.ntpclient;


        import java.text.DateFormat;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.Locale;
        import java.util.TimeZone;


public class TimeStamp implements java.io.Serializable, Comparable<TimeStamp>
{

    protected static final long msb0baseTime = 2085978496000L;
    protected static final long msb1baseTime = -2208988800000L;
    public final static String NTP_DATE_FORMAT = "EEE, MMM dd yyyy HH:mm:ss.SSS";
    private final long ntpTime;

    private DateFormat simpleFormatter;
    private DateFormat utcFormatter;

    public TimeStamp(long ntpTime) {
        this.ntpTime = ntpTime;
    }

    public TimeStamp(String hexStamp) throws NumberFormatException {
        ntpTime = decodeNtpHexString(hexStamp);
    }

    public TimeStamp(Date d) {
        ntpTime = (d == null) ? 0 : toNtpTime(d.getTime());
    }

    public long ntpValue() {
        return ntpTime;
    }

    public long getSeconds() {
        return (ntpTime >>> 32) & 0xffffffffL;
    }


    public long getFraction() {
        return ntpTime & 0xffffffffL;
    }


    public long getTime() {
        return getTime(ntpTime);
    }


    public Date getDate() {
        long time = getTime(ntpTime);
        return new Date(time);
    }

    public static long getTime(long ntpTimeValue) {
        long seconds = (ntpTimeValue >>> 32) & 0xffffffffL;     // high-order 32-bits
        long fraction = ntpTimeValue & 0xffffffffL;             // low-order 32-bits

        // Use round-off on fractional part to preserve going to lower precision
        fraction = Math.round(1000D * fraction / 0x100000000L);

        /*
         * If the most significant bit (MSB) on the seconds field is set we use
         * a different time base. The following text is a quote from RFC-2030 (SNTP v4):
         *
         *  If bit 0 is set, the UTC time is in the range 1968-2036 and UTC time
         *  is reckoned from 0h 0m 0s UTC on 1 January 1900. If bit 0 is not set,
         *  the time is in the range 2036-2104 and UTC time is reckoned from
         *  6h 28m 16s UTC on 7 February 2036.
         */
        long msb = seconds & 0x80000000L;
        if (msb == 0) {
            // use base: 7-Feb-2036 @ 06:28:16 UTC
            return msb0baseTime + (seconds * 1000) + fraction;
        } else {
            // use base: 1-Jan-1900 @ 01:00:00 UTC
            return msb1baseTime + (seconds * 1000) + fraction;
        }
    }

    public static TimeStamp getNtpTime(long date) {
        return new TimeStamp(toNtpTime(date));
    }


    public static TimeStamp getCurrentTime() {
        return getNtpTime(System.currentTimeMillis());
    }


    protected static long decodeNtpHexString(String hexString)
            throws NumberFormatException {
        if (hexString == null) {
            throw new NumberFormatException("null");
        }
        int ind = hexString.indexOf('.');
        if (ind == -1) {
            if (hexString.length() == 0) {
                return 0;
            }
            return Long.parseLong(hexString, 16) << 32; // no decimal
        }

        return Long.parseLong(hexString.substring(0, ind), 16) << 32 |
                Long.parseLong(hexString.substring(ind + 1), 16);
    }


    public static TimeStamp parseNtpString(String s)
            throws NumberFormatException {
        return new TimeStamp(decodeNtpHexString(s));
    }

    protected static long toNtpTime(long t) {
        boolean useBase1 = t < msb0baseTime;    // time < Feb-2036
        long baseTime;
        if (useBase1) {
            baseTime = t - msb1baseTime; // dates <= Feb-2036
        } else {
            // if base0 needed for dates >= Feb-2036
            baseTime = t - msb0baseTime;
        }

        long seconds = baseTime / 1000;
        long fraction = ((baseTime % 1000) * 0x100000000L) / 1000;

        if (useBase1) {
            seconds |= 0x80000000L; // set high-order bit if msb1baseTime 1900 used
        }

        long time = seconds << 32 | fraction;
        return time;
    }

    public int hashCode() {
        return (int) (ntpTime ^ (ntpTime >>> 32));
    }

    public boolean equals(Object obj) {
        if (obj instanceof TimeStamp) {
            return ntpTime == ((TimeStamp) obj).ntpValue();
        }
        return false;
    }


    public String toString() {
        return toString(ntpTime);
    }


    private static void appendHexString(StringBuilder buf, long l) {
        String s = Long.toHexString(l);
        for (int i = s.length(); i < 8; i++) {
            buf.append('0');
        }
        buf.append(s);
    }


    public static String toString(long ntpTime) {
        StringBuilder buf = new StringBuilder();
        // high-order second bits (32..63) as hexstring
        appendHexString(buf, (ntpTime >>> 32) & 0xffffffffL);

        // low-order fractional seconds bits (0..31) as hexstring
        buf.append('.');
        appendHexString(buf, ntpTime & 0xffffffffL);

        return buf.toString();
    }


    public String toDateString() {
        if (simpleFormatter == null) {
            simpleFormatter = new SimpleDateFormat(NTP_DATE_FORMAT, Locale.US);
            simpleFormatter.setTimeZone(TimeZone.getDefault());
        }
        Date ntpDate = getDate();
        return simpleFormatter.format(ntpDate);
    }

    public String toUTCString() {
        if (utcFormatter == null) {
            utcFormatter = new SimpleDateFormat(NTP_DATE_FORMAT + " 'UTC'",
                    Locale.US);
            utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        Date ntpDate = getDate();
        return utcFormatter.format(ntpDate);
    }


    public int compareTo(TimeStamp anotherTimeStamp) {
        long thisVal = this.ntpTime;
        long anotherVal = anotherTimeStamp.ntpTime;
        return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
    }

}
