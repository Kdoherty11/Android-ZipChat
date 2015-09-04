package com.kdoherty.zipchat.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.kdoherty.zipchat.R;
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
    private Message message;
    private AbstractRoom room;

    public MessageNotification(Context context, Bundle data) {
        super(context);
        this.message = mGson.fromJson(data.getString(Key.MESSAGE), Message.class);
        this.room = mGson.fromJson(data.getString(Key.ROOM), AbstractRoom.class);
    }

    @Override
    public void handleNotification() {
        if (room.isPublic()) {
            receivePublicChatMessage((PublicRoom) room);
        } else {
            receivePrivateChatMessage((PrivateRoom) room);
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
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle(message.getSender().getName())
                        .setAutoCancel(true)
                        .setLights(LIGHT_COLOR, LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message.getMessage()))
                        .setContentIntent(contentIntent)
                        .setContentText(message.getMessage());

        Bitmap facebookPicBm;
        if (!TextUtils.isEmpty(message.getSender().getFacebookId())) {
            facebookPicBm = FacebookManager.getFacebookProfilePicture(message.getSender().getFacebookId());
            if (facebookPicBm == null) {
                facebookPicBm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.com_facebook_profile_picture_blank_square);
                Log.w(TAG, "Null facebook picture for facebookId: " + message.getSender().getFacebookId());
            }
        } else {
            facebookPicBm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.com_facebook_profile_picture_blank_square);
        }

        builder.setLargeIcon(facebookPicBm);

        notify(builder.build());

        Log.d(TAG, "Success receiving public room chat message");
    }

    private void receivePrivateChatMessage(PrivateRoom room) {
        PendingIntent contentIntent = getPrivateRoomIntent(room.getRoomId(), message.getSender());

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle(message.getSender().getName())
                        .setAutoCancel(true)
                        .setLights(LIGHT_COLOR, LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message.getMessage()))
                        .setContentIntent(contentIntent)
                        .setContentText(message.getMessage());

        Bitmap facebookPicture = FacebookManager.getFacebookProfilePicture(message.getSender().getFacebookId());
        if (facebookPicture != null) {
            builder.setLargeIcon(facebookPicture);
        }

        notify(builder.build());
    }
}
