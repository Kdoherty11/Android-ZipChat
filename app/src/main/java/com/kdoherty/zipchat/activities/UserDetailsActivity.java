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
import android.widget.Toast;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.GsonProvider;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = UserDetailsActivity.class.getSimpleName();

    private static final String EXTRA_USER = "activities.UserDetailsActivity.extra.USER";
    private static final String EXTRA_ANON_USER_ID = "activities.UserDetailsActivity.extra.ANON_USER_ID";

    private Button mRequestButton;
    private ProgressBar mRequestStatusLoadingPb;

    private User mUser;

    public static Intent getIntent(Context context, User user) {
        Intent userDetailsIntent = new Intent(context, UserDetailsActivity.class);
        userDetailsIntent.putExtra(EXTRA_USER, user);
        return userDetailsIntent;
    }

    public static Intent getIntent(Context context, User user, long anonUserId) {
        Intent userDetailsIntent = getIntent(context, user);
        userDetailsIntent.putExtra(EXTRA_ANON_USER_ID, anonUserId);
        return userDetailsIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        mUser = intent.getParcelableExtra(EXTRA_USER);
        long mAnonUserId = intent.getLongExtra(EXTRA_ANON_USER_ID, 0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.user_details_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(mUser.getName());
        }

        mRequestButton = (Button) findViewById(R.id.chat_request_btn);
        mRequestButton.setOnClickListener(this);

        mRequestStatusLoadingPb = (ProgressBar) findViewById(R.id.request_status_loading_pb);

        boolean mIsSelf = mUser.getUserId() == UserManager.getId(this) || mUser.getUserId() == mAnonUserId;

        int width = 800;
        int height = mIsSelf ? 1200 : 1000;

        final ImageView profilePictureView = (ImageView) findViewById(R.id.chat_request_profile_picture);
        FacebookManager.displayProfilePicture(mUser.getFacebookId(), profilePictureView, width, height);

        if (!mIsSelf) {
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
                String statusCmpStr = status.trim().toLowerCase();

                if (statusCmpStr.equalsIgnoreCase("none") || statusCmpStr.equalsIgnoreCase("accepted")) {
                    mRequestButton.setText(getString(R.string.btn_text_send_request));
                    mRequestButton.setOnClickListener(UserDetailsActivity.this);
                } else if (statusCmpStr.equalsIgnoreCase("pending")) {
                    mRequestButton.setText(getString(R.string.btn_text_request_pending));
                    mRequestButton.setOnClickListener(null);
                } else if (statusCmpStr.equalsIgnoreCase("denied")) {
                    mRequestButton.setText(getString(R.string.btn_text_request_denied));
                    mRequestButton.setOnClickListener(null);
                } else {
                    final PrivateRoom existingRoom = GsonProvider.getInstance().fromJson(status, PrivateRoom.class);
                    mRequestButton.setText(getString(R.string.btn_text_already_chatting));
                    mRequestButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = PrivateRoomActivity.getIntent(UserDetailsActivity.this, existingRoom);
                            startActivity(intent);
                        }
                    });
                }

                stopLoading();
            }

            @Override
            public void failure(RetrofitError error) {
                mRequestStatusLoadingPb.setVisibility(View.GONE);
                Toast.makeText(UserDetailsActivity.this, getString(R.string.toast_no_internet), Toast.LENGTH_SHORT).show();
                NetworkManager.handleErrorResponse(TAG, "Getting request status", error, UserDetailsActivity.this);
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
                NetworkManager.handleErrorResponse(TAG, "Sending a chat request", error, UserDetailsActivity.this);
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case (R.id.chat_request_btn):
                sendChatRequest();
                break;
            default:
                throw new AssertionError("Default onClick case");
        }
    }
}
