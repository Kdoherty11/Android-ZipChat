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
import android.util.Log;
import android.view.View;

import com.facebook.widget.ProfilePictureView;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.MemberJoinEvent;
import com.kdoherty.zipchat.events.MemberLeaveEvent;
import com.kdoherty.zipchat.events.ReceivedRoomMembersEvent;
import com.kdoherty.zipchat.fragments.ChatRoomFragment;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.utils.UserUtils;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by kevindoherty on 2/2/15.
 */
public class PrivateRoomActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = PrivateRoomActivity.class.getSimpleName();

    private ChatRoomFragment mChatRoomFragment;

    private static final String EXTRA_ROOM_ID = "PrivateChatActivityRoomId";
    private static final String EXTRA_USER_NAME = "PrivateChatActivityUserName";
    private static final String EXTRA_USER_FB_ID = "PrivateChatActivityUserFacebookId";

    private CircleImageView mActiveUserCircle;

    private long mRoomId;
    private String mOtherFbId;


    public static Intent getIntent(Context context, long roomId, String name, String facebookId) {
        Intent privateRoomIntent = new Intent(context, PrivateRoomActivity.class);
        privateRoomIntent.putExtra(EXTRA_ROOM_ID, roomId);
        privateRoomIntent.putExtra(EXTRA_USER_NAME, name);
        privateRoomIntent.putExtra(EXTRA_USER_FB_ID, facebookId);
        return privateRoomIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        String userName = intent.getStringExtra(EXTRA_USER_NAME);
        String facebookId = intent.getStringExtra(EXTRA_USER_FB_ID);

        mActiveUserCircle = (CircleImageView) findViewById(R.id.active_user_circle);
        Toolbar toolbar = (Toolbar) findViewById(R.id.private_chat_room_app_bar);
        ProfilePictureView otherMembersPic = (ProfilePictureView) toolbar.findViewById(R.id.other_user_pic);
        otherMembersPic.setProfileId(facebookId);
        otherMembersPic.setOnClickListener(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(userName);
        }

        if (savedInstanceState == null && mChatRoomFragment == null) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            long roomId = intent.getExtras().getLong(EXTRA_ROOM_ID);

            mChatRoomFragment = ChatRoomFragment.newInstance(roomId, false);

            fragmentTransaction.add(R.id.chat_room_fragment_container, mChatRoomFragment);
            fragmentTransaction.commit();
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onRoomMembersReceived(ReceivedRoomMembersEvent event) {
        User[] users = event.getUsers();
        if (users.length > 0) {
            mActiveUserCircle.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onUserJoinEvent(MemberJoinEvent event) {
        User joined = event.getUser();
        if (joined.getUserId() == UserUtils.getId(this)) {
            Log.e(TAG, "Received own join event");
            return;
        }
        mActiveUserCircle.setVisibility(View.VISIBLE);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onUserQuitEvent(MemberLeaveEvent event) {
        mActiveUserCircle.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.other_user_pic:
                // TODO
                break;
        }
    }
}
