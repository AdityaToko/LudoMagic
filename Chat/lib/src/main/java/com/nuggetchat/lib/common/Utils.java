package com.nuggetchat.lib.common;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Utils {
    private Utils() {}

    public static boolean isNullOrEmpty(String str) {
        str = (str == null) ? "" : str.trim();
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
}
