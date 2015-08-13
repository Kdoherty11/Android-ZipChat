package com.kdoherty.zipchat.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kdoherty on 12/18/14.
 */
public class User implements Parcelable {

    private long userId;
    private String name;
    private String facebookId;

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public User(String name, String facebookId, long userId) {
        this.name = name;
        this.facebookId = facebookId;
        this.userId = userId;
    }

    public User(String name, String facebookId) {
        this(name, facebookId, 0);
    }

    public User(Parcel in) {
        userId = in.readLong();
        name = in.readString();
        facebookId = in.readString();
    }

    public String getName() {
        return name;
    }
    public String getFacebookId() {
        return facebookId;
    }
    public long getUserId() {
        return userId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(userId);
        dest.writeString(name);
        dest.writeString(facebookId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (userId != user.userId) return false;
        if (name != null ? !name.equals(user.name) : user.name != null) return false;
        return !(facebookId != null ? !facebookId.equals(user.facebookId) : user.facebookId != null);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (facebookId != null ? facebookId.hashCode() : 0);
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", facebookId='" + facebookId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }
}
