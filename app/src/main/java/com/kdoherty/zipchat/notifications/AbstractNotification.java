package com.kdoherty.zipchat.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.kdoherty.zipchat.activities.HomeActivity;
import com.kdoherty.zipchat.activities.MessageDetailsActivity;
import com.kdoherty.zipchat.activities.PrivateRoomActivity;
import com.kdoherty.zipchat.activities.PublicRoomActivity;
import com.kdoherty.zipchat.models.AbstractRoom;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.GsonProvider;
import com.kdoherty.zipchat.utils.LocationManager;
import com.kdoherty.zipchat.utils.UserManager;

import java.util.concurrent.TimeUnit;

/**
 * Created by kdoherty on 9/3/15.
 */
public abstract class AbstractNotification {

    public static final int NOTIFICATION_ID = 1;
    public static final int LOCATION_TIMEOUT_IN_SECONDS = 8;
    protected static final int LIGHT_ON_MS = 1000;
    protected static final int LIGHT_OFF_MS = 4000;
    protected static final int LIGHT_COLOR = Color.argb(0, 93, 188, 210);
    private static final String TAG = AbstractNotification.class.getSimpleName();
    protected final Gson mGson = GsonProvider.getInstance();
    protected final Context mContext;
    protected NotificationManager mNotificationManager;

    protected AbstractNotification(Context context) {
        this.mContext = context;
    }

    public abstract void handleNotification();

    protected void notify(Notification notification) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    protected PendingIntent getMessageDetailsPendingIntent(Message message, AbstractRoom room, boolean includeRoomIntent) {
        Intent messageDetailsIntent = MessageDetailsActivity.getIntent(mContext, message);

        TaskStackBuilder builder = TaskStackBuilder.create(mContext)
                .addNextIntent(new Intent(mContext, HomeActivity.class));

        if (includeRoomIntent) {
            Intent roomIntent;
            if (room.isPublic()) {
                roomIntent = PublicRoomActivity.getIntent(mContext, (PublicRoom) room);
            } else {
                PrivateRoom privateRoom = (PrivateRoom) room;
                User other = privateRoom.getAndSetOther(UserManager.getId(mContext));
                roomIntent = PrivateRoomActivity.getIntent(mContext, room.getRoomId(), other);
            }
            builder.addNextIntent(roomIntent);
        }

        return builder.addNextIntent(messageDetailsIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent getPublicRoomPendingIntent(PublicRoom publicRoom) {
        Intent publicRoomIntent = PublicRoomActivity.getIntent(mContext, publicRoom);

        return TaskStackBuilder.create(mContext)
                .addParentStack(HomeActivity.class)
                .addNextIntentWithParentStack(publicRoomIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent getPrivateRoomIntent(long roomId, User user) {
        Intent intent = PrivateRoomActivity.getIntent(mContext, roomId, user);

        return TaskStackBuilder.create(mContext)
                .addParentStack(HomeActivity.class)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected boolean userInArea(PublicRoom publicRoom) {

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(mContext).addApi(LocationServices.API).build();

        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(LOCATION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

        if (connectionResult.isSuccess()) {
            Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (currentLocation == null) {
                // No location could be found
                return false;
            }
            double distance = LocationManager.getDistance(currentLocation.getLatitude(), currentLocation.getLongitude(), publicRoom.getLatitude(), publicRoom.getLongitude());
            Log.d(TAG, "Distance from the center of the room: " + distance + " and radius is: " + publicRoom.getRadius());
            return distance <= publicRoom.getRadius();
        }

        Log.e(TAG, "userInArea returning false because Google api client could not connect...");

        // Could not connect to mGoogleApi before the timeout
        return false;
    }

    public class Key {
        public static final String USER = "user";
        public static final String ROOM = "room";
        public static final String MESSAGE = "message";
        public static final String CHAT_REQUEST_RESPONSE = "response";
    }
}