package com.kdoherty.zipchat.models;

import android.os.Parcel;

import com.kdoherty.zipchat.utils.MyObjects;

/**
 * Created by kdoherty on 12/14/14.
 */
public class PublicRoom extends AbstractRoom {

    public static final Creator<PublicRoom> CREATOR = new Creator<PublicRoom>() {
        public PublicRoom createFromParcel(Parcel source) {
            return new PublicRoom(source);
        }

        public PublicRoom[] newArray(int size) {
            return new PublicRoom[size];
        }
    };
    private String name;
    private double latitude;
    private double longitude;
    private int radius;
    private int position;
    private int distance;

    public PublicRoom() {
        super(RoomType.PUBLIC);
    }

    public PublicRoom(long roomId, String name, int radius, double latitude, double longitude) {
        super(RoomType.PUBLIC, roomId);
        this.name = name;
        this.radius = radius;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected PublicRoom(Parcel in) {
        super(in);
        this.name = in.readString();
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.radius = in.readInt();
        this.position = in.readInt();
        this.distance = in.readInt();
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getRadius() {
        return radius;
    }

    public int getPosition() {
        return position;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public boolean canEqual(Object other) {
        return other instanceof PublicRoom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PublicRoom)) return false;
        PublicRoom that = (PublicRoom) other;
        return that.canEqual(this) && super.equals(that) &&
                MyObjects.equals(name, that.name) &&
                MyObjects.equals(latitude, that.latitude) &&
                MyObjects.equals(longitude, that.longitude) &&
                MyObjects.equals(radius, that.radius);
    }

    @Override
    public int hashCode() {
        return MyObjects.hash(super.hashCode(), name, latitude, longitude, radius);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.name);
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeInt(this.radius);
        dest.writeInt(this.position);
        dest.writeInt(this.distance);
    }
}
