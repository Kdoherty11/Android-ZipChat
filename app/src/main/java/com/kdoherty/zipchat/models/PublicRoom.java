package com.kdoherty.zipchat.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by kdoherty on 12/14/14.
 */
public class PublicRoom implements Parcelable {

    public static final Parcelable.Creator<PublicRoom> CREATOR = new Parcelable.Creator<PublicRoom>() {
        public PublicRoom createFromParcel(Parcel source) {
            return new PublicRoom(source);
        }

        public PublicRoom[] newArray(int size) {
            return new PublicRoom[size];
        }
    };
    private int position;
    private List<Message> messages;
    private double latitude;
    private double longitude;
    private int radius;
    private long timeStamp;
    private int distance = -1;
    private long roomId;
    private String name;
    private long lastActivity;

    public PublicRoom(long roomId, String name, int radius, double latitude, double longitude) {
        this.roomId = roomId;
        this.name = name;
        this.radius = radius;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected PublicRoom(Parcel in) {
        this.position = in.readInt();
        this.messages = in.createTypedArrayList(Message.CREATOR);
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.radius = in.readInt();
        this.timeStamp = in.readLong();
        this.distance = in.readInt();
        this.roomId = in.readLong();
        this.name = in.readString();
        this.lastActivity = in.readLong();
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getName() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.position);
        dest.writeTypedList(messages);
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeInt(this.radius);
        dest.writeLong(this.timeStamp);
        dest.writeInt(this.distance);
        dest.writeLong(this.roomId);
        dest.writeString(this.name);
        dest.writeLong(this.lastActivity);
    }
}
