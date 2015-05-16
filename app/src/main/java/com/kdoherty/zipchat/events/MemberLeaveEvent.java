package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

/**
 * Created by kevin on 5/4/15.
 */
public class MemberLeaveEvent {

    private User user;

    public MemberLeaveEvent(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
