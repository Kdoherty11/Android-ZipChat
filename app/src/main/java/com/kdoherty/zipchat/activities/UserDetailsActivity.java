package com.kdoherty.zipchat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = UserDetailsActivity.class.getSimpleName();

    private static final String EXTRA_USER = "UserDetailsActivityUserKey";

    private Button mRequestButton;
    private ProgressBar mRequestStatusLoadingPb;

    private User mUser;

    public static Intent getIntent(Context context, User user) {
        Intent userDetailsIntent = new Intent(context, UserDetailsActivity.class);
        userDetailsIntent.putExtra(EXTRA_USER, user);
        return userDetailsIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        mUser = intent.getParcelableExtra(EXTRA_USER);

        Toolbar toolbar = (Toolbar) findViewById(R.id.user_details_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(mUser.getName());
        }

        mRequestButton = (Button) findViewById(R.id.chat_request_button);
        mRequestButton.setOnClickListener(this);

        mRequestStatusLoadingPb = (ProgressBar) findViewById(R.id.request_status_loading_pb);

        final ImageView profilePictureView = (ImageView) findViewById(R.id.chat_request_profile_picture);
        FacebookManager.displayProfilePicture(mUser.getFacebookId(), profilePictureView, "large");

        if (!mUser.equals(UserManager.getSelf(this))) {
            setButtonText();
        } else {
            mRequestButton.setVisibility(View.GONE);
            mRequestStatusLoadingPb.setVisibility(View.GONE);
        }

        profilePictureView.post(new Runnable() {

            @Override
            public void run() {
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int screenWidth = size.x;

                Log.d(TAG, "Screen width: " + screenWidth);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) profilePictureView.getLayoutParams();
                params.width = screenWidth;
                params.height = screenWidth;

                profilePictureView.setLayoutParams(params);
                profilePictureView.postInvalidate();
            }
        });

    }

    private void startLoading() {
        Utils.debugToast(this, "Starting loading");
        mRequestButton.setVisibility(View.GONE);
        mRequestStatusLoadingPb.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        mRequestStatusLoadingPb.setVisibility(View.GONE);
        mRequestButton.setVisibility(View.VISIBLE);
        Utils.debugToast(this, "Stopped loading");
    }

    private void setButtonText() {
        startLoading();
        if (!NetworkManager.checkOnline(this)) {
            stopLoading();
            return;
        }

        ZipChatApi.INSTANCE.getStatus(UserManager.getAuthToken(this), UserManager.getId(this), mUser.getUserId(), new Callback<Response>() {
            @Override
            public void success(Response result, Response response) {
                String status = NetworkManager.responseToString(result);

                final Long privateRoomId = Utils.tryParse(status);

                if (privateRoomId != null) {
                    mRequestButton.setText(getString(R.string.request_status_already_chatting));
                    mRequestButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = PrivateRoomActivity.getIntent(UserDetailsActivity.this, privateRoomId, mUser);
                            startActivity(intent);
                        }
                    });
                } else if (!"none".equals(status)) {
                    mRequestButton.setText(status);
                    mRequestButton.setEnabled(false);
                }

                stopLoading();
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.logErrorResponse(TAG, "Getting request status", error);
                stopLoading();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            Utils.debugToast(this, "Finishing UserDetailsActivity because home was clicked");
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendChatRequest() {
        if (!NetworkManager.checkOnline(this)) {
            return;
        }
        Log.d(TAG, "Sending chat request to user " + mUser.getUserId());
        long userId = UserManager.getId(this);
        ZipChatApi.INSTANCE.sendChatRequest(UserManager.getAuthToken(this), userId, mUser.getUserId(), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.logErrorResponse(TAG, "Sending a chat request", error);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.chat_request_button):
                sendChatRequest();
                break;
            default:
                throw new AssertionError("Default onClick case");
        }
    }
}
