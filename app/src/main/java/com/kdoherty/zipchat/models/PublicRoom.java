package com.kdoherty.zipchat.models;

import java.util.List;
import java.util.Set;

/**
 * Created by kdoherty on 12/14/14.
 */
public class PublicRoom {

    public static final int SMALL_RADIUS = 100;
    public static final int MEDIUM_RADIUS = 200;
    public static final int LARGE_RADIUS = 300;

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

    private Set<Long> subscribers;

    public PublicRoom(String name, int position, long lastActivity, List<Message> messages) {
        this.name = name;
        this.lastActivity = lastActivity;
        this.position = position;
        this.messages = messages;
    }

    public PublicRoom(long roomId, String name, int radius, double latitude, double longitude, long timeStamp, long lastActivity) {
        this.name = name;
        this.lastActivity = lastActivity;
        this.roomId = roomId;
        this.radius = radius;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeStamp = timeStamp;
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

    public void addSubscriber(long id) {
        subscribers.add(id);
    }
}
