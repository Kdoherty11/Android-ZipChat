package com.kdoherty.zipchat.fragments;


import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.CreateRoomActivity;
import com.kdoherty.zipchat.activities.PublicRoomActivity;
import com.kdoherty.zipchat.activities.UserDetailsActivity;
import com.kdoherty.zipchat.adapters.PublicRoomDrawerAdapter;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.PrefsUtils;
import com.kdoherty.zipchat.utils.Utils;
import com.kdoherty.zipchat.views.RecyclerItemClickListener;

import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PublicRoomDrawerFragment extends Fragment implements GoogleMap.OnMapLoadedCallback{

    private static final String TAG = PublicRoomDrawerFragment.class.getSimpleName();

    private static final String PREFS_FILE_NAME = "chat_room_shared_preferences";
    public static final String KEY_USER_LEARNED_DRAWER = "chat_room_user_learned_drawer";

    private DrawerLayout mDrawerLayout;
    private View mContainerView;

    private boolean mUserLearnedDrawer;

    private GoogleMap mGoogleMap;
    private Marker mUserMarker;
    private Marker mMapMarker;

    private RecyclerView mRoomMembersRv;
    PublicRoomDrawerAdapter mRoomMembersAdapter;

    private boolean mMapLoaded = false;
    private int mRadius;
    private double mLat;
    private double mLon;
    private boolean isMapSetup;
    private String mRoomName;

    public PublicRoomDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserLearnedDrawer = PrefsUtils.readFromPreferences(getActivity(),
                PREFS_FILE_NAME, KEY_USER_LEARNED_DRAWER, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_room_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRoomMembersRv = (RecyclerView) view.findViewById(R.id.chat_room_drawer_list);
        mGoogleMap = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMap();
        mGoogleMap.setOnMapLoadedCallback(this);
        mRoomMembersRv.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Intent intent = UserDetailsActivity.getIntent(getActivity(), mRoomMembersAdapter.getUser(position));
                        startActivity(intent);
                    }
                })
        );
        mRoomMembersRv.setLayoutManager(new LinearLayoutManager(getActivity()));
    }


    public void toggleDrawer() {
        if (mDrawerLayout.isDrawerOpen(Gravity.END)) {
            mDrawerLayout.closeDrawer(Gravity.END);
        } else {
            mDrawerLayout.openDrawer(mContainerView);
        }
    }

    public void setupRoomMembers(List<User> roomMembers) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            mRoomMembersAdapter = new PublicRoomDrawerAdapter(activity, roomMembers);
            mRoomMembersRv.setAdapter(mRoomMembersAdapter);
        }
    }



    public void setUpMap(String roomName, int radius, double latitude, double longitude) {
        if (!mMapLoaded) {
            mRoomName = roomName;
            mRadius = radius;
            mLat = latitude;
            mLon = longitude;
            return;
        }

        if (radius == PublicRoomActivity.DEFAULT_ROOM_RADIUS
                && latitude == PublicRoomActivity.DEFAULT_ROOM_LATITUDE
                && longitude == PublicRoomActivity.DEFAULT_ROOM_LONGITUDE) {
            return;
        }

        if (mMapMarker != null) {
            mMapMarker.remove();
        }

        LatLng roomCenter = new LatLng(latitude, longitude);
        mMapMarker = mGoogleMap.addMarker(new MarkerOptions().position(roomCenter)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title(roomName));

        Utils.setRoomCircle(getActivity(), mGoogleMap, roomCenter, radius);
        isMapSetup = true;
    }

    public void setUp(DrawerLayout drawerLayout, final Toolbar toolbar, int drawerFragmentId) {
        mContainerView = getActivity().findViewById(drawerFragmentId);

        mDrawerLayout = drawerLayout;

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (slideOffset < 0.6) {
                    toolbar.setAlpha(1 - slideOffset);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if (!mUserLearnedDrawer) {
                    mUserLearnedDrawer = true;
                    PrefsUtils.saveToPreferences(getActivity(), PREFS_FILE_NAME,
                            KEY_USER_LEARNED_DRAWER, true);
                }
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    public void addUserMarker(Location location) {
        if (location != null) {
            if (mUserMarker != null) {
                mUserMarker.remove();
            }
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mUserMarker = mGoogleMap.addMarker(new MarkerOptions().position(userLocation)
                    .title("My Location"));
        }
    }

    public void addRoomMember(User user) {
        if (mRoomMembersAdapter != null) {
            mRoomMembersAdapter.addUser(user);
        } else if (getActivity() != null) {
            mRoomMembersAdapter = new PublicRoomDrawerAdapter(getActivity(), Collections.singletonList(user));
        }
    }

    public void removeRoomMember(User user) {
        if (mRoomMembersAdapter != null) {
            mRoomMembersAdapter.removeByUserId(user.getUserId());
        }
    }


    @Override
    public void onMapLoaded() {
        mMapLoaded = true;
        if (!isMapSetup) {
            setUpMap(mRoomName, mRadius, mLat, mLon);
        }
    }
}
