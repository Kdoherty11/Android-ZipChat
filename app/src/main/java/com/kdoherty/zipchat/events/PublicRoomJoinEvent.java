package com.kdoherty.zipchat.events;

import com.kdoherty.zipchat.models.User;

import java.util.List;

/**
 * Created by kdoherty on 8/28/15.
 */
public class PublicRoomJoinEvent {

    private List<User> mRoomMembers;
    private User mAnonUser;
    private boolean mIsSubscribed;

    public PublicRoomJoinEvent(List<User> mRoomMembers, User mAnonUser, boolean mIsSubscribed) {
        this.mRoomMembers = mRoomMembers;
        this.mAnonUser = mAnonUser;
        this.mIsSubscribed = mIsSubscribed;
    }

    public List<User> getRoomMembers() {
        return mRoomMembers;
    }

    public User getAnonUser() {
        return mAnonUser;
    }

    public boolean isSubscribed() {
        return mIsSubscribed;
    }
}
