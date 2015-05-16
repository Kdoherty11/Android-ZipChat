package com.kdoherty.zipchat.models;

import com.kdoherty.zipchat.utils.UserUtils;

/**
 * Created by kdoherty on 12/14/14.
 */
public class PrivateRoom {

    private long roomId;
    private User other;
    private long lastActivity;
    private User sender;
    private User receiver;

    public PrivateRoom(long roomId, User other, long lastActivity) {
        this.roomId = roomId;
        this.other = other;
        this.lastActivity = lastActivity;
    }

    public User getOther() {
        return other;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public long getId() {
        return roomId;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public User getAndSetOther(long userId) {
        if (userId == sender.getUserId()) {
            other = receiver;
        } else {
            other = sender;
        }
        return other;
    }

    @Override
    public String toString() {
        return "PrivateRoom{" +
                "roomId=" + roomId +
                ", other=" + other +
                ", lastActivity=" + lastActivity +
                ", sender=" + sender +
                ", receiver=" + receiver +
                '}';
    }
}
