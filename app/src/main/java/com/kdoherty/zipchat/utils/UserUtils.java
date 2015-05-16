package com.kdoherty.zipchat.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class UserUtils {

    private static final String TAG = UserUtils.class.getSimpleName();

    private static final String PREFS_FILE = "UserUtilsPrefsFile";
    private static final String PREFS_USER_ID = "UtilsUserIdKey";
    private static final long DEFAULT_USER_ID = -1;

    private static final int CREATE_USER_LOCK_TIMEOUT_SECONDS = 9;

    private static final Semaphore createUserLock = new Semaphore(1);

    private UserUtils() {
    }

    public static void saveId(Context context, long userId) {
        PrefsUtils.saveToPreferences(context, PREFS_FILE, PREFS_USER_ID, userId);
    }

    public static long getId(Context context) {
        return PrefsUtils.readFromPreferences(context, PREFS_FILE, PREFS_USER_ID, DEFAULT_USER_ID);
    }

    public static User getSelf(Context context) {
        return new User(FacebookUtils.getFacebookName(context), FacebookUtils.getFacebookId(context), getId(context));
    }

    public static boolean didCreateUser(Context context) {
        return getId(context) != DEFAULT_USER_ID;
    }

    /**
     * Create a user on the server if
     * 1. We have not already created a user
     * 2. We have a facebook name
     * 3. We have a facebook id
     * 4. We have a registration id
     */
    public static void attemptCreateUser(final Context context, final String facebookName, final String facebookId, final String regId) {

        Runnable attemptCreateUser = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Attempting to aquire the create user lock");
                    boolean lockAcquired = createUserLock.tryAcquire(CREATE_USER_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    if (!lockAcquired) {
                        Log.d(TAG, "Could not acquire the create user lock within " + CREATE_USER_LOCK_TIMEOUT_SECONDS + " seconds");
                        return;
                    } else {
                        Log.d(TAG, "Acquired the create user lock");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Acquisition of the create user lock was interrupted: " + e.getMessage());
                    return;
                }

                if (didCreateUser(context) || TextUtils.isEmpty(facebookName) || TextUtils.isEmpty(facebookId) || TextUtils.isEmpty(regId)) {
                    Log.d(TAG, "Not enough information to create the user");
                    Log.d(TAG, "Releasing the create user lock in not enough info statement");
                    createUserLock.release();
                    return;
                }

                Log.d(TAG, "Attempting to add user to server");

                String platform = "android";

                ZipChatApi.INSTANCE.createUser(facebookName, facebookId, regId, platform, new Callback<Response>() {
                    @Override
                    public void success(Response result, Response response) {
                        String resultString = Utils.responseToString(result);

                        try {
                            JSONObject obj = new JSONObject(resultString);
                            long id = obj.getLong("userId");
                            Log.i(TAG, "Created userId: " + id);
                            saveId(context, id);
                            Log.d(TAG, "Successfully added user to server");
                        } catch (JSONException e) {
                            throw new RuntimeException("Problem parsing the create user response: " + e.getMessage());
                        } finally {
                            Log.d(TAG, "Releasing the create user lock in success callback");
                            createUserLock.release();
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.d(TAG, "Releasing the create user lock in failure callback");
                        createUserLock.release();
                        Utils.logErrorResponse(TAG, "Creating a user", error);
                    }
                });
            }
        };

        new Thread(attemptCreateUser).start();
    }
}
