package com.kdoherty.zipchat.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";

    public static final String DEFAULT_REG_ID = "";

    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent!!");
        try {
            synchronized (TAG) {
                InstanceID instanceID = InstanceID.getInstance(this);

                String token = instanceID.getToken("1099267060283",
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                Log.i(TAG, "GCM Registration Token: " + token);

                sendRegistrationToServer(token);

                subscribeTopics(token);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }
    }

    private void sendRegistrationToServer(String token) {
        final Context appContext = getApplicationContext();
        if (UserManager.didRegisterDevice(appContext)) {
            updateRegId(token);
        } else {
            registerDevice(token);
        }
    }

    private void updateRegId(String token) {
        ZipChatApi.INSTANCE.replaceRegId(UserManager.getAuthToken(getApplicationContext()), UserManager.getDeviceId(getApplicationContext()), token, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.i(TAG, "Successfully replaced regId");
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.logErrorResponse(TAG, "Replacing regId", error);
            }
        });
    }

    private void registerDevice(String token) {
        ZipChatApi.INSTANCE.registerDevice(UserManager.getAuthToken(getApplicationContext()), UserManager.getId(getApplicationContext()), token, "android", new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.i(TAG, "Successfully registered device");
                String respStr = NetworkManager.responseToString(response);
                try {
                    JSONObject respJson = new JSONObject(respStr);
                    long deviceId = respJson.getLong("deviceId");
                    UserManager.storeDeviceId(getApplicationContext(), deviceId);
                    Log.i(TAG, "Successfully stored device id: " + deviceId);
                } catch (JSONException e) {
                    Log.e(TAG, "Parsing register device json", e);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.logErrorResponse(TAG, "Registering device", error);
            }
        });
    }

    private void subscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }

}
