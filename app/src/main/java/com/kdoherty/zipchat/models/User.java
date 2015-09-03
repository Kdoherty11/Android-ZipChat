package com.kdoherty.zipchat.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.kdoherty.zipchat.utils.MyObjects;

/**
 * Created by kdoherty on 12/18/14.
 */
public class User implements Parcelable {

    public static final Creator<User> CREATOR = new Creator<User>() {
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };
    private long userId;
    private String facebookId;
    private String name;
    private String gender;

    public User(long userId, @Nullable String facebookId, String name) {
        this.userId = userId;
        this.facebookId = facebookId;
        this.name = name;
    }

    public User(User user) {
        this(user.getUserId(), user.getFacebookId(), user.getName());
    }

    protected User(Parcel in) {
        this.userId = in.readLong();
        this.facebookId = in.readString();
        this.name = in.readString();
        this.gender = in.readString();
    }

    public long getUserId() {
        return userId;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public String getName() {
        return name;
    }

    public boolean isAnon() {
        return facebookId == null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof User)) return false;
        User that = (User) other;
        return MyObjects.equals(userId, that.userId) &&
                MyObjects.equals(facebookId, that.facebookId) &&
                MyObjects.equals(name, that.name) &&
                MyObjects.equals(gender, that.gender);
    }

    @Override
    public int hashCode() {
        return MyObjects.hash(userId, facebookId, name, gender);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", facebookId='" + facebookId + '\'' +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.userId);
        dest.writeString(this.facebookId);
        dest.writeString(this.name);
        dest.writeString(this.gender);
    }
}