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
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.widget.ProfilePictureView;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.UserUtils;
import com.kdoherty.zipchat.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = UserDetailsActivity.class.getSimpleName();

    private static final String RECEIVER_FB_ID_KEY = "ChatRequestActivityFacebookIdKey";
    private static final String RECEIVER_USER_ID_KEY = "ChatRequestActivityReceiverIdKey";
    private static final String RECEIVER_NAME_KEY = "ChatRequestActivityReceiverNameKey";

    private long mReceiverUserId;

    private Button mRequestButton;

    public static void startActivity(Context context, User user) {
        Intent requestIntent = new Intent(context, UserDetailsActivity.class);
        requestIntent.putExtra(RECEIVER_USER_ID_KEY, user.getUserId());
        requestIntent.putExtra(RECEIVER_NAME_KEY, user.getName());
        requestIntent.putExtra(RECEIVER_FB_ID_KEY, user.getFacebookId());

        context.startActivity(requestIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        mReceiverUserId = intent.getExtras().getLong(RECEIVER_USER_ID_KEY);
        final String profileId = intent.getStringExtra(RECEIVER_FB_ID_KEY);

        Toolbar toolbar = (Toolbar) findViewById(R.id.request_activity_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        actionBar.setTitle(intent.getStringExtra(RECEIVER_NAME_KEY));

        mRequestButton = (Button) findViewById(R.id.chat_request_button);
        mRequestButton.setOnClickListener(this);

        final ProfilePictureView profilePictureView = (ProfilePictureView) findViewById(R.id.chat_request_profile_picture);
        profilePictureView.setProfileId(profileId);

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

        ZipChatApi.INSTANCE.getStatus(UserUtils.getId(this), mReceiverUserId, new Callback<Response>() {
            @Override
            public void success(Response result, Response response) {
                String status = Utils.responseToString(result);
                if (!"\"none\"".equals(status)) {
                    mRequestButton.setText(status);
                    mRequestButton.setEnabled(false);
                }
                mRequestButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void failure(RetrofitError error) {
                Utils.logErrorResponse(TAG, "Getting request status", error);
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
        long userId = UserUtils.getId(this);
        Log.d(TAG, "Sending chat request to user " + mReceiverUserId);
        ZipChatApi.INSTANCE.sendChatRequest(userId, mReceiverUserId, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                Utils.logErrorResponse(TAG, "Sending a chat request", error);
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
