package com.kdoherty.zipchat.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.kdoherty.zipchat.utils.MyObjects;
import com.kdoherty.zipchat.utils.RuntimeTypeAdapterFactory;

import java.io.Serializable;
import java.util.List;

/**
 * Created by kdoherty on 9/2/15.
 */
@JsonAdapter(AbstractRoom.JsonAdapterFactory.class)
public abstract class AbstractRoom implements Parcelable {

    protected long roomId;
    protected long createdAt;
    protected long lastActivity;
    protected List<Message> messages;
    public RoomType type;

    AbstractRoom(RoomType type) {
        this.type = type;
    }

    protected AbstractRoom(RoomType roomType, long roomId) {
        this(roomType);
        this.roomId = roomId;
    }

    protected AbstractRoom(Parcel in) {
        this.roomId = in.readLong();
        this.createdAt = in.readLong();
        this.lastActivity = in.readLong();
        this.messages = in.createTypedArrayList(Message.CREATOR);
    }

    public static long getId(AbstractRoom room) {
        return room == null ? -1 : room.roomId;
    }

    public long getRoomId() {
        return roomId;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public final boolean isPublic() {
        return type == RoomType.PUBLIC;
    }

    public final boolean isPrivate() {
        return type == RoomType.PRIVATE;
    }

    public List<Message> getMessages() {
        return messages;
    }

    // http://www.artima.com/lejava/articles/equality.html
    public boolean canEqual(Object other) {
        return other instanceof AbstractRoom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AbstractRoom)) return false;
        AbstractRoom that = (AbstractRoom) other;
        return that.canEqual(this) &&
                MyObjects.equals(createdAt, that.createdAt) &&
                MyObjects.equals(lastActivity, that.lastActivity);
    }

    @Override
    public int hashCode() {
        return MyObjects.hash(createdAt, lastActivity);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.roomId);
        dest.writeLong(this.createdAt);
        dest.writeLong(this.lastActivity);
        dest.writeTypedList(messages);
    }

    public enum RoomType {
        PUBLIC, PRIVATE
    }

    final class JsonAdapterFactory extends RuntimeTypeAdapterFactory<AbstractRoom> {
        public JsonAdapterFactory() {
            super(AbstractRoom.class, "type");
            registerSubtype(PublicRoom.class, RoomType.PUBLIC.toString());
            registerSubtype(PrivateRoom.class, RoomType.PRIVATE.toString());
        }
    }
}
