package com.kdoherty.zipchat.models;

import java.util.Comparator;

/**
 * Created by kevindoherty on 1/31/15.
 */
public enum PrivateRoomComparator implements Comparator<PrivateRoom> {

    INSTANCE;

    @Override
    public int compare(PrivateRoom lhs, PrivateRoom rhs) {
        long lhsLastActivity = lhs.getLastActivity();
        long rhsLastActivity = rhs.getLastActivity();
        if (lhsLastActivity < rhsLastActivity) {
            return 1;
        } else if (lhsLastActivity > rhsLastActivity) {
            return -1;
        }
        return 0;
    }
}
