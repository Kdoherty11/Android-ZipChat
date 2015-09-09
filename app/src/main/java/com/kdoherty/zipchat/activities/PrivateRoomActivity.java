package com.kdoherty.zipchat.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
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
import android.widget.Toast;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.LeaveRoomEvent;
import com.kdoherty.zipchat.fragments.ChatRoomFragment;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.BusProvider;
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
    private static final String EXTRA_ROOM = "activities.PrivateRoomActivity.extra.PRIVATE_ROOM";
    private ChatRoomFragment mChatRoomFragment;
    private PrivateRoom mPrivateRoom;
    private User mOtherUser;

    public static Intent getIntent(Context context, PrivateRoom privateRoom) {
        Intent privateRoomIntent = new Intent(context, PrivateRoomActivity.class);
        privateRoomIntent.putExtra(EXTRA_ROOM, privateRoom);
        return privateRoomIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        mPrivateRoom = intent.getParcelableExtra(EXTRA_ROOM);
        mOtherUser = mPrivateRoom.getAndSetOther(UserManager.getId(this));

        if (!mPrivateRoom.isOtherInRoom()) {
            Toast.makeText(this, mOtherUser.getName() + " " + getResources().getString(R.string.toast_user_left_room), Toast.LENGTH_SHORT).show();
        }

        ZipChatApplication.initImageLoader(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.private_chat_room_app_bar);
        ImageView otherMembersPic = (ImageView) toolbar.findViewById(R.id.other_user_pic_iv);
        FacebookManager.displayProfilePicture(mOtherUser.getFacebookId(), otherMembersPic, 300, 300);
        otherMembersPic.setOnClickListener(this);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(mOtherUser.getName());
        }

        if (savedInstanceState == null && mChatRoomFragment == null) {

            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            mChatRoomFragment = ChatRoomFragment.newInstance(mPrivateRoom);

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
                showLeaveRoomAreYouSurePrompt();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLeaveRoomAreYouSurePrompt() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        leaveRoom();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        Resources res = getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(res.getString(R.string.are_you_sure_leave_room_prompt)).setPositiveButton(res.getString(R.string.yes), dialogClickListener)
                .setNegativeButton(res.getString(R.string.no), dialogClickListener).show();
    }

    private void leaveRoom() {
        if (!NetworkManager.checkOnline(this)) {
            return;
        }

        ZipChatApi.INSTANCE.leaveRoom(UserManager.getAuthToken(this), mPrivateRoom.getRoomId(), UserManager.getId(this), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                BusProvider.getInstance().post(new LeaveRoomEvent());
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.handleErrorResponse(TAG, "Leaving a private room", error, PrivateRoomActivity.this);
            }
        });

        finish();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.other_user_pic_iv:
                Intent userDetails = UserDetailsActivity.getIntent(this, mOtherUser);
                startActivity(userDetails);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MessageDetailsActivity.MessageDetailsResultHandler resultHandler =
                new MessageDetailsActivity.MessageDetailsResultHandler(mChatRoomFragment);
        resultHandler.onActivityResult(requestCode, resultCode, data);
    }
}
