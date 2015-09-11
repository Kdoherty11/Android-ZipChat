package com.kdoherty.zipchat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.MemberJoinEvent;
import com.kdoherty.zipchat.events.MemberLeaveEvent;
import com.kdoherty.zipchat.events.PublicRoomJoinEvent;
import com.kdoherty.zipchat.fragments.ChatRoomFragment;
import com.kdoherty.zipchat.fragments.PublicRoomDrawerFragment;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.squareup.otto.Subscribe;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class PublicRoomActivity extends AbstractLocationActivity {

    private static final String TAG = PublicRoomActivity.class.getSimpleName();

    private static final String EXTRA_ROOM = "PublicRoomActivityRoomExtra";

    private PublicRoomDrawerFragment mDrawerFragment;
    private ChatRoomFragment mChatRoomFragment;

    private PublicRoom mPublicRoom;

    private MenuItem mNotificationsToggle;
    private boolean mNotificationsOn;

    public static Intent getIntent(Context context, PublicRoom publicRoom) {
        Intent publicRoomIntent = new Intent(context, PublicRoomActivity.class);
        publicRoomIntent.putExtra(EXTRA_ROOM, publicRoom);
        return publicRoomIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        mPublicRoom = intent.getParcelableExtra(EXTRA_ROOM);

        Toolbar toolbar = (Toolbar) findViewById(R.id.chat_room_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(mPublicRoom.getName());
        }

        MapsInitializer.initialize(this);

        if (savedInstanceState == null) {

            if (mDrawerFragment == null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                int radius = mPublicRoom.getRadius();
                double latitude = mPublicRoom.getLatitude();
                double longitude = mPublicRoom.getLongitude();

                mDrawerFragment = PublicRoomDrawerFragment.newInstance(
                        mPublicRoom.getName(),
                        new LatLng(latitude, longitude),
                        radius);

                fragmentTransaction.add(R.id.chat_room_drawer_fragment_container, mDrawerFragment);
                fragmentTransaction.commit();

                DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.chat_room_drawer_layout);
                mDrawerFragment.setUp(this, drawerLayout, toolbar, R.id.chat_room_drawer_fragment_container);
            }

            if (mChatRoomFragment == null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                mChatRoomFragment = ChatRoomFragment.newInstance(mPublicRoom);

                fragmentTransaction.add(R.id.chat_room_fragment_container, mChatRoomFragment);
                fragmentTransaction.commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.public_room, menu);
        mNotificationsToggle = menu.findItem(R.id.action_toggle_notifications);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        setNotificationsIcon();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_toggle_notifications:
                if (mNotificationsOn) {
                    removeSubscription();
                } else {
                    subscribe();
                }
                mNotificationsOn = !mNotificationsOn;
                setNotificationsIcon();
                return true;
            case R.id.action_toggle_drawer:
                mDrawerFragment.toggleDrawer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setNotificationsIcon() {
        int iconId = mNotificationsOn ? R.drawable.ic_notifications_on_white_24dp : R.drawable.ic_notifications_white_24dp;
        if (mNotificationsToggle != null) {
            mNotificationsToggle.setIcon(iconId);
        } else {
            //Utils.debugToast(getApplicationContext(), "mNotificationsToggle is null in set notification icon");
            Log.e(TAG, "mNotificationsToggle is null in set notification icon");
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onJoinSuccess(PublicRoomJoinEvent event) {
        mNotificationsOn = event.isSubscribed();
        setNotificationsIcon();
        if (mDrawerFragment != null) {
            mDrawerFragment.setupRoomMembers(event.getRoomMembers());
        } else {
            Log.w(TAG, "mDrawerFragment is null during join success");
        }
    }

    private void subscribe() {
        if (!NetworkManager.checkOnline(this)) {
            return;
        }
        final long roomId = mPublicRoom.getRoomId();
        ZipChatApi.INSTANCE.subscribe(UserManager.getAuthToken(this), roomId, UserManager.getId(this), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.d(TAG, "Subscribed to room " + roomId);
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.handleErrorResponse(TAG, "Subscribing to room with id " + roomId, error, PublicRoomActivity.this);
            }
        });
    }

    private void removeSubscription() {
        if (!NetworkManager.checkOnline(this)) {
            return;
        }
        final long roomId = mPublicRoom.getRoomId();
        ZipChatApi.INSTANCE.removeSubscription(UserManager.getAuthToken(this), roomId, UserManager.getId(this), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.d(TAG, "Removed subscription from room " + roomId);
            }

            @Override
            public void failure(RetrofitError error) {
                NetworkManager.handleErrorResponse(TAG, "Removing subscription from room with id " + roomId, error, PublicRoomActivity.this);
            }
        });
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onUserJoinEvent(MemberJoinEvent event) {
        Log.d(TAG, "Received user join event");
        if (mDrawerFragment != null) {
            mDrawerFragment.addRoomMember(event.getUser());
        } else {
            Log.w(TAG, "mDrawerFragment is null when receiving user join event");
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onUserQuitEvent(MemberLeaveEvent event) {
        Log.d(TAG, "Received member quit event");
        if (mDrawerFragment != null) {
            mDrawerFragment.removeRoomMember(event.getUser());
        } else {
            Log.w(TAG, "mDrawerFragment is null when receiving user quit event");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);
        if (mDrawerFragment != null) {
            mDrawerFragment.displayUserMarker(getLastLocation());
        } else {
            Log.w(TAG, "mDrawerFragment is null when location is first available");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        if (mDrawerFragment != null) {
            mDrawerFragment.displayUserMarker(location);
        } else {
            Log.w(TAG, "mDrawerFragment is null when location changed");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MessageDetailsActivity.MessageDetailsResultHandler resultHandler =
                new MessageDetailsActivity.MessageDetailsResultHandler(mChatRoomFragment);
        resultHandler.onActivityResult(requestCode, resultCode, data);
    }
}