package com.kdoherty.zipchat.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.kdoherty.zipchat.fragments.LoginFragment;
import com.kdoherty.zipchat.utils.GcmUtils;
import io.fabric.sdk.android.Fabric;


public class LoginActivity extends FragmentActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private LoginFragment mLoginFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Log.d(TAG, "Logging in");

        GcmUtils.register(this);

        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            mLoginFragment = new LoginFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, mLoginFragment)
                    .commit();
        } else {
            // Or set the fragment from restored state info
            mLoginFragment = (LoginFragment) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }

    }
}