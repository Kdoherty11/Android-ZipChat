package com.kdoherty.zipchat.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.models.AbstractRoom;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.User;

/**
 * Created by kdoherty on 9/3/15.
 */
public class FavoriteNotification extends AbstractNotification {

    private Message message;
    private User mMessageFavoritor;
    private AbstractRoom room;

    public FavoriteNotification(Context context, Bundle data) {
        super(context);
        message = mGson.fromJson(data.getString(Key.MESSAGE), Message.class);
        mMessageFavoritor = mGson.fromJson(data.getString(Key.USER), User.class);
        room = mGson.fromJson(data.getString(Key.ROOM), AbstractRoom.class);
    }

    @Override
    public void handleNotification() {
        boolean addRoomToBackStack = !room.isPublic() || userInArea((PublicRoom) room);
        PendingIntent contentIntent = getMessageDetailsPendingIntent(message, room, addRoomToBackStack);
        notifyMessageFavorited(contentIntent, mMessageFavoritor.getName(), message.getMessage());
    }

    private void notifyMessageFavorited(PendingIntent contentIntent, String userName, String message) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle("Favorite")
                        .setAutoCancel(true)
                        .setLights(LIGHT_COLOR, LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentIntent(contentIntent)
                        .setContentText(userName + " has favorited your message");

        notify(builder.build());
    }
}
