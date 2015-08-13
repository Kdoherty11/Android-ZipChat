package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

/**
 * Created by kdoherty on 8/13/15.
 */
abstract class FavoriteEvent {

    private User user;
    private long messageId;

    public FavoriteEvent(User user, long messageId) {
        this.user = user;
        this.messageId = messageId;
    }

    public User getUser() {
        return user;
    }

    public long getMessageId() {
        return messageId;
    }
}
