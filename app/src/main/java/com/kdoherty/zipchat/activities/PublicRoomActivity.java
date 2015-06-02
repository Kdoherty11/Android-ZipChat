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
import android.widget.Toast;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.IsSubscribedEvent;
import com.kdoherty.zipchat.events.MemberJoinEvent;
import com.kdoherty.zipchat.events.MemberLeaveEvent;
import com.kdoherty.zipchat.events.ReceivedRoomMembersEvent;
import com.kdoherty.zipchat.fragments.ChatRoomFragment;
import com.kdoherty.zipchat.fragments.PublicRoomDrawerFragment;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.UserUtils;
import com.kdoherty.zipchat.utils.Utils;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class PublicRoomActivity extends AbstractLocationActivity {

    private static final String TAG = PublicRoomActivity.class.getSimpleName();

    private static final String EXTRA_ROOM_NAME = "ChatRoomNameExtra";
    private static final String EXTRA_ROOM_ID = "ChatRoomIdExtra";

    private static final String EXTRA_ROOM_RADIUS = "ChatRoomDrawerFragmentArgRoomRadius";
    private static final String EXTRA_ROOM_LATITUDE = "ChatRoomDrawerFragmentArgRoomLatitude";
    private static final String EXTRA_ROOM_LONGITUDE = "ChatRoomDrawerFragmentArgRoomLongitude";

    public static final int DEFAULT_ROOM_RADIUS = -1;
    public static final double DEFAULT_ROOM_LATITUDE = -1;
    public static final double DEFAULT_ROOM_LONGITUDE = -1;

    private PublicRoomDrawerFragment mDrawerFragment;
    private ChatRoomFragment mChatRoomFragment;
    private long mRoomId;

    private MenuItem mNotificationsToggle;
    private boolean mNotificationsOn;

    public static Intent getIntent(Context context, long roomId, String name, double lat, double lon, int radius) {
        Intent publicRoomIntent = new Intent(context, PublicRoomActivity.class);
        publicRoomIntent.putExtra(EXTRA_ROOM_ID, roomId);
        publicRoomIntent.putExtra(EXTRA_ROOM_NAME, name);
        publicRoomIntent.putExtra(EXTRA_ROOM_LATITUDE, lat);
        publicRoomIntent.putExtra(EXTRA_ROOM_LONGITUDE, lon);
        publicRoomIntent.putExtra(EXTRA_ROOM_RADIUS, radius);
        return publicRoomIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        final Intent intent = getIntent();
        String roomName = intent.getStringExtra(EXTRA_ROOM_NAME);

        Toolbar toolbar = (Toolbar) findViewById(R.id.chat_room_app_bar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(roomName);
        }

        MapsInitializer.initialize(this);

        if (savedInstanceState == null) {

            if (mDrawerFragment == null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                int radius = intent.getIntExtra(EXTRA_ROOM_RADIUS, DEFAULT_ROOM_RADIUS);
                double latitude = intent.getDoubleExtra(EXTRA_ROOM_LATITUDE, DEFAULT_ROOM_LATITUDE);
                double longitude = intent.getDoubleExtra(EXTRA_ROOM_LONGITUDE, DEFAULT_ROOM_LONGITUDE);

                mDrawerFragment = PublicRoomDrawerFragment.newInstance(
                        roomName,
                        new LatLng(latitude, longitude),
                        radius);

                fragmentTransaction.add(R.id.chat_room_drawer_fragment_container, mDrawerFragment);
                fragmentTransaction.commit();

                DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.chat_room_drawer_layout);
                mDrawerFragment.setUp(this, drawerLayout, toolbar, R.id.chat_room_drawer_fragment_container);

                mDrawerFragment.addUserMarker(getLastLocation());
            }

            if (mChatRoomFragment == null) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                mRoomId = intent.getExtras().getLong(EXTRA_ROOM_ID);
                mChatRoomFragment = ChatRoomFragment.newInstance(mRoomId, true);

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
        int iconId = mNotificationsOn ? R.drawable.ic_notifications_on_white_24dp: R.drawable.ic_notifications_white_24dp;
        if (mNotificationsToggle != null) {
            mNotificationsToggle.setIcon(iconId);
        } else {
            Toast.makeText(this, "mNotificationsToggle is null in set notification icon", Toast.LENGTH_LONG).show();
            Log.e(TAG, "mNotificationsToggle is null in set notification icon");
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onIsSubscribedEvent(IsSubscribedEvent event) {
        Log.d(TAG, "Is Subscribed event received: " + event.isSubscribed());
        mNotificationsOn = event.isSubscribed();
        setNotificationsIcon();
    }

    private void subscribe() {
        if (!Utils.checkOnline(this)) {
            return;
        }
        ZipChatApi.INSTANCE.subscribe(mRoomId, UserUtils.getId(this), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.d(TAG, "Subscribed to room " + mRoomId);
            }

            @Override
            public void failure(RetrofitError error) {
                Utils.logErrorResponse(TAG, "Subscribing to room with id " + mRoomId, error);
            }
        });
    }

    private void removeSubscription() {
        if (!Utils.checkOnline(this)) {
            return;
        }
        ZipChatApi.INSTANCE.removeSubscription(mRoomId, UserUtils.getId(this), new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.d(TAG, "Removed subscription from room " + mRoomId);
            }

            @Override
            public void failure(RetrofitError error) {
                Utils.logErrorResponse(TAG, "Removing subscription from room with id " + mRoomId, error);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location location = getLastLocation();
        if (mDrawerFragment != null) {
            mDrawerFragment.addUserMarker(location);
        } else {
            Log.e(TAG, "TODO Fix this. onConnected cb before mDrawerFragment is initialized");
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onUserJoinEvent(MemberJoinEvent event) {
        Log.d(TAG, "Received member join event");
        mDrawerFragment.addRoomMember(event.getUser());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onUserQuitEvent(MemberLeaveEvent event) {
        Log.d(TAG, "Received member quit event");
        mDrawerFragment.removeRoomMember(event.getUser());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onRoomMembersReceived(ReceivedRoomMembersEvent event) {
        User[] users = event.getUsers();
        Log.d(TAG, "Received room member event with " + users.length + " users");
        mDrawerFragment.setupRoomMembers(new ArrayList<>(Arrays.asList(users)));
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
}