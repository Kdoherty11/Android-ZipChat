package com.kdoherty.zipchat.models;

import android.os.Parcel;
import android.support.annotation.Nullable;

import com.kdoherty.zipchat.utils.MyObjects;
import com.kdoherty.zipchat.utils.UserManager;

/**
 * Created by kdoherty on 12/14/14.
 */
public class PrivateRoom extends AbstractRoom {

    public static final Creator<PrivateRoom> CREATOR = new Creator<PrivateRoom>() {
        public PrivateRoom createFromParcel(Parcel source) {
            return new PrivateRoom(source);
        }

        public PrivateRoom[] newArray(int size) {
            return new PrivateRoom[size];
        }
    };
    private User other;
    private User sender;
    private User receiver;
    private boolean senderInRoom;
    private boolean receiverInRoom;

    public PrivateRoom() {
        super(RoomType.PRIVATE);
    }

    public PrivateRoom(long roomId, User sender, User receiver) {
        super(RoomType.PRIVATE, roomId);
        this.sender = sender;
        this.receiver = receiver;
    }

    public PrivateRoom(long roomId, User other, long lastActivity) {
        super(RoomType.PRIVATE, roomId);
        this.other = other;
        this.lastActivity = lastActivity;
    }

    protected PrivateRoom(Parcel in) {
        super(in);
        this.other = in.readParcelable(User.class.getClassLoader());
        this.sender = in.readParcelable(User.class.getClassLoader());
        this.receiver = in.readParcelable(User.class.getClassLoader());
        this.senderInRoom = in.readByte() != 0;
        this.receiverInRoom = in.readByte() != 0;
    }

    public User getSender() {
        return sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public boolean isSenderInRoom() {
        return senderInRoom;
    }

    public boolean isReceiverInRoom() {
        return receiverInRoom;
    }

    public @Nullable User getOther() {
        return other;
    }

    public User getAndSetOther(long selfId) {
        if (selfId == sender.getUserId()) {
            other = receiver;
        } else {
            other = sender;
        }
        return other;
    }

    public boolean isOtherInRoom() {
        if (other == null) {
            throw new RuntimeException("other must be set before calling this method");
        }

        return other.equals(sender) ? senderInRoom : receiverInRoom;
    }

    @Override
    public boolean canEqual(Object other) {
        return other instanceof PrivateRoom;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PrivateRoom)) return false;
        PrivateRoom that = (PrivateRoom) other;
        return that.canEqual(this) && super.equals(that) &&
                MyObjects.equals(senderInRoom, that.senderInRoom) &&
                MyObjects.equals(receiverInRoom, that.receiverInRoom) &&
                MyObjects.equals(sender, that.sender) &&
                MyObjects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return MyObjects.hash(super.hashCode(), sender, receiver, senderInRoom, receiverInRoom);
    }

    @Override
    public String toString() {
        return "PrivateRoom{" +
                "roomId=" + roomId +
                ", other=" + other +
                ", lastActivity=" + lastActivity +
                ", sender=" + sender +
                ", receiver=" + receiver +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(this.other, 0);
        dest.writeParcelable(this.sender, 0);
        dest.writeParcelable(this.receiver, 0);
        dest.writeByte(senderInRoom ? (byte) 1 : (byte) 0);
        dest.writeByte(receiverInRoom ? (byte) 1 : (byte) 0);
    }
}
