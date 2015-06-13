package com.kdoherty.zipchat.utils;

import android.content.Context;

import com.kdoherty.zipchat.models.User;

public class UserInfo {

    private static final String TAG = UserInfo.class.getSimpleName();

    private static final String PREFS_FILE = "UserInfoPrefsFile";

    private static final String PREFS_USER_ID = "userId";
    private static final String PREFS_AUTH_TOKEN = "authTokenKey";
    private static final String PREFS_DEVICE_ID = "deviceId";

    private static final long DEFAULT_USER_ID = -1;
    private static final String DEFAULT_AUTH_TOKEN = "";
    private static final long DEFAULT_DEVICE_ID = -1;

    private UserInfo() { }

    public static void storeId(Context context, long userId) {
        PrefsHelper.saveToPreferences(context, PREFS_FILE, PREFS_USER_ID, userId);
    }

    public static long getId(Context context) {
        return PrefsHelper.readFromPreferences(context, PREFS_FILE, PREFS_USER_ID, DEFAULT_USER_ID);
    }

    public static void storeAuthToken(Context context, String authToken) {
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
        return new User(FacebookManager.getFacebookName(context), FacebookManager.getFacebookId(context), getId(context));
    }

}
