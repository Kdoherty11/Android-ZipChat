package com.kdoherty.zipchat.events;


import android.widget.Filter;

/**
 * Created by kevindoherty on 2/7/15.
 */
public class FilterChangeEvent {

    private Filter mFilter;

    public FilterChangeEvent(Filter filter) {
        this.mFilter = filter;
    }

    public Filter getFilter() {
        return mFilter;
    }
}
