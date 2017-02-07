package com.nuggetchat.messenger.activities;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;

import com.appsflyer.AppsFlyerLib;
import com.nuggetchat.messenger.R;

/**
 * Deeplink testing
 * <pre>
 *     Assuming the activity manifest has following data scheme
 *
 *     <data android:scheme="http" android:host="nuggetchat.com"/>
 *     {@code
 *      adb shell am start -W -a android.intent.action.VIEW -d  "http://nuggetchat.com" com.nuggetmessenger.games.debug
 *     }
 *
 *     <data android:scheme="nuggetchat"/>
 *     {@code
 *      adb shell am start -W -a android.intent.action.VIEW -d  "nuggetchat://" com.nuggetmessenger.games.debug
 *     }
 *
 * </pre>
 */
public class AppsFlyerDeeplink extends Activity {

    private static final String LOG_TAG = AppsFlyerDeeplink.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "onCreate");

        setContentView(R.layout.activity_deeplink);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        /**
         #AppsFlyer: sendDeepLinkData method must be the first one the be called in the onCreate() of your Deep Link activity
         Please refer to your AndroidManifest.xml for AppsFlyer deep links for the sample app
         **/
        AppsFlyerLib.getInstance().sendDeepLinkData(this);

        /**
         * The call will trigger {@link com.nuggetchat.messenger.utils.AppsFlyerHelper.MyAppsFlyerConversionListner}
         */
    }
}
