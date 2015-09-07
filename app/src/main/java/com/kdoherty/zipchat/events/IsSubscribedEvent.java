package com.kdoherty.zipchat.events;

/**
 * Created by kevin on 5/12/15.
 */
public class IsSubscribedEvent {

    private boolean mIsSubscribed;

    public IsSubscribedEvent(boolean isSubscribed) {
        this.mIsSubscribed = isSubscribed;
    }

    public boolean isSubscribed() {
        return mIsSubscribed;
    }
}
