package com.kdoherty.zipchat.events;

/**
 * Created by kevin on 5/12/15.
 */
public class IsSubscribedEvent {

    private boolean isSubscribed;

    public IsSubscribedEvent(boolean isSubscribed) {
        this.isSubscribed = isSubscribed;
    }

    public boolean isSubscribed() {
        return isSubscribed;
    }
}
