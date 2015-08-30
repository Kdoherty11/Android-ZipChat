package com.kdoherty.zipchat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.kdoherty.zipchat.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.IOException;
import java.net.URL;

/**
 * Created by kdoherty on 12/31/14.
 */
public class FacebookManager {

    public static final DisplayImageOptions displayProfPicOpts = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.com_facebook_profile_picture_blank_portrait)
            .showImageForEmptyUri(R.drawable.com_facebook_profile_picture_blank_portrait)
            .showImageOnFail(R.drawable.com_facebook_profile_picture_blank_portrait)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .build();
    private static final String TAG = FacebookManager.class.getSimpleName();
    private static final String FACEBOOK_PREFERENCES = "facebookPreferences";
    private static final String PREFS_USER_ID_KEY = "facebookUserIdKey";
    private static final String PREFS_FACEBOOK_NAME_KEY = "facebookUsernameKey";
    private static final int DEFAULT_WIDTH = 250;
    private static final int DEFAULT_HEIGHT = 250;

    private FacebookManager() {
    }

    public static void saveFacebookInformation(Context context, String facebookName, String facebookId) {
        PrefsHelper.saveToPreferences(context, FACEBOOK_PREFERENCES, PREFS_FACEBOOK_NAME_KEY, facebookName);
        PrefsHelper.saveToPreferences(context, FACEBOOK_PREFERENCES, PREFS_USER_ID_KEY, facebookId);
    }

    public static String getFacebookId(Context context) {
        return PrefsHelper.readFromPreferences(context, FACEBOOK_PREFERENCES, PREFS_USER_ID_KEY, "");
    }

    public static String getFacebookName(Context context) {
        return PrefsHelper.readFromPreferences(context, FACEBOOK_PREFERENCES, PREFS_FACEBOOK_NAME_KEY, "");
    }

    public static void clearStoredFacebookInformation(Context context) {
        PrefsHelper.clearPreferences(context, FACEBOOK_PREFERENCES);
    }

    public static String getProfilePicUrl(String userId) {
        return getProfilePicUrl(userId, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public static String getProfilePicUrl(String userId, String type) {
        return "https://graph.facebook.com/" + userId + "/picture?type=" + type;
    }

    public static String getProfilePicUrl(String userId, int width, int height) {
        return "https://graph.facebook.com/" + userId + "/picture?width=" + width + "&height=" + height;
    }

    public static void displayProfilePicture(String userId, ImageView imageView) {
        ImageLoader.getInstance().displayImage(getProfilePicUrl(userId), imageView, displayProfPicOpts);
    }

    public static void displayProfilePicture(String userId, ImageView imageView, String type) {
        ImageLoader.getInstance().displayImage(getProfilePicUrl(userId, type), imageView, displayProfPicOpts);
    }

    public static void displayProfilePicture(String userId, ImageView imageView, int width, int height) {
        ImageLoader.getInstance().displayImage(getProfilePicUrl(userId, width, height), imageView, displayProfPicOpts);
    }



    public static Bitmap getFacebookProfilePicture(String userId) {
        try {
            URL imageURL = new URL(getProfilePicUrl(userId, "square"));
            return BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "Problem getting the facebook profile picture: " + e.getMessage());
            return null;
        }
    }
}
