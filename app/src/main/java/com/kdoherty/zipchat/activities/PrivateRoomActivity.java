package com.kdoherty.zipchat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.LeaveRoomEvent;
import com.kdoherty.zipchat.fragments.ChatRoomFragment;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kevindoherty on 2/2/15.
 */
public class PrivateRoomActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = PrivateRoomActivity.class.getSimpleName();

    private ChatRoomFragment mChatRoomFragment;

    private static final String EXTRA_ROOM_ID = "PrivateChatActivityRoomId";
    private static final String EXTRA_USER = "PrivateChatActivityUser";

    private User mUser;
    private long mRoomId;

    public static Intent getIntent(Context context, long roomId, User other) {
        Intent privateRoomIntent = new Intent(context, PrivateRoomActivity.class);
        privateRoomIntent.putExtra(EXTRA_ROOM_ID, roomId);
        privateRoomIntent.putExtra(EXTRA_USER, other);
        return privateRoomIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        mUser = intent.getParcelableExtra(EXTRA_USER);

        ZipChatApplication.initImageLoader(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.private_chat_room_app_bar);
        ImageView otherMembersPic = (ImageView) toolbar.findViewById(R.id.other_user_pic);
        FacebookManager.displayProfilePicture(mUser.getFacebookId(), otherMembersPic);
        otherMembersPic.setOnClickListener(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(mUser.getName());
        }

        if (savedInstanceState == null && mChatRoomFragment == null) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            mRoomId = intent.getExtras().getLong(EXTRA_ROOM_ID);

            mChatRoomFragment = ChatRoomFragment.newInstance(mRoomId, false);

            fragmentTransaction.add(R.id.chat_room_fragment_container, mChatRoomFragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_private_room, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_leave_room:
                leaveRoom();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void leaveRoom() {

        if (!NetworkManager.checkOnline(this)) {
            return;
        }

        ZipChatApi.INSTANCE.leaveRoom(UserManager.getAuthToken(this), mRoomId, UserManager.getId(this), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                BusProvider.getInstance().post(new LeaveRoomEvent());
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.logErrorResponse(TAG, "Leaving a private room", error);
            }
        });

        finish();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.other_user_pic:
                Intent userDetails = UserDetailsActivity.getIntent(this, mUser);
                startActivity(userDetails);
                break;
        }
    }
}
