package com.kdoherty.zipchat.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

public class MyInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = "MyInstanceIDLS";

    @Override
    public void onTokenRefresh() {
        Log.i(TAG, "onTokenRefresh called");
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}
