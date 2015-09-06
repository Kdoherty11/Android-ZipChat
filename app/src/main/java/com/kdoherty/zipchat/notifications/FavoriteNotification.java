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
        notifyMessageFavorited(contentIntent);
    }

    private void notifyMessageFavorited(PendingIntent contentIntent) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(mContext.getString(R.string.message_favorited))
                        .setContentIntent(contentIntent)
                        .setContentText(mMessageFavoritor.getName() + " " + mContext.getString(R.string.has_favorited_your_message));

        setNotificationDefaults(builder);

        notify(builder.build());
    }
}
