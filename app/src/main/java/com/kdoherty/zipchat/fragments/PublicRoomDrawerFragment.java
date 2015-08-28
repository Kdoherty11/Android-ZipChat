package com.kdoherty.zipchat.fragments;


import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.UserAdapter;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.LocationManager;
import com.kdoherty.zipchat.utils.PrefsHelper;
import com.kdoherty.zipchat.utils.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PublicRoomDrawerFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {

    private static final String TAG = PublicRoomDrawerFragment.class.getSimpleName();

    private static final String PREFS_FILE_NAME = "chat_room_shared_preferences";
    public static final String KEY_USER_LEARNED_DRAWER = "chat_room_user_learned_drawer";

    private DrawerLayout mDrawerLayout;
    private View mContainerView;

    private boolean mUserLearnedDrawer;

    private GoogleMap mGoogleMap;
    private Marker mUserMarker;
    private Marker mRoomCenterMarker;

    private RecyclerView mRoomMembersRv;
    private UserAdapter mRoomMembersAdapter;

    private int mRoomRadius;
    private LatLng mRoomCenter;

    private String mRoomName;

    private static final String ARG_ROOM_NAME = "RoomName";
    private static final String ARG_ROOM_CENTER = "RoomCenterLocation";
    private static final String ARG_ROOM_RADIUS = "RoomRadius";

    public static PublicRoomDrawerFragment newInstance(String roomName, LatLng roomCenter, int roomRadius) {
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_NAME, roomName);
        args.putParcelable(ARG_ROOM_CENTER, roomCenter);
        args.putInt(ARG_ROOM_RADIUS, roomRadius);

        PublicRoomDrawerFragment fragment = new PublicRoomDrawerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public PublicRoomDrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLearnedDrawer = PrefsHelper.readFromPreferences(getActivity(),
                PREFS_FILE_NAME, KEY_USER_LEARNED_DRAWER, false);

        Bundle args = getArguments();
        mRoomName = args.getString(ARG_ROOM_NAME);
        mRoomCenter = args.getParcelable(ARG_ROOM_CENTER);
        mRoomRadius = args.getInt(ARG_ROOM_RADIUS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_room_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mRoomMembersRv = (RecyclerView) view.findViewById(R.id.chat_room_drawer_list);
        mRoomMembersRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
    }


    public void toggleDrawer() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            mDrawerLayout.openDrawer(mContainerView);
        }
    }

    public void setupRoomMembers(List<User> roomMembers) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            roomMembers.add(UserManager.getSelf(activity));
            mRoomMembersAdapter = new UserAdapter(activity, R.layout.cell_user, roomMembers);
            mRoomMembersRv.setAdapter(mRoomMembersAdapter);
        }
    }

    private void displayRoom() {
        if (mRoomCenterMarker != null) {
            mRoomCenterMarker.remove();
        }

        mRoomCenterMarker = mGoogleMap.addMarker(new MarkerOptions().position(mRoomCenter)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title(mRoomName));

        LocationManager.setRoomCircle(getActivity(), mGoogleMap, mRoomCenter, mRoomRadius);
    }

    public void setUp(final Activity context, DrawerLayout drawerLayout, final Toolbar toolbar, int drawerFragmentId) {
        mContainerView = context.findViewById(drawerFragmentId);

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
                    PrefsHelper.saveToPreferences(getActivity(), PREFS_FILE_NAME,
                            KEY_USER_LEARNED_DRAWER, true);
                }
                context.invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                context.invalidateOptionsMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    public void addUserMarker(Location location) {
        if (mUserMarker != null && location != null) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mUserMarker = mGoogleMap.addMarker(new MarkerOptions().position(userLocation)
                    .title(getResources().getString(R.string.drawer_user_location_pin_title)));
        }
    }

    public void addRoomMember(User user) {
        if (mRoomMembersAdapter != null) {
            mRoomMembersAdapter.addUser(user);
        } else if (getActivity() != null) {
            List<User> users = new ArrayList<>(Arrays.asList(UserManager.getSelf(getActivity()), user));
            mRoomMembersAdapter = new UserAdapter(getActivity(), R.layout.cell_user, users);
        }
    }

    public void removeRoomMember(User user) {
        if (mRoomMembersAdapter != null) {
            mRoomMembersAdapter.removeByUserId(user.getUserId());
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMapLoadedCallback(this);
    }

    @Override
    public void onMapLoaded() {
        displayRoom();
    }
}
