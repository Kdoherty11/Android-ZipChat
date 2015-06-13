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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserInfo;
import com.kdoherty.zipchat.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = UserDetailsActivity.class.getSimpleName();

    private static final String EXTRA_RECEIVER_FB_ID = "UserDetailsActivityFacebookIdKey";
    private static final String EXTRA_RECEIVER_USER_ID = "UserDetailsActivityReceiverIdKey";
    private static final String EXTRA_RECEIVER_NAME = "UserDetailsActivityReceiverNameKey";
    private static final String EXTRA_IS_ANON = "UserDetailsActivityIsAnonKey";

    private long mReceiverUserId;
    private String mReceiverFbId;

    private Button mRequestButton;
    private String mReceiverName;

    public static Intent getIntent(Context context, User user) {
        Intent userDetailsIntent = new Intent(context, UserDetailsActivity.class);
        userDetailsIntent.putExtra(EXTRA_RECEIVER_USER_ID, user.getUserId());
        userDetailsIntent.putExtra(EXTRA_RECEIVER_NAME, user.getName());
        userDetailsIntent.putExtra(EXTRA_RECEIVER_FB_ID, user.getFacebookId());
        return userDetailsIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        mReceiverUserId = intent.getExtras().getLong(EXTRA_RECEIVER_USER_ID);
        mReceiverName = intent.getStringExtra(EXTRA_RECEIVER_NAME);
        mReceiverFbId = intent.getStringExtra(EXTRA_RECEIVER_FB_ID);

        Toolbar toolbar = (Toolbar) findViewById(R.id.request_activity_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(mReceiverName);
        }

        mRequestButton = (Button) findViewById(R.id.chat_request_button);
        mRequestButton.setOnClickListener(this);

        final ImageView profilePictureView = (ImageView) findViewById(R.id.chat_request_profile_picture);
        FacebookManager.displayProfilePicture(mReceiverFbId, profilePictureView, "large");

        setButtonText();

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

    private void setButtonText() {
        if (!NetworkManager.checkOnline(this)) {
            return;
        }

        ZipChatApi.INSTANCE.getStatus(UserInfo.getAuthToken(this), UserInfo.getId(this), mReceiverUserId, new Callback<Response>() {
            @Override
            public void success(Response result, Response response) {
                String status = NetworkManager.responseToString(result);

                final Long privateRoomId = Utils.tryParse(status);

                if (privateRoomId != null) {
                    mRequestButton.setText("Already chatting");
                    mRequestButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = PrivateRoomActivity.getIntent(UserDetailsActivity.this, privateRoomId, mReceiverName, mReceiverFbId);
                            startActivity(intent);
                        }
                    });
                } else if (!"none".equals(status)) {
                    mRequestButton.setText(status);
                    mRequestButton.setEnabled(false);
                }

                mRequestButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.logErrorResponse(TAG, "Getting request status", error);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat_request, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendChatRequest() {
        if (!NetworkManager.checkOnline(this)) {
            return;
        }
        Log.d(TAG, "Sending chat request to user " + mReceiverUserId);
        long userId = UserInfo.getId(this);
        ZipChatApi.INSTANCE.sendChatRequest(UserInfo.getAuthToken(this), userId, mReceiverUserId, new Callback<Response>() {
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
            case(R.id.chat_request_button):
                sendChatRequest();
                break;
            default:
                throw new AssertionError("Default onClick case");
        }
    }
}
