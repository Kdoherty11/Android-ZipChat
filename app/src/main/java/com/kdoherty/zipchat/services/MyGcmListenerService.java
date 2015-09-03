package com.kdoherty.zipchat.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.HomeActivity;
import com.kdoherty.zipchat.activities.MessageDetailsActivity;
import com.kdoherty.zipchat.activities.PrivateRoomActivity;
import com.kdoherty.zipchat.activities.PublicRoomActivity;
import com.kdoherty.zipchat.events.ReceivedRequestEvent;
import com.kdoherty.zipchat.events.RequestAcceptedEvent;
import com.kdoherty.zipchat.models.AbstractRoom;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.PrivateRoom;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.BusProvider;
import com.kdoherty.zipchat.utils.FacebookManager;
import com.kdoherty.zipchat.utils.LocationManager;

import java.util.concurrent.TimeUnit;

public class MyGcmListenerService extends GcmListenerService {
    public static final int NOTIFICATION_ID = 1;
    public static final int LOCATION_TIMEOUT_IN_SECONDS = 8;

    private static final String TAG = MyGcmListenerService.class.getSimpleName();

    private static final int LIGHT_ON_MS = 1000;
    private static final int LIGHT_OFF_MS = 4000;
    private static final int LIGHT_COLOR = Color.argb(0, 93, 188, 210);
    private NotificationManager mNotificationManager;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From: " + from);

        final String event = data.getString(Key.EVENT, "No event found in received message");
        Gson gson = new Gson();

        switch (event) {
            case Event.CHAT_MESSAGE: {
                Message message = gson.fromJson(data.getString(Key.MESSAGE), Message.class);
                AbstractRoom room = gson.fromJson(data.getString(Key.ROOM), AbstractRoom.class);

                if (room instanceof PublicRoom) {
                    receivePublicChatMessage((PublicRoom) room, message);
                } else if (room instanceof PrivateRoom) {
                    receivePrivateChatMessage((PrivateRoom) room, message);
                } else {
                    Log.e(TAG, "Abstract room can't be cast to PublicRoom or PrivateRoom");
                }
                break;
            }
            case Event.MESSAGE_FAVORITED: {
                Message message = gson.fromJson(data.getString(Key.MESSAGE), Message.class);
                User user = gson.fromJson(data.getString(Key.USER), User.class);
                AbstractRoom room = gson.fromJson(data.getString(Key.ROOM), AbstractRoom.class);
                if (room.isPublic()) {
                    receivePublicRoomMessageFavorited((PublicRoom) room, message, user);
                } else if (room.isPrivate()) {
                    receivePrivateRoomMessageFavorited((PrivateRoom) room, message, user);
                } else {
                    Log.e(TAG, "Abstract room can't be cast to PublicRoom or PrivateRoom");
                }
                break;
            }
            case Event.CHAT_REQUEST:
                User sender = gson.fromJson(data.getString(Key.USER), User.class);
                receiveChatRequest(sender);
                break;
            case Event.CHAT_REQUEST_RESPONSE:
                User receiver = gson.fromJson(data.getString(Key.USER), User.class);
                String response = data.getString(Key.CHAT_REQUEST_RESPONSE);
                receiveChatRequestResponse(receiver, response);
                break;
            default:
                Log.i(TAG, "Received notification with event: " + event);
        }

        Log.i(TAG, "Received notification extras: " + data.toString());
    }

    private void receivePrivateRoomMessageFavorited(PrivateRoom room, Message message, User user) {
        PendingIntent contentIntent = getPrivateRoomIntent(room.getRoomId(), user);
        notifyMessageFavorited(contentIntent, user.getName(), message.getMessage());
    }

    private void receivePublicRoomMessageFavorited(PublicRoom publicRoom, Message message, User user) {

        if (!isInArea(publicRoom)) {
            return;
        }

        // TODO

        PendingIntent contentIntent = getPublicRoomPendingIntent(publicRoom);
        notifyMessageFavorited(contentIntent, user.getName(), message.getMessage());
    }

    private void notifyMessageFavorited(PendingIntent contentIntent, String userName, String message) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
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

    private PendingIntent getPublicRoomPendingIntent(PublicRoom publicRoom) {
        Intent publicRoomIntent = PublicRoomActivity.getIntent(this, publicRoom);

        return TaskStackBuilder.create(this)
                .addParentStack(HomeActivity.class)
                .addNextIntentWithParentStack(publicRoomIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPrivateRoomIntent(long roomId, User user) {
        Intent intent = PrivateRoomActivity.getIntent(this, roomId, user);

        return TaskStackBuilder.create(this)
                .addParentStack(HomeActivity.class)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public PendingIntent getPublicRoomMessageDetailsIntent(PublicRoom publicRoom, Message message) {
        Intent intent = MessageDetailsActivity.getIntent(this, message);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this)
                .addParentStack(HomeActivity.class)
                .addParentStack(PublicRoomActivity.class)
                .addNextIntentWithParentStack(intent);

        Intent publicRoomIntent = PublicRoomActivity.getIntent(this, publicRoom);
        stackBuilder.editIntentAt(1).getExtras().putAll(publicRoomIntent.getExtras());

        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void receivePrivateChatMessage(PrivateRoom room, Message message) {
        PendingIntent contentIntent = getPrivateRoomIntent(room.getRoomId(), message.getSender());

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle(message.getSender().getName())
                        .setAutoCancel(true)
                        .setLights(Color.argb(0, 93, 188, 210), LIGHT_ON_MS, LIGHT_OFF_MS)
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

    private boolean isInArea(PublicRoom publicRoom) {

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).build();

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

        Log.e(TAG, "isInArea returning false because Google api client could not connect...");

        // Could not connect to mGoogleApi before the timeout
        return false;
    }

    private void receivePublicChatMessage(PublicRoom publicRoom, Message message) {

        Log.d(TAG, "Receive public chat message");

        if (!isInArea(publicRoom)) {
            Log.d(TAG, "Not showing notification because not in the room radius");
            return;
        }

        PendingIntent contentIntent = getPublicRoomPendingIntent(publicRoom);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle(message.getSender().getName())
                        .setAutoCancel(true)
                        .setLights(Color.argb(0, 93, 188, 210), LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message.getMessage()))
                        .setContentIntent(contentIntent)
                        .setContentText(message.getMessage());

        Bitmap facebookPicBm;
        if (!TextUtils.isEmpty(message.getSender().getFacebookId())) {
            facebookPicBm = FacebookManager.getFacebookProfilePicture(message.getSender().getFacebookId());
            if (facebookPicBm == null) {
                facebookPicBm = BitmapFactory.decodeResource(getResources(), R.drawable.com_facebook_profile_picture_blank_square);
                Log.w(TAG, "Null facebook picture for facebookId: " + message.getSender().getFacebookId());
            }
        } else {
            facebookPicBm = BitmapFactory.decodeResource(getResources(), R.drawable.com_facebook_profile_picture_blank_square);
        }

        builder.setLargeIcon(facebookPicBm);

        notify(builder.build());

        Log.d(TAG, "Success receiving public room chat message");
    }

    private void notify(Notification notification) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager)
                    this.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void receiveChatRequestResponse(User receiver, String response) {
        String message = receiver.getName() + " has " + response + " your chat request";

        Intent intent = new Intent(this, HomeActivity.class);

        if (Request.Status.accepted.toString().equals(response)) {
            intent.setAction(HomeActivity.ACTION_OPEN_PRIVATE_ROOMS_TAB);
            BusProvider.getInstance().post(new RequestAcceptedEvent());
        }

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle("Response")
                        .setAutoCancel(true)
                        .setLights(Color.argb(0, 93, 188, 210), LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentIntent(contentIntent)
                        .setContentText(message);

        notify(builder.build());
    }

    private void receiveChatRequest(User user) {
        BusProvider.getInstance().post(new ReceivedRequestEvent());

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setAction(HomeActivity.ACTION_OPEN_REQUESTS_TAB);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String message = user.getName() + " sent you a chat request!";

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle("Chat Request")
                        .setAutoCancel(true)
                        .setLights(Color.argb(0, 93, 188, 210), LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentIntent(contentIntent)
                        .setContentText(message);

        Bitmap facebookPicture = FacebookManager.getFacebookProfilePicture(user.getFacebookId());
        if (facebookPicture != null) {
            builder.setLargeIcon(facebookPicture);
        }

        notify(builder.build());
    }

    public static class Key {
        public static final String EVENT = "event";
        public static final String USER = "user";
        public static final String ROOM = "room";
        public static final String MESSAGE = "message";
        public static final String CHAT_REQUEST_RESPONSE = "response";
    }

    public static class Event {
        public static final String CHAT_REQUEST = "Chat Request";
        public static final String CHAT_REQUEST_RESPONSE = "Chat Request Response";
        public static final String CHAT_MESSAGE = "Chat Message";
        public static final String MESSAGE_FAVORITED = "Message Favorited";
    }
}