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
import com.kdoherty.zipchat.events.ReceivedRequestEvent;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.FacebookManager;

/**
 * Created by kdoherty on 9/3/15.
 */
public class ChatRequestNotification extends AbstractNotification {

    private User mRequestSender;

    public ChatRequestNotification(Context context, Bundle data) {
        super(context);
        this.mRequestSender = mGson.fromJson(data.getString(Key.USER), User.class);
    }

    @Override
    public void handleNotification() {
        BusProvider.getInstance().post(new ReceivedRequestEvent());

        Intent intent = new Intent(mContext, HomeActivity.class);
        intent.setAction(HomeActivity.ACTION_OPEN_REQUESTS_TAB);

        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        String message = mRequestSender.getName() + " sent you a chat request!";

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle("Chat Request")
                        .setAutoCancel(true)
                        .setLights(Color.argb(0, 93, 188, 210), LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentIntent(contentIntent)
                        .setContentText(message);

        Bitmap facebookPicture = FacebookManager.getFacebookProfilePicture(mRequestSender.getFacebookId());
        if (facebookPicture != null) {
            builder.setLargeIcon(facebookPicture);
        }

        notify(builder.build());
    }
}