package com.kdoherty.zipchat.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by kdoherty on 12/15/14.
 */
public class Message implements Parcelable {

    private long messageId;
    private String message;
    private long timeStamp;

    private String senderName;
    private Date timeStampDate;
    private String facebookId;
    private long senderId;
    private User sender;
    private int score;

    private List<User> favorites = new ArrayList<>();
    private FavoriteState favoriteState;

    public enum FavoriteState {
        USER_FAVORITED, FAVORITED, UNFAVORITED
    }

    public Message(long messageId, long senderId, String senderName, String message, String facebookId, Date timeStampDate) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timeStampDate = new Date();
        this.facebookId = facebookId;
        this.timeStampDate = timeStampDate;
    }

    public Message(long messageId, long senderId, String senderName, String message, String facebookId) {
        this(messageId, senderId, senderName, message, facebookId, new Date());
    }

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimeStampDate() {
        return timeStampDate;
    }

    public void setTimeStampDate(Date timeStampDate) {
        this.timeStampDate = timeStampDate;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public long getMessageId() {
        return messageId;
    }

    public User getSender() {
        return sender;
    }

    public List<User> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<User> favorites) {
        this.favorites = favorites;
    }

    public int getFavoriteCount() {
        return favorites.size();
    }

    public void addFavorite(User user, long selfId) {
        favorites.add(user);
        score++;
        initFavoriteState(selfId);
    }

    public void removeFavorite(User user, long selfId) {
        favorites.remove(user);
        score--;
        initFavoriteState(selfId);
    }

    public void initFavoriteState(long userId) {
        if (favorites.isEmpty()) {
            favoriteState = FavoriteState.UNFAVORITED;
        } else {
            for (User user : favorites) {
                if (user.getUserId() == userId) {
                    favoriteState = FavoriteState.USER_FAVORITED;
                    return;
                }
            }
            // Favorited - but not by the user
            favoriteState = FavoriteState.FAVORITED;
        }
    }



    public FavoriteState getFavoriteState(long userId) {
        if (favoriteState == null) {
            initFavoriteState(userId);
        }

        return favoriteState;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Message{");
        sb.append("messageId=").append(messageId);
        sb.append(", message='").append(message).append('\'');
        sb.append(", timeStamp=").append(timeStamp);
        sb.append(", senderName='").append(senderName).append('\'');
        sb.append(", timeStampDate=").append(timeStampDate);
        sb.append(", facebookId='").append(facebookId).append('\'');
        sb.append(", senderId=").append(senderId);
        sb.append(", sender=").append(sender);
        sb.append(", favorites=").append(favorites);
        sb.append(", favoriteState=").append(favoriteState);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.messageId);
        dest.writeString(this.message);
        dest.writeLong(this.timeStamp);
        dest.writeString(this.senderName);
        dest.writeLong(timeStampDate != null ? timeStampDate.getTime() : -1);
        dest.writeString(this.facebookId);
        dest.writeLong(this.senderId);
        dest.writeParcelable(this.sender, 0);
        dest.writeInt(this.score);
        dest.writeTypedList(favorites);
        dest.writeInt(this.favoriteState == null ? -1 : this.favoriteState.ordinal());
    }

    private Message(Parcel in) {
        this.messageId = in.readLong();
        this.message = in.readString();
        this.timeStamp = in.readLong();
        this.senderName = in.readString();
        long tmpTimeStampDate = in.readLong();
        this.timeStampDate = tmpTimeStampDate == -1 ? null : new Date(tmpTimeStampDate);
        this.facebookId = in.readString();
        this.senderId = in.readLong();
        this.sender = in.readParcelable(User.class.getClassLoader());
        this.score = in.readInt();
        in.readTypedList(favorites, User.CREATOR);
        int tmpFavoriteState = in.readInt();
        this.favoriteState = tmpFavoriteState == -1 ? null : FavoriteState.values()[tmpFavoriteState];
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}
