package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.Message;

/**
 * Created by kdoherty on 8/16/15.
 */
public class TalkConfirmationEvent {

    private String uuid;
    private Message message;

    public TalkConfirmationEvent(String uuid, Message message) {
        this.uuid = uuid;
        this.message = message;
    }

    public String getUuid() {
        return uuid;
    }

    public Message getMessage() {
        return message;
    }
}
