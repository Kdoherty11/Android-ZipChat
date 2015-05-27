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
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.RoomCreatedEvent;
import com.kdoherty.zipchat.fragments.PublicRoomsFragment;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.Utils;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class CreateRoomActivity extends AbstractLocationActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    private static final String TAG = CreateRoomActivity.class.getSimpleName();

    public static final float CIRCLE_STROKE_WIDTH = 6f;

    private EditText mRoomNameEt;
    private boolean mDisableButton = false;

    private Location mLocation;

    // Name of the room that was created before we had a location
    private String waitListRoom;

    private GoogleMap mGoogleMap;
    private Circle mMapCircle;
    private Marker mMarker;
    private int mRadius;

    private boolean mMapLoaded = false;

    private SeekBar mRadiusSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMapAsync(this);

        // Fixing Later Map loading Delay
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MapView mv = new MapView(getApplicationContext());
                    mv.onCreate(null);
                    mv.onPause();
                    mv.onDestroy();
                }catch (Exception ignored){

                }
            }
        }).start();

        mRadius = 50;

        Toolbar toolbar = (Toolbar) findViewById(R.id.create_room_app_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        }

        mRadiusSeekBar = (SeekBar) findViewById(R.id.radius_seek_bar);

        toolbar.findViewById(R.id.create_room_create_button).setOnClickListener(this);

        mRoomNameEt = (EditText) findViewById(R.id.create_room_name_et);
        Utils.hideKeyboardOnEnter(this, mRoomNameEt);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.create_room_create_button:
                if (mDisableButton) {
                    break;
                }
                mDisableButton = true;
                Utils.hideKeyboard(this, mRoomNameEt);
                final String roomName = mRoomNameEt.getText().toString().trim();
                if (!roomName.isEmpty()) {
                    if (Utils.checkOnline(this)) {
                        new CreateRoomTask(roomName).execute();
                        finish();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.create_room_no_name_toast), Toast.LENGTH_SHORT).show();
                    mDisableButton = false;
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
            Toast.makeText(getApplicationContext(), "Creating room using waitListRoom", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getApplicationContext(), "Could not find location", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Null location found in create room. Not creating room");
            } else if (Utils.checkOnline(CreateRoomActivity.this)) {

                ZipChatApi.INSTANCE.createPublicRoom(name, mRadius, location.getLatitude(), location.getLongitude(), new Callback<Response>() {
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
                        Utils.logErrorResponse(TAG, "Creating a chat room", error);
                    }
                });
            }

        }
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
                .title("My Location"));

        mMapCircle = Utils.setRoomCircle(this, mGoogleMap, userLatLng, mRadius);
    }
}
