package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.Message;

/**
 * Created by kdoherty on 8/13/15.
 */
public class TalkEvent {

    private Message mMessage;

    public TalkEvent(Message message) {
        this.mMessage = message;
    }

    public Message getMessage() {
        return mMessage;
    }
}
