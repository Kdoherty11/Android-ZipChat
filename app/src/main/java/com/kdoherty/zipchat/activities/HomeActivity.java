package com.kdoherty.zipchat.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Filter;
import android.widget.ImageView;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.events.LocationAvailableEvent;
import com.kdoherty.zipchat.events.TabChangeEvent;
import com.kdoherty.zipchat.fragments.HomeTabsFragment;
import com.kdoherty.zipchat.services.BusProvider;
import com.squareup.otto.Subscribe;

public class HomeActivity extends AbstractLocationActivity implements SearchView.OnQueryTextListener {

    private static final String TAG = HomeActivity.class.getSimpleName();

    public static final String ACTION_OPEN_REQUESTS_TAB = "OpenRequestsTab";
    public static final String ACTION_OPEN_PRIVATE_ROOMS_TAB = "OpenOnPrivateRoomsTab";

    private HomeTabsFragment mTabsFragment;
    private SearchView mSearchView;

    private Toolbar mToolbar;

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
        Filter currentFilter = getFilter();
        if (currentFilter != null) {
            currentFilter.filter(s.trim());
        } else {
            Log.e(TAG, "Filter was null in onQueryTextChange");
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, HomeTabsFragment.TAG, mTabsFragment);
    }

    public Filter getFilter() {
        if (mTabsFragment != null) {
            return mTabsFragment.getFilter();
        }
        return null;
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onTabChangeEvent(TabChangeEvent event) {
        if (mToolbar != null) {
            mToolbar.setTitle(event.getTabTitle());
        } else {
            Log.e(TAG, "Toolbar was null in onTabChangeEvent");
        }
    }

}
