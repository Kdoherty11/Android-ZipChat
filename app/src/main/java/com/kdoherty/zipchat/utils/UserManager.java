package com.kdoherty.zipchat.utils;

import android.content.Context;

import com.kdoherty.zipchat.models.User;

public class UserManager {

    private static final String TAG = UserManager.class.getSimpleName();

    private static final String PREFS_FILE = "utils.UserManager.prefs.FILE";

    private static final String PREFS_USER_ID = "utils.UserManager.prefs.USER_ID";
    private static final String PREFS_AUTH_TOKEN = "utils.UserManager.prefs.AUTH_TOKEN";
    private static final String PREFS_AUTH_TOKEN_TIMESTAMP = "utils.UserManager.prefs.AUTH_TOKEN_TIMESTAMP";
    private static final String PREFS_DEVICE_ID = "utils.UserManager.prefs.DEVICE_ID";

    private static final long DEFAULT_USER_ID = -1;
    private static final String DEFAULT_AUTH_TOKEN = "";
    private static final long DEFAULT_DEVICE_ID = -1;
    private static User self = null;

    private UserManager() {
    }

    public static void storeId(Context context, long userId) {
        PrefsHelper.saveToPreferences(context, PREFS_FILE, PREFS_USER_ID, userId);
    }

    public static long getId(Context context) {
        return PrefsHelper.readFromPreferences(context, PREFS_FILE, PREFS_USER_ID, DEFAULT_USER_ID);
    }

//    public static boolean isAuthTokenExpired(Context context) {
//        long defaultTimeStamp = -1;
//        long authTokenTimeStamp = PrefsHelper.readFromPreferences(context, PREFS_FILE, PREFS_AUTH_TOKEN_TIMESTAMP, defaultTimeStamp);
//        long now = new Date().getTime();
//        long expirationTimeMillis = 24 * 60 * 60 * 1000;
//        return authTokenTimeStamp == defaultTimeStamp || authTokenTimeStamp + expirationTimeMillis >= now;
//    }

    public static void storeAuthToken(Context context, String authToken) {
        PrefsHelper.saveToPreferences(context, PREFS_FILE, PREFS_AUTH_TOKEN_TIMESTAMP, authToken);
        PrefsHelper.saveToPreferences(context, PREFS_FILE, PREFS_AUTH_TOKEN, authToken);
    }

    public static String getAuthToken(Context context) {
        return PrefsHelper.readFromPreferences(context, PREFS_FILE, PREFS_AUTH_TOKEN, DEFAULT_AUTH_TOKEN);
    }

    public static void storeDeviceId(Context context, long deviceId) {
        PrefsHelper.saveToPreferences(context, PREFS_FILE, PREFS_DEVICE_ID, deviceId);
    }

    public static long getDeviceId(Context context) {
        return PrefsHelper.readFromPreferences(context, PREFS_FILE, PREFS_DEVICE_ID, DEFAULT_DEVICE_ID);
    }

    public static boolean didCreateUser(Context context) {
        return getId(context) != DEFAULT_USER_ID;
    }

    public static boolean didRegisterDevice(Context context) {
        return getDeviceId(context) != DEFAULT_DEVICE_ID;
    }

    public static User getSelf(Context context) {
        if (self == null) {
            self = new User(getId(context), FacebookManager.getFacebookId(context), FacebookManager.getFacebookName(context));
        }
        return self;
    }

}
