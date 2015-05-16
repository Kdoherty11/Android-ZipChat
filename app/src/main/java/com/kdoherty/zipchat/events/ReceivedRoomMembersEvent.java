package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

/**
 * Created by kevin on 5/4/15.
 */
public class ReceivedRoomMembersEvent {

    private User[] users;

    public ReceivedRoomMembersEvent(User[] users) {
        this.users = users;
    }

    public User[] getUsers() {
        return users;
    }

}
