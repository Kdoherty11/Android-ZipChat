package com.kdoherty.zipchat.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.HomeActivity;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookUtils;
import com.kdoherty.zipchat.utils.PrefsUtils;
import com.kdoherty.zipchat.utils.UserUtils;
import com.kdoherty.zipchat.utils.Utils;
import com.koushikdutta.async.Util;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginFragment extends Fragment implements FacebookCallback<LoginResult> {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private CallbackManager mCallbackManager;
    private LoginButton mLoginButton;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mCallbackManager = CallbackManager.Factory.create();
        final LoginButton mLoginButton = (LoginButton) view.findViewById(R.id.login_button);
        mLoginButton.setReadPermissions("user_friends");
        mLoginButton.setFragment(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        if (!TextUtils.isEmpty(currentAccessToken.getToken()) && !currentAccessToken.isExpired()) {
            authUser(currentAccessToken.getToken());
        } else {
            mLoginButton.registerCallback(mCallbackManager, this);
        }
    }

    private void authUser(String accessToken) {
        ZipChatApi.INSTANCE.auth(accessToken, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                try {
                    JSONObject respJson = new JSONObject(Utils.responseToString(response));
                    String authToken = respJson.getString("authToken");
                    UserUtils.storeAuthToken(getActivity(), authToken);
                } catch (JSONException e) {
                    Log.e(TAG, "Problem parsing the auth json response");
                    return;
                }

                continueToApp();
            }

            @Override
            public void failure(RetrofitError error) {
                Utils.logErrorResponse(TAG, "Sending fb access token", error);
            }
        });
    }

    private void continueToApp() {
        Intent homeScreen = new Intent(getActivity(), HomeActivity.class);
        startActivity(homeScreen);
        // Do not remove
        getActivity().finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
        if (UserUtils.didCreateUser(getActivity())) {
            authUser(loginResult.getAccessToken().getToken());
        } else {
            Log.i(TAG, "Creating user");

            ZipChatApi.INSTANCE.createUser(loginResult.getAccessToken().getToken(), null, "android", new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    try {
                        JSONObject respJson = new JSONObject(Utils.responseToString(response));
                        long userId = respJson.getLong("userId");
                        String authToken = respJson.getString("authToken");
                        String fbId = respJson.getString("facebookId");
                        String fbName = respJson.getString("name");

                        Log.i(TAG, "onCreate userId: " + userId);
                        Log.i(TAG, "OnCreate auth: " + authToken);
                        Log.i(TAG, "OnCreate fbId: " + fbId);
                        Log.i(TAG, "OnCreate fbName: " + fbName);

                        Activity activity = getActivity();
                        UserUtils.saveId(activity, userId);
                        UserUtils.storeAuthToken(activity, authToken);
                        FacebookUtils.saveFacebookInformation(activity, fbName, fbId);
                    } catch (JSONException e) {
                        Log.e(TAG, "Problem parsing the createUser json response " + e.getMessage());
                        return;
                    }

                    continueToApp();
                }

                @Override
                public void failure(RetrofitError error) {
                    Utils.logErrorResponse(TAG, "Creating a user", error);
                }
            });
        }


    }

    @Override
    public void onCancel() {
        Log.w(TAG, "onCancel called in facebook login");
    }

    @Override
    public void onError(FacebookException e) {
        Log.e(TAG, "Error logging into facebook " + e.getMessage());
    }
}
