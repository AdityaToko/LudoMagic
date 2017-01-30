package com.nuggetchat.messenger.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.compat.BuildConfig;

import java.util.Hashtable;

public class Typefaces {
    private static final String LOG_TAG = Typefaces.class.getSimpleName();
    private static final Hashtable<String, Typeface> cache = new Hashtable<>();

    @Nullable
    public static Typeface get(Context context, String assetPath) {
        synchronized (cache) {
            if (!cache.containsKey(assetPath)) {
                try {
                    Typeface typeface = Typeface.createFromAsset(context.getAssets(), assetPath);
                    // Store null to avoid loading again. Caller should handle null.
                    cache.put(assetPath, typeface);
                    if (BuildConfig.DEBUG) {
                        MyLog.v(LOG_TAG, "Loaded - " + assetPath);
                    }
                } catch (Exception e) {
                    MyLog.e(LOG_TAG, "Unable to load " + assetPath, e);
                }
            }
            return cache.get(assetPath);
        }
    }
}
