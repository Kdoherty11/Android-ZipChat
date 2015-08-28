package com.kdoherty.zipchat.models;

/**
 * Created by kdoherty on 12/14/14.
 */
public class Request {

    private long requestId;
    private User sender;
    private long createdAt;

    public User getSender() {
        return this.sender;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public enum Status {
        accepted,
        denied,
        pending
    }
}
