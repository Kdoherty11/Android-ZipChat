package com.kdoherty.zipchat.utils;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

/**
 * Created by kdoherty on 10/27/14.
 */
public class GcmUtils {

    private static final String TAG = GcmUtils.class.getSimpleName();

    public static final String PREFS_REG_ID = "registrationId";
    private static final String PREFS_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String SENDER_ID = "1099267060283";
    public static final String PREFS_FILE_NAME = "GcmUtilsFileName";

    private static String regId;
    private static GoogleCloudMessaging gcm;

    private GcmUtils() {
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */

    public static String getRegistrationId(Context context) {
        String registrationId = PrefsUtils.readFromPreferences(context, PREFS_FILE_NAME, PREFS_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = PrefsUtils.readFromPreferences(context, PREFS_FILE_NAME, PREFS_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = Utils.getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    public static void register(Context context) {
        if (!TextUtils.isEmpty(regId)) {
            return;
        }
        if (checkPlayServices(context)) {
            if (gcm == null) {
                gcm = GoogleCloudMessaging.getInstance(context);
            }

            regId = getRegistrationId(context);

            if (regId.isEmpty()) {
                registerInBackground(context);
            }
        } else {
            Log.w(TAG, "No valid Google Play Services APK found.");
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private static void registerInBackground(final Context context) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";

                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regId;

                    String facebookName = FacebookUtils.getFacebookName(context);
                    String facebookId = FacebookUtils.getFacebookId(context);

                    Log.d(TAG, "Attempting to create user from GcmUtils.registerInBackground");
                    UserUtils.attemptCreateUser(context, facebookName, facebookId, regId);

                    // Persist the regID - no need to register again.
                    GcmUtils.storeRegistrationId(context, regId);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }
        }.execute(null, null, null);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean checkPlayServices(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, ((Activity) context),
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                ((Activity) context).finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId   registration ID
     */
    public static void storeRegistrationId(Context context, String regId) {
        int appVersion = Utils.getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);

        PrefsUtils.saveToPreferences(context, PREFS_FILE_NAME, PREFS_REG_ID, regId);
        PrefsUtils.saveToPreferences(context, PREFS_FILE_NAME, PREFS_APP_VERSION, appVersion);
    }
}
