package com.kdoherty.zipchat.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by kdoherty on 12/30/14.
 */
public class PrefsHelper {

    private PrefsHelper() {
        // Hide constructor
    }

    public static void saveToPreferences(Context context, String prefsFileName, String preferenceName, String preferenceValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(preferenceName, preferenceValue).apply();
    }

    public static String readFromPreferences(Context context, String prefsFileName, String preferenceName, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        return sharedPreferences.getString(preferenceName, defaultValue);
    }

    public static void saveToPreferences(Context context, String prefsFileName, String preferenceName, boolean preferenceValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(preferenceName, preferenceValue).apply();
    }

    public static boolean readFromPreferences(Context context, String prefsFileName, String preferenceName, boolean defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(preferenceName, defaultValue);
    }

    public static void saveToPreferences(Context context, String prefsFileName, String preferenceName, int preferenceValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(preferenceName, preferenceValue).apply();
    }

    public static int readFromPreferences(Context context, String prefsFileName, String preferenceName, int defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(preferenceName, defaultValue);
    }

    public static void saveToPreferences(Context context, String prefsFileName, String preferenceName, long preferenceValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        sharedPreferences.edit().putLong(preferenceName, preferenceValue).apply();
    }

    public static long readFromPreferences(Context context, String prefsFileName, String preferenceName, long defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(preferenceName, defaultValue);
    }

    public static boolean removeFromPreferences(Context context, String prefsFileName, String preferenceName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        boolean containedPref = sharedPreferences.contains(preferenceName);
        sharedPreferences.edit().remove(preferenceName).apply();
        return containedPref;
    }

    public static void clearPreferences(Context context, String prefsFileName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }
}
