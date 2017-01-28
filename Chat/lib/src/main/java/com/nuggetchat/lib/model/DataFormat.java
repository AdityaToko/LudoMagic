package com.nuggetchat.lib.model;

/**
 * DataFormat - Data formatting related assertions String clean, key clean, integer clean
 */
public class DataFormat {

    public static int getCleanInt(String str) {
        return getCleanInt(str, 0);
    }

    public static int getCleanInt(String str, int defaultVal) {
        String cleanStr = getCleanStr(str);
        try {
            return Integer.parseInt(cleanStr);
        } catch (NumberFormatException ignored) {}

        return defaultVal;
    }

    public static float getCleanFloat(String str) {
        return getCleanFloat(str, 0);
    }

    public static float getCleanFloat(String str, float defaultVal) {
        String cleanStr = getCleanStr(str);
        try {
            return Float.valueOf(cleanStr);
        } catch (NumberFormatException ignored) {}

        return defaultVal;
    }

    public static String getCleanStr(String str) {
        return (str == null) ? "" : str.trim();
    }

    public static String getCleanEmbedCode(String embedStr) {
        return getCleanStr(embedStr);
    }

    /**
     * A child node's key cannot be longer than 768 bytes.
     * It can include any unicode characters except for . $ # [ ] / and ASCII
     * control characters 0-31 and 127.
     */
    public static String getCleanFbaseKey(String fbaseKey) {
        fbaseKey = getCleanStr(fbaseKey); // Not doing toLower since this could reduce key space.
        fbaseKey = fbaseKey.replaceAll("[ \t\r\n\\.\\$#\\[\\]/\u007F\u0000-\u001F]", "");
        if (fbaseKey.length() > 700) {
            fbaseKey = fbaseKey.substring(0, 700);
        }
        return fbaseKey;
    }

    public static boolean isNotNullNorEmpty(String str) {
        return !isNullOrEmpty(str);
    }

    public static boolean isNullOrEmpty(String str) {
        str = getCleanEmbedCode(str);
        return str.isEmpty();
    }

    public static String getTagKey(String tagName) {
        return getCleanFbaseKey(tagName).toLowerCase();
    }

    public static String getFbaseKeyFromUrl(String url) {
        String fbaseKey = getCleanFbaseKey(url).toLowerCase();
        if (DataFormat.isNotNullNorEmpty(fbaseKey)) {
            fbaseKey = fbaseKey.replaceAll("http(s)?:", "").replaceAll("[\\?&=]", "_");
        }
        return fbaseKey;
    }

    public static String convertWhiteSpaceToSpace(String string) {
        if (string != null) {
            return string.replaceAll("\\s+", " ").trim();
        } else {
            return "";
        }
    }
}
