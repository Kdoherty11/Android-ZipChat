package com.kdoherty.zipchat.models;

/**
 * Created by kdoherty on 12/14/14.
 */
public class Request {

    private long requestId;
    private User sender;
    private long timeStamp;

    public enum Status {
        accepted,
        denied,
        pending
    }

    public Request(User sender, long timeStamp) {
        this.sender = sender;
        this.timeStamp = timeStamp;
    }

    public User getSender() {
        return this.sender;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
}
