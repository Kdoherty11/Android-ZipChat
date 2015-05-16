package com.kdoherty.zipchat.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.kdoherty.zipchat.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kdoherty on 12/15/14.
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    private Utils() {
        // Hide constructor
    }

    public static void hideKeyboardOnEnter(final Activity activity, final EditText et) {
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    hideKeyboard(activity, et);
                    return true;
                }
                return false;
            }
        });
    }

    public static void hideKeyboard(final Activity activity, final EditText et) {
        InputMethodManager in = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(et.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
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

    public static boolean checkServices(Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);

        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(resultCode, activity, 0).show();
            return false;
        }
    }

    public static void checkLocation(final Context context) {
        boolean locationEnabled = isLocationEnabled(context);
        if (!locationEnabled) {
            Resources res = context.getResources();
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(res.getString(R.string.location_not_enabled));
            dialog.setPositiveButton(res.getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                }
            });
            dialog.setNegativeButton(context.getString(R.string.cancel_location_dialog), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                }
            });
            dialog.show();
        }
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
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

    public static void checkLocation(final Activity activity) {
        if (!isLocationEnabled(activity)) {

            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setMessage(activity.getString(R.string.location_not_enabled));
            dialog.setPositiveButton(activity.getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(myIntent);
                }
            });

            dialog.setNegativeButton(activity.getString(R.string.cancel_location_dialog), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // Do nothing if user cancels
                }
            });

            dialog.show();

        }
    }

    private static boolean isLocationEnabled(Activity activity) {
        LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = false;
        boolean isNetworkEnabled = false;
        try {
            isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            // gps_enabled is false
        }
        try {
            isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            // network
        }
        return isGpsEnabled || isNetworkEnabled;
    }

    // Gets distance between two points in meters
    public static double getDistance(Location thisLoc, Location otherLoc) {

        final double thisLat = thisLoc.getLatitude();
        final double thisLong = thisLoc.getLongitude();

        final double otherLat = otherLoc.getLatitude();
        final double otherLong = otherLoc.getLongitude();

        return getDistance(thisLat, thisLong, otherLat, otherLong);
    }

    public static double getDistance(double thisLat, double thisLong, double otherLat, double otherLong) {
        final int earthRadius = 6371;
        return Math.acos(Math.sin(Math.toRadians(thisLat)) * Math.sin(Math.toRadians(otherLat)) + Math.cos(Math.toRadians(thisLat)) * Math.cos(Math.toRadians(otherLat)) * Math.cos(Math.toRadians(thisLong) - Math.toRadians(otherLong))) * earthRadius * 1000;
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

        Log.e(tag, sb.toString());
    }
}
