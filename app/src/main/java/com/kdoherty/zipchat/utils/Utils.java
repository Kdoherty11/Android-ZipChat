package com.kdoherty.zipchat.utils;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.kdoherty.zipchat.BuildConfig;

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

    public static boolean checkServices(Activity activity) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);

        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(resultCode, activity, 0).show();
            return false;
        }
    }

    public static Long tryParse(String string) {
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        boolean negative = string.charAt(0) == '-';
        int index = negative ? 1 : 0;
        if (index == string.length()) {
            return null;
        }
        int digit = string.charAt(index++) - '0';
        if (digit < 0 || digit > 9) {
            return null;
        }
        long accum = -digit;
        while (index < string.length()) {
            digit = string.charAt(index++) - '0';
            if (digit < 0 || digit > 9 || accum < Long.MIN_VALUE / 10) {
                return null;
            }
            accum *= 10;
            if (accum < Long.MIN_VALUE + digit) {
                return null;
            }
            accum -= digit;
        }

        if (negative) {
            return accum;
        } else if (accum == Long.MIN_VALUE) {
            return null;
        } else {
            return -accum;
        }
    }

    public static void debugToast(Context context, String message) {
        debugToast(context, message, Toast.LENGTH_LONG);
    }

    public static void debugToast(final Context context, final String message, final int duration) {
        if (BuildConfig.DEBUG) {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, message, duration).show();
                    }
                });
            }
        }
    }

}
