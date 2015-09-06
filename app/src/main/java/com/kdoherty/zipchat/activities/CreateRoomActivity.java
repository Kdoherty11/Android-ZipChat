package com.kdoherty.zipchat.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.RoomCreatedEvent;
import com.kdoherty.zipchat.fragments.PublicRoomsFragment;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.LocationManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class CreateRoomActivity extends AbstractLocationActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    private static final String TAG = CreateRoomActivity.class.getSimpleName();
    private static final int MAX_ROOM_NAME_CHARS = 28;
    private static final int DEFAULT_RADIUS = 250;

    private EditText mRoomNameEt;
    private boolean mDisableButton = false;

    // Name of the room that was created before we had a location
    private String waitListRoom;

    private Location mLocation;
    private GoogleMap mGoogleMap;
    private Circle mMapCircle;
    private Marker mMarker;
    private int mRadius = DEFAULT_RADIUS;
    private SeekBar mRadiusSeekBar;

    private boolean mMapLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.create_room_app_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        }

        mRadiusSeekBar = (SeekBar) findViewById(R.id.radius_seek_bar);
        mRadiusSeekBar.setProgress(DEFAULT_RADIUS);

        toolbar.findViewById(R.id.create_room_create_btn).setOnClickListener(this);

        mRoomNameEt = (EditText) findViewById(R.id.create_room_name_et);
        Utils.hideKeyboardOnEnter(this, mRoomNameEt);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.create_room_create_btn:
                if (mDisableButton) {
                    break;
                }
                mDisableButton = true;
                Utils.hideKeyboard(this, mRoomNameEt);
                final String roomName = mRoomNameEt.getText().toString().trim();
                final int roomNameLength = roomName.length();
                if (roomNameLength == 0) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_room_name_empty), Toast.LENGTH_SHORT).show();
                    mDisableButton = false;
                    break;
                }
                if (roomNameLength > MAX_ROOM_NAME_CHARS) {
                    Toast.makeText(getApplicationContext(), getString(R.string.toast_room_name_too_long), Toast.LENGTH_SHORT).show();
                    mDisableButton = false;
                    break;
                }
                if (NetworkManager.checkOnline(this)) {
                    new CreateRoomTask(roomName).execute();
                    finish();
                }

                break;
            default:
                Log.e(TAG, "Default case in onClick");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (waitListRoom != null) {
            Log.d(TAG, "Creating room using waitListRoom");
            Utils.debugToast(this, "Creating room using waitListRoom");
            new CreateRoomTask(waitListRoom).execute();
        }

        if (mLocation != null) {
            showRoom();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mRadius = (progress * 2) + 50;
        showRoom();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMapLoadedCallback(this);
    }

    @Override
    public void onMapLoaded() {
        mRadiusSeekBar.setOnSeekBarChangeListener(this);
        mMapLoaded = true;
        showRoom();
    }

    private void addToWaitList(String roomName) {
        waitListRoom = roomName;
    }

    private void showRoom() {
        if (mLocation == null) {
            return;
        }

        if (!mMapLoaded) {
            return;
        }

        if (mMapCircle != null) {
            mMapCircle.remove();
        }

        if (mMarker != null) {
            mMarker.remove();
        }

        LatLng userLatLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());

        mMarker = mGoogleMap.addMarker(new MarkerOptions().position(userLatLng)
                .title(getResources().getString(R.string.my_location)));

        mMapCircle = LocationManager.setRoomCircle(this, mGoogleMap, userLatLng, mRadius);
    }

    private class CreateRoomTask extends AsyncTask<Void, Void, Location> {

        private String name;

        public CreateRoomTask(String name) {
            this.name = name;
        }

        @Override
        protected Location doInBackground(Void... params) {
            if (mLocation != null) {
                return mLocation;
            } else {
                if (mGoogleApiClient.isConnected()) {
                    // Try to get a location again
                    return getLastLocation();
                } else if (mGoogleApiClient.isConnecting()) {
                    addToWaitList(name);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Location location) {
            if (location == null) {
                Utils.debugToast(getApplicationContext(), "Could not find location... not creating room");
                Log.w(TAG, "Null location found in create room. Not creating room");
            } else if (NetworkManager.checkOnline(CreateRoomActivity.this)) {

                ZipChatApi.INSTANCE.createPublicRoom(UserManager.getAuthToken(CreateRoomActivity.this), name, mRadius, location.getLatitude(), location.getLongitude(), new Callback<Response>() {
                    @Override
                    public void success(Response response, Response response2) {
                        Log.i(TAG, "Successfully created chat room");
                        Intent refreshIntent = new Intent();
                        refreshIntent.setAction(PublicRoomsFragment.REFRESH_FEED_ACTION);
                        sendBroadcast(refreshIntent);
                        BusProvider.getInstance().post(new RoomCreatedEvent());
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        NetworkManager.handleErrorResponse(TAG, "Creating a chat room", error, CreateRoomActivity.this);
                    }
                });
            }

        }
    }
}
