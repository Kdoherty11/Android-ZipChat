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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.kdoherty.zipchat.utils.Utils;

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
    private Button mRetryAuthBtn;
    private ProgressBar mAuthPb;
    private String mFacebookAccessToken = null;

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mCallbackManager = CallbackManager.Factory.create();

        mLoginButton = (LoginButton) view.findViewById(R.id.login_btn);
        mRetryAuthBtn = (Button) view.findViewById(R.id.retry_auth_btn);
        mRetryAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createOrAuthUser(mFacebookAccessToken);
            }
        });
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
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        if (currentAccessToken != null && !TextUtils.isEmpty(currentAccessToken.getToken()) && !currentAccessToken.isExpired()) {
            createOrAuthUser(currentAccessToken.getToken());
        } else {
            AccessToken.refreshCurrentAccessTokenAsync();
        }
    }

    private void authUser(String accessToken) {
        final Activity activity = getActivity();
        if (activity == null || !NetworkManager.checkOnline(activity)) {
            return;
        }
        Log.i(TAG, "Sending auth request");

        showLoading();
        mSentAuthRequest = true;
        ZipChatApi.INSTANCE.auth(accessToken, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                hideButtons();
                try {
                    JSONObject respJson = new JSONObject(NetworkManager.responseToString(response));
                    String authToken = respJson.getString("authToken");
                    UserManager.storeAuthToken(activity, authToken);
                } catch (JSONException e) {
                    Log.e(TAG, "Problem parsing the auth json response");
                    return;
                }

                continueToApp();
            }

            @Override
            public void failure(RetrofitError error) {
                showRetryLoginBtn();
                NetworkManager.handleErrorResponse(TAG, "Sending fb access token to zipchat /auth", error, activity);
                Toast.makeText(activity, getString(R.string.toast_login_failure),
                        Toast.LENGTH_SHORT).show();
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

    private void createOrAuthUser(String facebookToken) {
        if (TextUtils.isEmpty(facebookToken)) {
            showFacebookButton();
            Log.w(TAG, "Facebook token is null or empty in createOrAuthUser");
            return;
        }
        mFacebookAccessToken = facebookToken;
        if (UserManager.didCreateUser(getActivity())) {
            if (!mSentAuthRequest) {
                authUser(facebookToken);
            }
        } else {
            createUser(facebookToken);
        }
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
        Log.i(TAG, "Success logging into facebook");
        createOrAuthUser(loginResult.getAccessToken().getToken());
    }

    private void createUser(String accessToken) {
        if (!NetworkManager.checkOnline(getActivity())) {
            return;
        }
        Log.i(TAG, "Creating user");
        showLoading();
        ZipChatApi.INSTANCE.createUser(accessToken, null, "android", new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                hideButtons();
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

                continueToApp();
            }

            @Override
            public void failure(RetrofitError error) {
                showRetryLoginBtn();
                NetworkManager.handleErrorResponse(TAG, "Creating a user", error, getActivity());

                Toast.makeText(getActivity(), getString(R.string.toast_login_failure), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        Utils.nullSafeSetVisibility(mLoginButton, View.GONE);
        Utils.nullSafeSetVisibility(mRetryAuthBtn, View.GONE);
        Utils.nullSafeSetVisibility(mAuthPb, View.VISIBLE);
    }

    private void showFacebookButton() {
        Utils.nullSafeSetVisibility(mAuthPb, View.GONE);
        Utils.nullSafeSetVisibility(mRetryAuthBtn, View.GONE);
        Utils.nullSafeSetVisibility(mLoginButton, View.VISIBLE);
    }

    private void showRetryLoginBtn() {
        mSentAuthRequest = false;
        Utils.nullSafeSetVisibility(mAuthPb, View.GONE);
        Utils.nullSafeSetVisibility(mLoginButton, View.GONE);
        Utils.nullSafeSetVisibility(mRetryAuthBtn, View.VISIBLE);
    }

    private void hideButtons() {
        Utils.nullSafeSetVisibility(mAuthPb, View.GONE);
        Utils.nullSafeSetVisibility(mRetryAuthBtn, View.GONE);
        Utils.nullSafeSetVisibility(mLoginButton, View.GONE);
    }


    @Override
    public void onCancel() {
        Log.w(TAG, "onCancel called in facebook login");
    }

    @Override
    public void onError(FacebookException e) {
        Log.e(TAG, "Error logging into facebook " + e.getMessage());
        showFacebookButton();
        Toast.makeText(getActivity(), getString(R.string.toast_login_failure), Toast.LENGTH_SHORT).show();
    }
}
