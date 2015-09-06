package com.kdoherty.zipchat.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.HomeActivity;
import com.kdoherty.zipchat.events.RequestAcceptedEvent;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.FacebookManager;

/**
 * Created by kdoherty on 9/3/15.
 */
public class ChatResponseNotification extends AbstractNotification {

    private User mRequestReceiver;
    private String mResponse;

    public ChatResponseNotification(Context context, Bundle data) {
        super(context);
        mRequestReceiver = mGson.fromJson(data.getString(Key.USER), User.class);
        mResponse = data.getString(Key.CHAT_REQUEST_RESPONSE);
    }

    @Override
    public void handleNotification() {
        String message = mRequestReceiver.getName() + " has " + mResponse + " your chat request";

        Intent intent = new Intent(mContext, HomeActivity.class);

        boolean isAccepted = Request.Status.accepted.toString().equals(mResponse);

        if (isAccepted) {
            intent.setAction(HomeActivity.ACTION_OPEN_PRIVATE_ROOMS_TAB);
            BusProvider.getInstance().post(new RequestAcceptedEvent());
        }

        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                intent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setContentTitle("Response")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentIntent(contentIntent)
                        .setContentText(message);

        setNotificationDefaults(builder);

        if (isAccepted) {
            Bitmap senderFbPicBm = FacebookManager.getFacebookProfilePicture(mContext, mRequestReceiver.getFacebookId());
            builder.setLargeIcon(senderFbPicBm);
        }

        notify(builder.build());
    }
}
