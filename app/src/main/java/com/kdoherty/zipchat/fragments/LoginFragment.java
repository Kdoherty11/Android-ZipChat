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
import android.widget.ProgressBar;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.HomeActivity;
import com.kdoherty.zipchat.services.RegistrationIntentService;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class LoginFragment extends Fragment implements FacebookCallback<LoginResult> {

    private static final String TAG = LoginFragment.class.getSimpleName();

    private CallbackManager mCallbackManager;
    private boolean mSentAuthRequest = false;
    private LoginButton mLoginButton;
    private ProgressBar mAuthPb;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mCallbackManager = CallbackManager.Factory.create();

        mLoginButton = (LoginButton) view.findViewById(R.id.login_button);
        mAuthPb = (ProgressBar) view.findViewById(R.id.auth_pb);

        if (mSentAuthRequest) {
            showLoading();
        } else {
            mLoginButton.setReadPermissions("user_friends");
            mLoginButton.setFragment(this);
            mLoginButton.registerCallback(mCallbackManager, this);
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        if (currentAccessToken != null && !TextUtils.isEmpty(currentAccessToken.getToken()) && !currentAccessToken.isExpired()) {
            authUser(currentAccessToken.getToken());
            mSentAuthRequest = true;
        }
    }

    private void authUser(String accessToken) {
        ZipChatApi.INSTANCE.auth(accessToken, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                try {
                    JSONObject respJson = new JSONObject(NetworkManager.responseToString(response));
                    String authToken = respJson.getString("authToken");
                    UserManager.storeAuthToken(getActivity(), authToken);
                    mAuthPb.setVisibility(View.GONE);
                } catch (JSONException e) {
                    Log.e(TAG, "Problem parsing the auth json response");
                    return;
                }

                continueToApp();
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.logErrorResponse(TAG, "Sending fb access token", error);
                mAuthPb.setVisibility(View.GONE);
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

    private void showLoading() {
        mLoginButton.setVisibility(View.GONE);
        mAuthPb.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
        if (UserManager.didCreateUser(getActivity()) && !mSentAuthRequest) {
            authUser(loginResult.getAccessToken().getToken());
        } else {
            Log.i(TAG, "Creating user");

            showLoading();

            ZipChatApi.INSTANCE.createUser(loginResult.getAccessToken().getToken(), null, "android", new Callback<Response>() {
                @Override
                public void success(Response response, Response response2) {
                    try {
                        JSONObject respJson = new JSONObject(NetworkManager.responseToString(response));
                        long userId = respJson.getLong("userId");
                        String authToken = respJson.getString("authToken");
                        String fbId = respJson.getString("facebookId");
                        String fbName = respJson.getString("name");

                        Log.i(TAG, "onCreate userId: " + userId);
                        Log.i(TAG, "OnCreate auth: " + authToken);
                        Log.i(TAG, "OnCreate fbId: " + fbId);
                        Log.i(TAG, "OnCreate fbName: " + fbName);

                        Activity activity = getActivity();
                        UserManager.storeId(activity, userId);
                        UserManager.storeAuthToken(activity, authToken);
                        FacebookManager.saveFacebookInformation(activity, fbName, fbId);
                    } catch (JSONException e) {
                        Log.e(TAG, "Parsing the createUser json response " + e);
                        return;
                    }

                    // Start service which will register the device
                    Intent intent = new Intent(getActivity(), RegistrationIntentService.class);
                    getActivity().startService(intent);

                    mAuthPb.setVisibility(View.GONE);

                    continueToApp();
                }

                @Override
                public void failure(RetrofitError error) {
                    NetworkManager.logErrorResponse(TAG, "Creating a user", error);
                    mLoginButton.setVisibility(View.VISIBLE);
                    mAuthPb.setVisibility(View.GONE);
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
