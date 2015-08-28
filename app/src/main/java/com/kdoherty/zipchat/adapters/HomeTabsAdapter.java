package com.kdoherty.zipchat.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.widget.Filter;
import android.widget.Filterable;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.fragments.PrivateRoomsFragment;
import com.kdoherty.zipchat.fragments.PublicRoomsFragment;
import com.kdoherty.zipchat.fragments.RequestsFragment;

/**
 * Created by kdoherty on 12/26/14.
 */
public class HomeTabsAdapter extends FragmentPagerAdapter {

    private static final String TAG = HomeTabsAdapter.class.getSimpleName();

    private PublicRoomsFragment mPublicRoomsFragment = new PublicRoomsFragment();
    private final Fragment[] tabFragments = {mPublicRoomsFragment, new PrivateRoomsFragment(),
            new RequestsFragment()};
    private Resources mResources;
    private int[] imageResId = {
            R.drawable.ic_pin_drop_grey600_36dp,
            R.drawable.ic_chat_grey600_36dp,
            R.drawable.ic_people_grey600_36dp
    };

    public HomeTabsAdapter(FragmentManager fm, Context context) {
        super(fm);
        mResources = context.getResources();
    }

    @Override
    public int getCount() {
        return tabFragments.length;
    }

    @Override
    public Fragment getItem(int position) {
        return tabFragments[position];
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Drawable image = mResources.getDrawable(imageResId[position]);
        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
        SpannableString sb = new SpannableString(" ");
        ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    public Filter getFilter(int position) {
        Fragment fragment = getItem(position);
        if (fragment instanceof Filterable) {
            Filterable filterableFragment = (Filterable) fragment;
            return filterableFragment.getFilter();
        }
        return null;
    }
}
