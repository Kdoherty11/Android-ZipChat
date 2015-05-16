package com.kdoherty.zipchat.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.Toast;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.FilterChangeEvent;
import com.kdoherty.zipchat.events.LocationAvailableEvent;
import com.kdoherty.zipchat.fragments.HomeTabsFragment;
import com.kdoherty.zipchat.services.BusProvider;
import com.squareup.otto.Subscribe;

public class HomeActivity extends AbstractLocationActivity implements SearchView.OnQueryTextListener, HomeTabsFragment.OnTabChangeListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    public static final String ACTION_OPEN_REQUESTS_TAB = "OpenRequestsTabAction";
    public static final String ACTION_OPEN_PRIVATE_ROOMS_TAB = "OpenOnPrivateRoomsTab";

    private HomeTabsFragment mTabsFragment;
    private SearchView mSearchView;

    private Toolbar mToolbar;
    private Filter mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (savedInstanceState == null) {
            mTabsFragment = (HomeTabsFragment) getSupportFragmentManager().findFragmentById(R.id.home_screen_tabs_fragment);
        } else {
            mTabsFragment = (HomeTabsFragment) getSupportFragmentManager().getFragment(
                    savedInstanceState, HomeTabsFragment.TAG);
        }

        mToolbar = (Toolbar) findViewById(R.id.home_screen_app_bar);
        mToolbar.setTitle(mTabsFragment.getTabTitle());
        setSupportActionBar(mToolbar);

        String action = getIntent().getAction();
        if (!TextUtils.isEmpty(action)) {
            if (ACTION_OPEN_REQUESTS_TAB.equals(action)) {
                mTabsFragment.goToTab(HomeTabsFragment.REQUESTS_TAB_INDEX);
            } else if (ACTION_OPEN_PRIVATE_ROOMS_TAB.equals(action)) {
                mTabsFragment.goToTab(HomeTabsFragment.PRIVATE_ROOMS_TAB_INDEX);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_screen, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        ImageView searchIcon = (ImageView) mSearchView.findViewById(R.id.search_button);
        searchIcon.setImageResource(R.drawable.ic_search_white_24dp);
        mSearchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        mSearchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        if (mFilter != null) {
            mFilter.filter(s.trim());
        } else {
            Log.d(TAG, "Filter was null");
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, HomeTabsFragment.TAG, mTabsFragment);
    }

    @Override
    public void onTabChanged(String title, Filter filter, int tabPosition) {
        mToolbar.setTitle(title);
        mFilter = filter;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onFilterChange(FilterChangeEvent event) {
        Log.d(TAG, "Received filter change event");
        mFilter = event.getFilter();
    }

    @Override
    public void onConnected(Bundle bundle) {
        BusProvider.getInstance().post(new LocationAvailableEvent());
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }
}
