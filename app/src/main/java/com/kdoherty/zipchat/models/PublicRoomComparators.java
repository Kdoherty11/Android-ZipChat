package com.kdoherty.zipchat.models;

import java.util.Comparator;

/**
 * Created by kdoherty on 12/16/14.
 */
public class PublicRoomComparators {

    private PublicRoomComparators() {
    }

    public enum DistanceComparator implements Comparator<PublicRoom> {

        ASCENDING;

        @Override
        public int compare(PublicRoom c1, PublicRoom c2) {

            final int c1Distance = c1.getDistance();
            final int c2Distance = c2.getDistance();

            if (c1Distance == c2Distance) {
                return ActivityComparator.DESCENDING.compare(c1, c2);
            }

            return c1Distance - c2Distance;
        }
    }

    public enum ActivityComparator implements Comparator<PublicRoom> {

        DESCENDING;

        @Override
        public int compare(PublicRoom c1, PublicRoom c2) {
            long c1LastActivity = c1.getLastActivity();
            long c2LastActivity = c2.getLastActivity();
            if (c1LastActivity < c2LastActivity) {
                return 1;
            } else if (c1LastActivity > c2LastActivity) {
                return -1;
            }
            return DistanceComparator.ASCENDING.compare(c1, c2);
        }
    }
}
