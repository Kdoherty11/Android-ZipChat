package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

/**
 * Created by kdoherty on 8/13/15.
 */
public class RemoveFavoriteEvent extends FavoriteEvent {

    public RemoveFavoriteEvent(User user, long messageId) {
        super(user, messageId);
    }
}
