package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.Message;

/**
 * Created by kdoherty on 8/16/15.
 */
public class TalkConfirmationEvent {

    private String mUuid;
    private Message mMessage;

    public TalkConfirmationEvent(String uuid, Message message) {
        this.mUuid = uuid;
        this.mMessage = message;
    }

    public String getUuid() {
        return mUuid;
    }

    public Message getMessage() {
        return mMessage;
    }
}
