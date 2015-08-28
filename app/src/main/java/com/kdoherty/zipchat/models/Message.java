package com.kdoherty.zipchat.models;

import android.os.Parcel;
import android.os.Parcelable;

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

    private String uuid;

    private int score;

    private long createdAt = new Date().getTime() / 1000;
    private Date createdAtDate;

    private List<User> favorites = new ArrayList<>();

    private FavoriteState favoriteState;

    private boolean isConfirmed = true;

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

    public String getUuid() {
        return uuid;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void confirm() {
        this.isConfirmed = true;
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

    public Message(String message, User sender, String uuid) {
        this.messageId = -1;
        this.message = message;
        // TODO is this necessary
        this.sender = new User(sender);
        this.isConfirmed = false;
        this.uuid = uuid;
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


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Message{");
        sb.append("messageId=").append(messageId);
        sb.append(", message='").append(message).append('\'');
        sb.append(", sender=").append(sender);
        sb.append(", confirmed=").append(isConfirmed);
        sb.append(", uuid=").append(uuid);
        sb.append(", score=").append(score);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", createdAtDate=").append(createdAtDate);
        sb.append(", favorites=").append(favorites);
        sb.append(", favoriteState=").append(favoriteState);
        sb.append('}');
        return sb.toString();
    }
}