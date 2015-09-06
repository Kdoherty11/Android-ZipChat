package com.kdoherty.zipchat.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.kdoherty.zipchat.fragments.LoginFragment;
import com.kdoherty.zipchat.utils.Utils;

import io.fabric.sdk.android.Fabric;


public class LoginActivity extends AbstractLocationActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        Fabric.with(this, new Crashlytics());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.d(TAG, "Logging in");

        Utils.checkServices(this);

        getSupportFragmentManager()
                .beginTransaction()
                .add(android.R.id.content, new LoginFragment())
                .commit();
    }
}
