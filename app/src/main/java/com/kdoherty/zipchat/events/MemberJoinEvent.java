package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

/**
 * Created by kevin on 5/4/15.
 */
public class MemberJoinEvent {

    private User user;

    public MemberJoinEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
