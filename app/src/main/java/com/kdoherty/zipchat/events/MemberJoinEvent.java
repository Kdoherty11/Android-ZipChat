package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

/**
 * Created by kevin on 5/4/15.
 */
public class MemberJoinEvent {

    private User mUser;

    public MemberJoinEvent(User user) {
        this.mUser = user;
    }

    public User getUser() {
        return mUser;
    }
}
