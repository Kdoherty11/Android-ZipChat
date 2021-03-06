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
import android.widget.Toast;

import com.etiennelawlor.quickreturn.library.enums.QuickReturnViewType;
import com.etiennelawlor.quickreturn.library.listeners.QuickReturnRecyclerViewOnScrollListener;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.AbstractLocationActivity;
import com.kdoherty.zipchat.activities.CreateRoomActivity;
import com.kdoherty.zipchat.adapters.PublicRoomAdapter;
import com.kdoherty.zipchat.events.DismissLocationDialogEvent;
import com.kdoherty.zipchat.events.LocationAvailableEvent;
import com.kdoherty.zipchat.events.RoomCreatedEvent;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.services.ZipChatApi;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.LocationManager;
import com.kdoherty.zipchat.utils.NetworkManager;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.views.DividerItemDecoration;
import com.melnykov.fab.FloatingActionButton;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class PublicRoomsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, Filterable {

    public static final String REFRESH_FEED_ACTION = "fragments.PublicRoomsFragment.action.REFRESH_FEED";
    private static final String TAG = PublicRoomsFragment.class.getSimpleName();
    private static final String BUNDLE_RECYCLER_LAYOUT_KEY = "fragments.PublicRoomsFragment.key.RECYCLER_STATE";

    private PublicRoomAdapter mAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mChatRoomsRv;
    private TabHost mTabHost;
    private TabWidget mSortingTabs;
    private TextView mNoRoomsInAreaTv;
    private FloatingActionButton mCreateRoomFab;

    private Map<String, ImageView> mDotIndicatorMap = new HashMap<>();

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

        mChatRoomsRv = (RecyclerView) rootView.findViewById(R.id.public_rooms_rv);
        mCreateRoomFab = (FloatingActionButton) rootView.findViewById(R.id.create_room_fab);
        mNoRoomsInAreaTv = (TextView) rootView.findViewById(R.id.no_rooms_in_area_tv);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mChatRoomsRv.setItemAnimator(new DefaultItemAnimator());
        mChatRoomsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        mChatRoomsRv.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.message_list_divider), true, true));

        mCreateRoomFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createRoom = new Intent(getActivity(), CreateRoomActivity.class);
                startActivity(createRoom);
            }
        });

        mCreateRoomFab.attachToRecyclerView(mChatRoomsRv);

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

            int distance = (int) LocationManager.getDistance(location.getLatitude(), location.getLongitude(),
                    latitude, longitude);
            publicRoom.setDistance(distance);
        }
    }

    public void populateList(List<PublicRoom> publicRooms) {
        if (getActivity() == null) {
            displayNoRoomsFoundMessage();
            return;
        }
        mSwipeRefreshLayout.setRefreshing(false);
        mAdapter = new PublicRoomAdapter(getActivity(), publicRooms);
        mChatRoomsRv.setAdapter(mAdapter);

        if (publicRooms.isEmpty()) {
            displayNoRoomsFoundMessage();
        } else {
            hideNoRoomsFoundMessage();
            setRoomDistances(publicRooms);
            sortRooms();
        }
    }

    private void displayEmptyList() {
        populateList(new ArrayList<PublicRoom>());
    }

    private void displayNoRoomsFoundMessage() {
        mNoRoomsInAreaTv.setVisibility(View.VISIBLE);
    }

    private void hideNoRoomsFoundMessage() {
        mNoRoomsInAreaTv.setVisibility(View.GONE);
    }

    private void setupSortingTabs() {

        String mSortByDistanceTab = getString(R.string.distance);
        String mSortByActivityTab = getString(R.string.activity);

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

        mDotIndicatorMap.get(mCurrentTabTitle).setVisibility(View.VISIBLE);

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabTitle) {
                setCurrentTab(tabTitle);
                sortRooms();
            }
        });

        int footerHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.sorting_tab_height);

        QuickReturnRecyclerViewOnScrollListener scrollListener = new QuickReturnRecyclerViewOnScrollListener.Builder(QuickReturnViewType.FOOTER)
                .footer(mSortingTabs)
                .minFooterTranslation(footerHeight)
                .build();

        mChatRoomsRv.addOnScrollListener(scrollListener);
    }

    private void sortRooms() {
        HomeTabsFragment.TabType tab = HomeTabsFragment.TabType.valueOf(mCurrentTabTitle.toUpperCase());
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
        if (getActivity() == null) {
            return;
        }

        Location location = mLocationCallback.getLastLocation();

        if (location == null || !NetworkManager.checkOnline(getActivity())) {
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }

        ZipChatApi.INSTANCE.getPublicRooms(UserManager.getAuthToken(getActivity()), location.getLatitude(), location.getLongitude(), new Callback<List<PublicRoom>>() {
            @Override
            public void success(List<PublicRoom> publicRooms, Response response) {
                populateList(publicRooms);
            }

            @Override
            public void failure(RetrofitError error) {
                displayEmptyList();
                NetworkManager.handleErrorResponse(TAG, "Getting public rooms", error, getActivity());
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
        refreshFeed();
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onLocationDialogDismissed(DismissLocationDialogEvent event) {
        displayEmptyList();
    }

    @Override
    public Filter getFilter() {
        if (mAdapter != null) {
            return mAdapter.getFilter();
        }
        return null;
    }
}
