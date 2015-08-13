package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.Message;

/**
 * Created by kdoherty on 8/13/15.
 */
public class TalkReceivedEvent {

    private Message message;

    public TalkReceivedEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
