package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.Message;

/**
 * Created by kdoherty on 8/13/15.
 */
public class TalkEvent {

    private Message message;

    public TalkEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
