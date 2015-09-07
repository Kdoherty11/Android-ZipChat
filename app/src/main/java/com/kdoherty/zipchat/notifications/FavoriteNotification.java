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

    private Message mMessage;
    private User mMessageFavoritor;
    private AbstractRoom mRoom;

    public FavoriteNotification(Context context, Bundle data) {
        super(context);
        mMessage = mGson.fromJson(data.getString(Key.MESSAGE), Message.class);
        mMessageFavoritor = mGson.fromJson(data.getString(Key.USER), User.class);
        mRoom = mGson.fromJson(data.getString(Key.ROOM), AbstractRoom.class);
    }

    @Override
    public void handleNotification() {
        boolean addRoomToBackStack = !mRoom.isPublic() || userInArea((PublicRoom) mRoom);
        PendingIntent contentIntent = getMessageDetailsPendingIntent(mMessage, mRoom, addRoomToBackStack);
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
