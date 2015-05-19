package com.kdoherty.zipchat.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.adapters.HomeTabsAdapter;
import com.kdoherty.zipchat.events.TabChangeEvent;
import com.kdoherty.zipchat.services.BusProvider;
import com.kdoherty.zipchat.utils.PrefsUtils;
import com.kdoherty.zipchat.views.SlidingTabLayout;

public class HomeTabsFragment extends Fragment implements ViewPager.OnPageChangeListener {

    public static final String TAG = HomeTabsFragment.class.getSimpleName();

    public static final String PREFS_TAB_POSITION = "tabPositionKey";
    public static final String PREFS_FILE_NAME = "HomeScreenTabsFragmentPrefs";

    private SlidingTabLayout mSlidingTabLayout;
    private ViewPager mViewPager;
    private HomeTabsAdapter mAdapter;
    private String[] mTabTitles;
    private int mTabPosition;

    public static final int PRIVATE_ROOMS_TAB_INDEX = 1;
    public static final int REQUESTS_TAB_INDEX = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTabTitles = getResources().getStringArray(R.array.home_tab_titles);

        // Clear tab preferences
        boolean removed = PrefsUtils.removeFromPreferences(getActivity(), PREFS_FILE_NAME, PREFS_TAB_POSITION);
        Log.d(TAG, "Cleared stored tab position: " + removed);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home_screen_tabs, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mViewPager = (ViewPager) view.findViewById(R.id.home_screen_pager);
        mAdapter = new HomeTabsAdapter(getFragmentManager(), getActivity());

        mViewPager.setAdapter(mAdapter);
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.home_screen_tabs);
        mSlidingTabLayout.setCustomTabView(R.layout.home_screen_tab, 0);
        mSlidingTabLayout.setOnPageChangeListener(this);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.home_tab_indicator);
            }

            @Override
            public int getDividerColor(int position) {
                return 0;
            }
        });
        mSlidingTabLayout.setTabTextColor(mTabPosition, getResources().getColor(R.color.zipchat_blue));
    }

    public void goToTab(int tabIndex) {
        Log.d(TAG, "Going to tab: " + tabIndex);
        mTabPosition = tabIndex;
        mViewPager.setCurrentItem(mTabPosition);
        PrefsUtils.saveToPreferences(getActivity(), PREFS_FILE_NAME, PREFS_TAB_POSITION, mTabPosition);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        Resources res = getResources();
        mSlidingTabLayout.setActiveTabTextColor(position, mTabPosition,
                res.getColor(R.color.zipchat_blue),
                res.getColor(R.color.home_tab_unselected_text));

        mTabPosition = position;
        BusProvider.getInstance().post(new TabChangeEvent(getTabTitle()));
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
        mTabPosition = PrefsUtils.readFromPreferences(getActivity(), PREFS_FILE_NAME, PREFS_TAB_POSITION, 0);
        Log.d(TAG, "Using stored tab position: " + mTabPosition);
        mViewPager.setCurrentItem(mTabPosition);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Storing tab position: " + mTabPosition);
        PrefsUtils.saveToPreferences(getActivity(), PREFS_FILE_NAME, PREFS_TAB_POSITION, mTabPosition);
        BusProvider.getInstance().unregister(this);
    }

    public String getTabTitle() {
        return mTabTitles[mTabPosition];
    }

    public Filter getFilter() {
        return mAdapter.getFilter(mTabPosition);
    }
}
