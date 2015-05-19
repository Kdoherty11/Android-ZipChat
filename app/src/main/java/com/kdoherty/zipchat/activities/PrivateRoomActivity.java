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

import com.facebook.widget.ProfilePictureView;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.fragments.ChatRoomFragment;

/**
 * Created by kevindoherty on 2/2/15.
 */
public class PrivateRoomActivity extends AppCompatActivity {

    private static final String TAG = PrivateRoomActivity.class.getSimpleName();

    private ChatRoomFragment mChatRoomFragment;

    private static final String EXTRA_ROOM_ID = "PrivateChatActivityRoomId";
    private static final String EXTRA_USER_NAME = "PrivateChatActivityUserName";
    private static final String EXTRA_USER_FB_ID = "PrivateChatActivityUserFacebookId";

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.private_chat_room_app_bar);
        ProfilePictureView otherMembersPic = (ProfilePictureView) toolbar.findViewById(R.id.other_user_pic);
        otherMembersPic.setProfileId(facebookId);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        actionBar.setTitle(userName);

        if (savedInstanceState == null && mChatRoomFragment == null) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            long roomId = intent.getExtras().getLong(EXTRA_ROOM_ID);

            mChatRoomFragment = ChatRoomFragment.newInstance(roomId);

            fragmentTransaction.add(R.id.chat_room_fragment_container, mChatRoomFragment);
            fragmentTransaction.commit();
        }
    }
}
