package com.kdoherty.zipchat.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.kdoherty.zipchat.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevin on 6/8/15.
 */
public final class NetworkManager {

    private static final String TAG = NetworkManager.class.getSimpleName();

    private NetworkManager() {
    }


    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static boolean checkOnline(Context context) {
        return checkOnline(context, context.getString(R.string.default_no_internet_toast));
    }

    public static boolean checkOnline(Context context, String message) {
        boolean isOnline = isOnline(context);
        if (!isOnline) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        return isOnline;
    }

    public static String responseToString(Response result) {
        if (result == null || result.getBody() == null) {
            return "";
        }
        BufferedReader reader;
        StringBuilder sb = new StringBuilder();
        try {

            reader = new BufferedReader(new InputStreamReader(result.getBody().in()));

            String line;

            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Problem reading response " + e.getMessage());
            }
        } catch (IOException e) {
            Log.e(TAG, "Problem initializing BufferedReader " + e.getMessage());
        }

        return sb.toString();
    }

    public static void logErrorResponse(String tag, String scenario, RetrofitError error) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(scenario)) {
            sb.append("Scenario: ");
            sb.append(scenario);
        }

        if (error != null) {
            sb.append("\nURL: ");
            sb.append(error.getUrl());

            if (error.getResponse() != null) {
                sb.append("\nStatus: ");
                sb.append(error.getResponse().getStatus());
            }

            sb.append("\nResponse: ");
            sb.append(responseToString(error.getResponse()));

            sb.append("\nMessage: ");
            sb.append(error.getMessage());
        }

        String errorMessage = sb.toString();

        Crashlytics.log(Log.ERROR, tag, errorMessage);
    }
}
