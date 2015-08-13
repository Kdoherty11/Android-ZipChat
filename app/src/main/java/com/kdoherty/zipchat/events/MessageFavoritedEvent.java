package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;

/**
 * Created by kdoherty on 8/13/15.
 */
public class MessageFavoritedEvent extends FavoriteEvent {

    public MessageFavoritedEvent(User user, long messageId) {
        super(user, messageId);
    }
}
