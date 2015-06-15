package com.kdoherty.zipchat.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.AbstractLocationActivity;
import com.kdoherty.zipchat.activities.CreateRoomActivity;
import com.kdoherty.zipchat.activities.PublicRoomActivity;
import com.kdoherty.zipchat.adapters.PublicRoomAdapter;
import com.kdoherty.zipchat.events.LocationAvailableEvent;
import com.kdoherty.zipchat.events.RoomCreatedEvent;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.SortingTabs;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.LocationManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserInfo;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.kdoherty.zipchat.views.QuickReturnRecyclerView;
import com.kdoherty.zipchat.views.RecyclerItemClickListener;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PublicRoomsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, Filterable, RecyclerItemClickListener.OnItemClickListener {

    private static final String TAG = PublicRoomsFragment.class.getSimpleName();

    public static final String REFRESH_FEED_ACTION = "ChatRoomsFragmentRefreshFeedAction";
    private static final String BUNDLE_RECYCLER_LAYOUT_KEY = "classname.recycler.layout";

    private PublicRoomAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mChatRoomsRv;
    private TabHost mTabHost;
    private TabWidget mSortingTabs;

    private Map<String, ImageView> mDotIndicatorMap = new HashMap<>();

    private String mSortByDistanceTab;
    private String mSortByActivityTab;
    private String mSortByVotesTab;

    private String mCurrentTabTitle;

    private AbstractLocationActivity mLocationCallback;

    private boolean mFirst = true;

    public PublicRoomsFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mLocationCallback = (AbstractLocationActivity) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must be an AbstractLocationActivity");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_public_rooms, container, false);

        mChatRoomsRv = (QuickReturnRecyclerView) rootView.findViewById(R.id.chat_rooms);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createRoom = new Intent(getActivity(), CreateRoomActivity.class);
                startActivity(createRoom);
            }
        });

        mChatRoomsRv.setItemAnimator(new DefaultItemAnimator());
        mChatRoomsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mChatRoomsRv.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.message_list_divider), true, true));
        mChatRoomsRv.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this));
        registerForContextMenu(mChatRoomsRv);

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.orange);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });

        mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        mSortingTabs = (TabWidget) view.findViewById(android.R.id.tabs);

        setupSortingTabs();
    }

    private void setRoomDistances(List<PublicRoom> publicRooms) {
        Location location = mLocationCallback.getLastLocation();
        if (location == null) {
            return;
        }
        for (PublicRoom publicRoom : publicRooms) {
            double latitude = publicRoom.getLatitude();
            double longitude = publicRoom.getLongitude();

            // TODO Get from server
            int distance = (int) LocationManager.getDistance(location.getLatitude(), location.getLongitude(),
                    latitude, longitude);
            publicRoom.setDistance(distance);
        }
    }

    public void populateList(List<PublicRoom> publicRooms) {
        setRoomDistances(publicRooms);

        mAdapter = new PublicRoomAdapter(getActivity(), publicRooms);
        mChatRoomsRv.setAdapter(mAdapter);

        //mChatRoomsRv.setReturningView(mQuickReturnView);

        mSwipeRefreshLayout.setRefreshing(false);

        sortRooms();
    }

    private void setupSortingTabs() {

        mSortByDistanceTab = getString(R.string.home_sort_by_distance_tab);
        mSortByActivityTab = getString(R.string.home_sort_by_activity_tab);
        mSortByVotesTab = getString(R.string.home_sort_by_votes_tab);

        mCurrentTabTitle = mSortByDistanceTab;

        final int tabChildrenCount = mSortingTabs.getChildCount();
        View currentView;
        for (int i = 0; i < tabChildrenCount; i++) {
            currentView = mSortingTabs.getChildAt(i);
            LinearLayout.LayoutParams currentLayout =
                    (LinearLayout.LayoutParams) currentView.getLayoutParams();
            currentLayout.setMargins(0, 10, 10, 0);
        }
        mSortingTabs.requestLayout();

        mTabHost.setup();
        setupTab(mTabHost, new TextView(getActivity()), mSortByDistanceTab);
        setupTab(mTabHost, new TextView(getActivity()), mSortByActivityTab);
        setupTab(mTabHost, new TextView(getActivity()), mSortByVotesTab);
        //mTabHost.getTabWidget().setShowDividers(TabWidget.SHOW_DIVIDER_MIDDLE);
        //mTabHost.getTabWidget().setDividerDrawable(R.drawable.home_screen_tabs_divider);

        mDotIndicatorMap.get(mCurrentTabTitle).setVisibility(View.VISIBLE);

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabTitle) {
                setCurrentTab(tabTitle);
                sortRooms();
            }
        });
    }

    private void sortRooms() {
        SortingTabs tab = SortingTabs.valueOf(mCurrentTabTitle.toUpperCase());
        if (mAdapter != null) {
            mAdapter.sortRooms(tab);
        }
    }

    private void setCurrentTab(final String tabTitle) {
        mDotIndicatorMap.get(mCurrentTabTitle).setVisibility(View.INVISIBLE);
        mCurrentTabTitle = tabTitle;
        mDotIndicatorMap.get(tabTitle).setVisibility(View.VISIBLE);
    }

    private void setupTab(final TabHost tabHost, final View view, final String tag) {
        View tabView = createTabView(tabHost.getContext(), tag);
        TabHost.TabSpec setContent = tabHost.newTabSpec(tag).setIndicator(tabView).setContent(new TabHost.TabContentFactory() {
            public View createTabContent(String tag) {
                return view;
            }
        });

        tabHost.addTab(setContent);
    }

    private View createTabView(final Context context, final String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.home_screen_tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        ImageView dotIndicator = (ImageView) view.findViewById(R.id.tab_indicator);
        // Store reference to dot indicator
        mDotIndicatorMap.put(text, dotIndicator);
        return view;
    }

    @Override
    public void onRefresh() {
        refreshFeed();
    }

    private void refreshFeed() {
        Log.d(TAG, "Refreshing public rooms");

        Location location = mLocationCallback.getLastLocation();

        if (location == null) {
            Log.w(TAG, "Null location when refreshing public rooms");
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (!NetworkManager.checkOnline(getActivity())) {
            return;
        }

        ZipChatApi.INSTANCE.getPublicRooms(UserInfo.getAuthToken(getActivity()), location.getLatitude(), location.getLongitude(), new Callback<List<PublicRoom>>() {
            @Override
            public void success(List<PublicRoom> publicRooms, Response response) {
                populateList(publicRooms);
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void failure(RetrofitError error) {
                mSwipeRefreshLayout.setRefreshing(false);
                if (mAdapter == null) {
                    mAdapter = new PublicRoomAdapter(getActivity(), new ArrayList<PublicRoom>());
                }
                NetworkManager.logErrorResponse(TAG, "Getting public rooms", error);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFirst) {
            mFirst = false;
        } else {
            Log.d(TAG, "Refreshing rooms in onResume");
            refreshFeed();
        }

        BusProvider.getInstance().register(this);

        final IntentFilter refreshIntentFilter = new IntentFilter();
        refreshIntentFilter.addAction(REFRESH_FEED_ACTION);
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            Log.d(TAG, "Restoring list position");
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT_KEY);
            mChatRoomsRv.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Saving list position");
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT_KEY, mChatRoomsRv.getLayoutManager().onSaveInstanceState());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onRoomCreated(RoomCreatedEvent event) {
        refreshFeed();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onLocationAvailable(LocationAvailableEvent event) {
        Log.d(TAG, "Location available event");
        Location location = mLocationCallback.getLastLocation();
        if (location != null) {
            Log.d(TAG, "Sending location with accuracy " + location.getAccuracy() + " to server in getPublicRooms request");

            if (!NetworkManager.checkOnline(getActivity())) {
                return;
            }

            ZipChatApi.INSTANCE.getPublicRooms(UserInfo.getAuthToken(getActivity()),
                    location.getLatitude(), location.getLongitude(), new Callback<List<PublicRoom>>() {
                @Override
                public void success(List<PublicRoom> publicRooms, Response response) {
                    populateList(publicRooms);
                }

                @Override
                public void failure(RetrofitError error) {
                    NetworkManager.logErrorResponse(TAG, "Getting public rooms", error);
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        } else {
            mAdapter = new PublicRoomAdapter(getActivity(), new ArrayList<PublicRoom>());
            Log.w(TAG, "Last location is null");
        }
    }

    @Override
    public Filter getFilter() {
        if (mAdapter != null) {
            return mAdapter.getFilter();
        }
        return null;
    }

    @Override
    public void onItemClick(View view, int position) {
        PublicRoom room = mAdapter.getPublicRoom(position);
        Intent publicRoomIntent = PublicRoomActivity.getIntent(getActivity(), room.getRoomId(), room.getName(),
                room.getLatitude(), room.getLongitude(), room.getRadius());
        startActivity(publicRoomIntent);
    }
}
