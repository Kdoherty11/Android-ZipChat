package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

/**
 * Created by kdoherty on 8/13/15.
 */
public class AddFavoriteEvent extends FavoriteEvent {

    public AddFavoriteEvent(User user, long messageId) {
        super(user, messageId);
    }
}
