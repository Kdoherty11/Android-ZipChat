package com.kdoherty.zipchat.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.RoomCreatedEvent;
import com.kdoherty.zipchat.fragments.PublicRoomsFragment;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.Utils;

import info.hoang8f.android.segmented.SegmentedGroup;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class CreateRoomActivity extends AbstractLocationActivity implements View.OnClickListener {

    private static final String TAG = CreateRoomActivity.class.getSimpleName();

    public static final float CIRCLE_STROKE_WIDTH = 6f;

    private String mRadius;
    private EditText mRoomNameEt;
    private boolean mDisableButton = false;

    private Location mLocation;

    // Name of the room that was created before we had a location
    private String waitListRoom;

    private GoogleMap mGoogleMap;
    private Circle mMapCircle;
    private Marker mMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();

        Toolbar toolbar = (Toolbar) findViewById(R.id.create_room_app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        SegmentedGroup radiusOptions = (SegmentedGroup) findViewById(R.id.create_room_radius_options);

        Resources resources = getResources();
        radiusOptions.setTintColor(resources.getColor(R.color.zipchat_blue),
                resources.getColor(R.color.white));

        mRadius = getString(R.string.create_room_small_option);

        findViewById(R.id.create_room_small_radius_option).setOnClickListener(this);
        findViewById(R.id.create_room_medium_radius_option).setOnClickListener(this);
        findViewById(R.id.create_room_large_radius_option).setOnClickListener(this);

        toolbar.findViewById(R.id.create_room_create_button).setOnClickListener(this);

        mRoomNameEt = (EditText) findViewById(R.id.create_room_name_et);
        Utils.hideKeyboardOnEnter(this, mRoomNameEt);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.create_room_small_radius_option:
            case R.id.create_room_medium_radius_option:
            case R.id.create_room_large_radius_option:
                String newRadius = ((RadioButton) view).getText().toString();
                if (!mRadius.equals(newRadius)) {
                    mRadius = newRadius;
                    addCircle(mLocation);
                }
                break;
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
            LatLng userLocation = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            if (mMarker != null) {
                mMarker.remove();
            }
            mMarker = mGoogleMap.addMarker(new MarkerOptions().position(userLocation)
                    .title("My Location"));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
            addCircle(userLocation);
        }
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

                ZipChatApi.INSTANCE.createPublicRoom(name, getRadius(), location.getLatitude(), location.getLongitude(), new Callback<Response>() {
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

    private void addCircle(Location location) {
        if (location == null) {
            return;
        }
        addCircle(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void addCircle(LatLng userLocation) {
        if (mMapCircle != null) {
            mMapCircle.remove();
        }

        CircleOptions circleOptions = new CircleOptions()
                .center(userLocation)
                .radius(getRadius())
                .fillColor(getResources().getColor(R.color.create_room_map_circle_fill))
                .strokeColor(getResources().getColor(R.color.zipchat_blue))
                .strokeWidth(CIRCLE_STROKE_WIDTH);

        mMapCircle = mGoogleMap.addCircle(circleOptions);
    }

    private int getRadius() {
        switch (mRadius) {
            case "Small":
                return PublicRoom.SMALL_RADIUS;
            case "Medium":
                return PublicRoom.MEDIUM_RADIUS;
            case "Large":
                return PublicRoom.LARGE_RADIUS;
            default:
                return -1;
        }
    }
}
