package com.nuggetchat.messenger.utils;

import android.app.Application;
import android.util.Log;

import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.nuggetchat.lib.common.Utils;
import com.nuggetchat.messenger.R;
import com.nuggetchat.messenger.base.NuggetInjector;

import java.util.Map;

public class AppsFlyerHelper {


    private static final String LOG_TAG = AppsFlyerHelper.class.getSimpleName();
    private static final String APPS_FLYER_DEV_KEY = "zGUQ6vtvz7KVFoFH9nBpCm";

    private static AppsFlyerHelper appsFlyerHelper = null;

    private Application application;
    private AppsFlyerLib appsFlyerLib;

    private AppsFlyerHelper(Application app) {
        this.application = app;
        appsFlyerLib = AppsFlyerLib.getInstance();
    }

    public static AppsFlyerHelper getInstance(Application app) {
        if (appsFlyerHelper == null) {
            appsFlyerHelper = new AppsFlyerHelper(app);
        }
        return appsFlyerHelper;
    }


    /**
     * Reference Source
     * https://github.com/AppsFlyerSDK/AndroidSampleApp/blob/master/app/src/main/java/android/appsflyer/sampleapp/MainActivity.java
     */

    public void initTracking() {
        /**
         #AppsFlyer: collecting your GCM project ID by setGCMProjectID allows you to track uninstalldata in your dashboard
         Please refer to this documentation for more information:
         https://support.appsflyer.com/hc/en-us/articles/208004986
         */
        String gcmSenderId = NuggetInjector.getInstance()
                .getString(R.string.gcm_defaultSenderId);
        appsFlyerLib.setGCMProjectNumber(application, gcmSenderId);

        /**
         #AppsFlyer: the startTracking method must be called after any optional 'Set' methods
         You can get your AppsFlyer Dev Key from the "SDK Integration" section in your dashboard
         */
        appsFlyerLib.startTracking(application, APPS_FLYER_DEV_KEY);

        /**
         #AppsFlyer: registerConversionListener implements the collection of attribution (conversion) data
         Please refer to this documentation to view all the available attribution parameters:
         https://support.appsflyer.com/hc/en-us/articles/207032096-Accessing-AppsFlyer-Attribution-Conversion-Data-from-the-SDK-Deferred-Deeplinking
         */
        appsFlyerLib.registerConversionListener(application, new MyAppsFlyerConversionListner(application) );
    }

    public void setUserId(String mixpanelUserId) {
        if (Utils.isNullOrEmpty(mixpanelUserId)) {
            appsFlyerLib.setCustomerUserId(mixpanelUserId);
        }
    }


    private class MyAppsFlyerConversionListner implements AppsFlyerConversionListener {

        private Application application;

        public MyAppsFlyerConversionListner(Application application) {
            this.application = application;
        }

        /**
         * Refer for available keys in attribution response.
         * https://support.appsflyer.com/hc/en-us/articles/207032096-Accessing-AppsFlyer-Attribution-Conversion-Data-from-the-SDK-iOS-Deferred-Deeplinking-
         */

        @Override
        public void onInstallConversionDataLoaded(Map<String, String> conversionData) {
            for (Map.Entry<String, String> data : conversionData.entrySet()) {
                SharedPreferenceUtility.setAppsFlyerConversionData(application,
                        data.getKey(), data.getValue());
            }
        }

        @Override
        public void onInstallConversionFailure(String errorMessage) {
            MyLog.e(LOG_TAG, "error getting conversion data: " + errorMessage);
        }

        @Override
        public void onAppOpenAttribution(Map<String, String> conversionData) {
            MyLog.i(LOG_TAG, "onApp open attribution");
        }

        @Override
        public void onAttributionFailure(String errorMessage) {
            MyLog.e(LOG_TAG, "error onAttributionFailure : " + errorMessage);
        }
    }
}
