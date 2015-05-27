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

    private long senderId;
    private String senderName;
    private String senderFbId;

    private int score;
    private boolean isAnon;

    private long timeStamp;
    private Date timeStampDate;

    private List<User> favorites = new ArrayList<>();

    private FavoriteState favoriteState;

    public enum FavoriteState {
        USER_FAVORITED, FAVORITED, UNFAVORITED
    }

    public long getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public long getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderFbId() {
        return senderFbId;
    }

    public int getScore() {
        return score;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Date getTimeStampDate() {
        return timeStampDate;
    }

    public List<User> getFavorites() {
        return favorites;
    }

    public FavoriteState getFavoriteState() {
        return favoriteState;
    }

    public int getFavoriteCount() {
        return favorites.size();
    }

    public boolean isAnon() {
        return isAnon;
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
        dest.writeString(this.senderFbId);
        dest.writeLong(this.senderId);
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
        this.senderFbId = in.readString();
        this.senderId = in.readLong();
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
