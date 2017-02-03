package com.nuggetchat.lib.common;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {
    private Utils() {}


    public static String getCleanStr(String str) {
        return (str == null) ? "" : str.trim();
    }

    public static boolean isNullOrEmpty(String str) {
        str = getCleanStr(str);
        return str.isEmpty();
    }


    public static long getCurrentTimestampMs() {
        return System.currentTimeMillis();
    }

    public static String getDateFromTimestampMs(long currentTimestampMs) {
        Timestamp stamp = new Timestamp(currentTimestampMs);
        Date date = new Date(stamp.getTime());
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return getCurrentDate(cal);
    }

    public static String getCurrentDate(Calendar cal) {
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DATE);
        String date = String.format(Locale.ENGLISH, "%4d%02d%02d", year, month, day);
        return String.valueOf(date);
    }

    public static String getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return getCurrentDate(cal);
    }

    /**
     * Get exception stack trace to string
     * @param throwable
     * @return
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
