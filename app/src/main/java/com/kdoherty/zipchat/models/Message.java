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
    private User sender;

    private int score;

    private long createdAt;
    private Date createdAtDate;

    private List<User> favorites = new ArrayList<>();

    private FavoriteState favoriteState;

    public enum FavoriteState {
        USER_FAVORITED, FAVORITED, UNFAVORITED
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

    public long getMessageId() {
        return messageId;
    }

    public String getMessage() {
        return message;
    }

    public User getSender() {
        return sender;
    }

    public int getScore() {
        return score;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Date getCreatedAtDate() {
        return createdAtDate;
    }

    public List<User> getFavorites() {
        return favorites;
    }

    public FavoriteState getFavoriteState() {
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
        dest.writeParcelable(this.sender, 0);
        dest.writeInt(this.score);
        dest.writeLong(this.createdAt);
        dest.writeLong(createdAtDate != null ? createdAtDate.getTime() : -1);
        dest.writeTypedList(favorites);
        dest.writeInt(this.favoriteState == null ? -1 : this.favoriteState.ordinal());
    }

    public Message() {
    }

    protected Message(Parcel in) {
        this.messageId = in.readLong();
        this.message = in.readString();
        this.sender = in.readParcelable(User.class.getClassLoader());
        this.score = in.readInt();
        this.createdAt = in.readLong();
        long tmpCreatedAtDate = in.readLong();
        this.createdAtDate = tmpCreatedAtDate == -1 ? null : new Date(tmpCreatedAtDate);
        this.favorites = in.createTypedArrayList(User.CREATOR);
        int tmpFavoriteState = in.readInt();
        this.favoriteState = tmpFavoriteState == -1 ? null : FavoriteState.values()[tmpFavoriteState];
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };
}
