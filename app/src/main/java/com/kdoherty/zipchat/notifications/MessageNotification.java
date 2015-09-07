package com.kdoherty.zipchat.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kdoherty.zipchat.models.AbstractRoom;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.utils.FacebookManager;

/**
 * Created by kdoherty on 9/3/15.
 */
public class MessageNotification extends AbstractNotification {

    private static final String TAG = MessageNotification.class.getSimpleName();
    private Message mMessage;
    private AbstractRoom mRoom;

    public MessageNotification(Context context, Bundle data) {
        super(context);
        this.mMessage = mGson.fromJson(data.getString(Key.MESSAGE), Message.class);
        this.mRoom = mGson.fromJson(data.getString(Key.ROOM), AbstractRoom.class);
    }

    @Override
    public void handleNotification() {
        if (mRoom.isPublic()) {
            receivePublicChatMessage((PublicRoom) mRoom);
        } else {
            receivePrivateChatMessage((PrivateRoom) mRoom);
        }
    }

    private void receivePublicChatMessage(PublicRoom publicRoom) {
        Log.d(TAG, "Receive public chat message");

        if (!userInArea(publicRoom)) {
            Log.d(TAG, "Not showing notification because not in the room radius");
            return;
        }

        PendingIntent contentIntent = getPublicRoomPendingIntent(publicRoom);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(publicRoom.getName())
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(mMessage.getMessage()))
                        .setContentIntent(contentIntent)
                        .setContentText(mMessage.getSender().getName() + ": " + mMessage.getMessage());

        setNotificationDefaults(builder);

        Bitmap senderFbPicBm = FacebookManager.getFacebookProfilePicture(mContext, mMessage.getSender().getFacebookId());
        builder.setLargeIcon(senderFbPicBm);

        notify(builder.build());

        Log.d(TAG, "Success receiving public mRoom chat message");
    }

    private void receivePrivateChatMessage(PrivateRoom room) {
        PendingIntent contentIntent = getPrivateRoomIntent(room);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(mMessage.getSender().getName())
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(mMessage.getMessage()))
                        .setContentIntent(contentIntent)
                        .setContentText(mMessage.getMessage());

        setNotificationDefaults(builder);

        Bitmap senderFbPicBm = FacebookManager.getFacebookProfilePicture(mContext, mMessage.getSender().getFacebookId());
        builder.setLargeIcon(senderFbPicBm);

        notify(builder.build());
    }
}
