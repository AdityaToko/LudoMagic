package com.nuggetchat.lib.common;

public class Utils {
    private Utils() {}

    public static boolean isNullOrEmpty(String str) {
        str = (str == null) ? "" : str.trim();
        return str.isEmpty();
    }
}
