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
public class FacebookUtils {

    private static final String TAG = FacebookUtils.class.getSimpleName();

    private static final String FACEBOOK_PREFERENCES = "facebookPreferences";
    private static final String PREFS_USER_ID_KEY = "facebookUserIdKey";
    private static final String PREFS_FACEBOOK_NAME_KEY = "facebookUsernameKey";

    public static final DisplayImageOptions displayProfPicOpts = new DisplayImageOptions.Builder()
            .showImageOnLoading(R.drawable.com_facebook_profile_picture_blank_portrait)
            .showImageForEmptyUri(R.drawable.com_facebook_profile_picture_blank_portrait)
            .showImageOnFail(R.drawable.com_facebook_profile_picture_blank_portrait)
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .considerExifParams(true)
            .build();

    private FacebookUtils() { }

    public static void saveFacebookInformation(Context context, String facebookName, String facebookId) {
        PrefsUtils.saveToPreferences(context, FACEBOOK_PREFERENCES, PREFS_FACEBOOK_NAME_KEY, facebookName);
        PrefsUtils.saveToPreferences(context, FACEBOOK_PREFERENCES, PREFS_USER_ID_KEY, facebookId);
    }

    public static String getFacebookId(Context context) {
        return PrefsUtils.readFromPreferences(context, FACEBOOK_PREFERENCES, PREFS_USER_ID_KEY, "");
    }

    public static String getFacebookName(Context context) {
        return PrefsUtils.readFromPreferences(context, FACEBOOK_PREFERENCES, PREFS_FACEBOOK_NAME_KEY, "");
    }

    public static void clearStoredFacebookInformation(Context context) {
        PrefsUtils.clearPreferences(context, FACEBOOK_PREFERENCES);
    }

    public static String getProfilePicUrl(String userId) {
        return getProfilePicUrl(userId, "square");
    }

    public static String getProfilePicUrl(String userId, String type) {
        return "https://graph.facebook.com/" + userId + "/picture?type=" + type;
    }

    public static void displayProfilePicture(String userId, ImageView imageView) {
        ImageLoader.getInstance().displayImage(getProfilePicUrl(userId), imageView, displayProfPicOpts);
    }

    public static void displayProfilePicture(String userId, ImageView imageView, String type) {
        ImageLoader.getInstance().displayImage(getProfilePicUrl(userId, type), imageView, displayProfPicOpts);
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
