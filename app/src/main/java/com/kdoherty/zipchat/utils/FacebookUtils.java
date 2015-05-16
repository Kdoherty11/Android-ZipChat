package com.kdoherty.zipchat.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphUser;

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

    private FacebookUtils() { }

    private static void saveFacebookInformation(Context context, String facebookName, String facebookId) {
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

    public static void storeFacebookInformation(final Context context) {
        // No need to store if we already have this information
        if (!getFacebookId(context).isEmpty() && !getFacebookName(context).isEmpty()) {
            return;
        }

        final Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            // If the session is open, make an API call to get user data
            // and define a new callback to handle the response
            Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    // If the response is successful
                    if (session == Session.getActiveSession()) {
                        if (user != null) {

                            String id = user.getId();
                            String name = user.getName();

                            Log.d(TAG, "Attempting to create user from FacebookUtils.storeFacebookInformation");
                            UserUtils.attemptCreateUser(context, name, id, GcmUtils.getRegistrationId(context));
                            saveFacebookInformation(context, name, id);
                        } else {
                            throw new IllegalStateException("Facebook user is null");
                        }
                    }
                }
            });
            Request.executeBatchAsync(request);
        }
    }

    public static Bitmap getFacebookProfilePicture(String userID) {
        try {
            URL imageURL = new URL("https://graph.facebook.com/" + userID + "/picture?type=large");
            return BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "Problem getting the facebook profile picture: " + e.getMessage());
            return null;
        }
    }
}
