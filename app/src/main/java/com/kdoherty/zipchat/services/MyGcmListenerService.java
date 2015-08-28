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
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.HomeActivity;
import com.kdoherty.zipchat.activities.PrivateRoomActivity;
import com.kdoherty.zipchat.activities.PublicRoomActivity;
import com.kdoherty.zipchat.events.ReceivedRequestEvent;
import com.kdoherty.zipchat.events.RequestAcceptedEvent;
import com.kdoherty.zipchat.models.PublicRoom;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.models.User;
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

        final String event = data.getString(Key.EVENT);

        switch (event) {
            case Event.CHAT_MESSAGE:
                String roomType = data.getString(Key.ROOM_TYPE);
                long roomId = Long.parseLong(data.getString(Key.ROOM_ID));
                String senderName = data.getString(Key.FACEBOOK_NAME);
                String senderFacebookId = data.getString(Key.FACEBOOK_ID);
                long senderId = Long.parseLong(data.getString(Key.USER_ID));
                User sender = new User(senderName, senderFacebookId, senderId);
                String message = data.getString(Key.MESSAGE);

                if (Value.PUBLIC_ROOM_TYPE.equals(roomType)) {
                    String roomName = data.getString(Key.ROOM_NAME);
                    int roomRadius = Integer.parseInt(data.getString(Key.ROOM_RADIUS));
                    double roomLat = Double.parseDouble(data.getString(Key.ROOM_LATITUDE));
                    double roomLon = Double.parseDouble(data.getString(Key.ROOM_LONGITUDE));
                    PublicRoom publicRoom = new PublicRoom(roomId, roomName, roomRadius, roomLat, roomLon);
                    receivePublicChatMessage(publicRoom, senderName, senderFacebookId, message);
                } else {
                    receivePrivateChatMessage(roomId, sender, message);
                }
                break;
            case Event.MESSAGE_FAVORITED:
                String favoritorName = data.getString(Key.FACEBOOK_NAME);
                long messageRoomId = Long.parseLong(data.getString(Key.ROOM_ID, ""));
                String messageText = data.getString(Key.MESSAGE);
                String type = data.getString(Key.ROOM_TYPE);
                if (Value.PUBLIC_ROOM_TYPE.equals(type)) {
                    String roomName = data.getString(Key.ROOM_NAME);
                    int roomRadius = Integer.parseInt(data.getString(Key.ROOM_RADIUS, ""));
                    double roomLat = Double.parseDouble(data.getString(Key.ROOM_LATITUDE, ""));
                    double roomLon = Double.parseDouble(data.getString(Key.ROOM_LONGITUDE, ""));
                    PublicRoom publicRoom = new PublicRoom(messageRoomId, roomName, roomRadius, roomLat, roomLon);
                    receivePublicRoomMessageFavorited(favoritorName, messageText, publicRoom);
                } else {
                    String favoritorFacebookId = data.getString(Key.FACEBOOK_ID);
                    long favoritorUserId = Long.parseLong(data.getString(Key.USER_ID));
                    User favoritor = new User(favoritorName, favoritorFacebookId, favoritorUserId);
                    receivePrivateRoomMessageFavorited(messageRoomId, favoritor, messageText);
                }
                break;
            case Event.CHAT_REQUEST:
                String requesterName = data.getString(Key.FACEBOOK_NAME);
                String facebookId = data.getString(Key.FACEBOOK_ID);
                receiveChatRequest(requesterName, facebookId);
                break;
            case Event.CHAT_REQUEST_RESPONSE:
                String respondedName = data.getString(Key.FACEBOOK_NAME);
                String response = data.getString(Key.CHAT_REQUEST_RESPONSE);
                receiveChatRequestResponse(respondedName, response);
                break;
            default:
                Log.i(TAG, "Received notification with event: " + event);
        }

        Log.i(TAG, "Received notification extras: " + data.toString());
    }

    private void receivePrivateRoomMessageFavorited(long roomId, User user, String message) {
        PendingIntent contentIntent = getPrivateRoomIntent(roomId, user);
        notifyMessageFavorited(contentIntent, user.getName(), message);
    }

    private void receivePublicRoomMessageFavorited(String userName, String message, PublicRoom publicRoom) {

        if (!isInArea(publicRoom)) {
            return;
        }

        PendingIntent contentIntent = getPublicRoomPendingIntent(publicRoom);
        notifyMessageFavorited(contentIntent, userName, message);
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

    private void receivePrivateChatMessage(long roomId, User sender, String message) {
        PendingIntent contentIntent = getPrivateRoomIntent(roomId, sender);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle(sender.getName())
                        .setAutoCancel(true)
                        .setLights(Color.argb(0, 93, 188, 210), LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentIntent(contentIntent)
                        .setContentText(message);

        Bitmap facebookPicture = FacebookManager.getFacebookProfilePicture(sender.getFacebookId());
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

    private void receivePublicChatMessage(PublicRoom publicRoom,
                                          String senderName, String senderFacebookId, String message) {

        Log.d(TAG, "Receive public chat message");

        if (!isInArea(publicRoom)) {
            Log.d(TAG, "Not showing notification because not in the room radius");
            return;
        }

        PendingIntent contentIntent = getPublicRoomPendingIntent(publicRoom);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_zipchat)
                        .setContentTitle(senderName)
                        .setAutoCancel(true)
                        .setLights(Color.argb(0, 93, 188, 210), LIGHT_ON_MS, LIGHT_OFF_MS)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(message))
                        .setContentIntent(contentIntent)
                        .setContentText(message);

        Bitmap facebookPicBm;
        if (!TextUtils.isEmpty(senderFacebookId)) {
            facebookPicBm = FacebookManager.getFacebookProfilePicture(senderFacebookId);
            if (facebookPicBm == null) {
                facebookPicBm = BitmapFactory.decodeResource(getResources(), R.drawable.com_facebook_profile_picture_blank_square);
                Log.w(TAG, "Null facebook picture for facebookId: " + senderFacebookId);
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

    private void receiveChatRequestResponse(String name, String response) {
        String message = name + " has " + response + " your chat request";

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

    private void receiveChatRequest(String name, String facebookId) {
        BusProvider.getInstance().post(new ReceivedRequestEvent());

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setAction(HomeActivity.ACTION_OPEN_REQUESTS_TAB);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        String message = name + " sent you a chat request!";

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

        Bitmap facebookPicture = FacebookManager.getFacebookProfilePicture(facebookId);
        if (facebookPicture != null) {
            builder.setLargeIcon(facebookPicture);
        }

        notify(builder.build());
    }

    public static final class Key {
        public static final String EVENT = "event";
        public static final String FACEBOOK_NAME = "name";
        public static final String CHAT_REQUEST_RESPONSE = "response";
        public static final String FACEBOOK_ID = "facebookId";
        public static final String MESSAGE = "message";
        public static final String ROOM_NAME = "roomName";
        public static final String ROOM_ID = "roomId";
        public static final String ROOM_TYPE = "roomType";
        public static final String ROOM_RADIUS = "roomRadius";
        public static final String ROOM_LATITUDE = "roomLatitude";
        public static final String ROOM_LONGITUDE = "roomLongitude";
        public static final String USER_ID = "userId";
    }

    public static final class Event {
        public static final String CHAT_REQUEST = "Chat Request";
        public static final String CHAT_REQUEST_RESPONSE = "Chat Request Response";
        public static final String CHAT_MESSAGE = "Chat Message";
        public static final String MESSAGE_FAVORITED = "Message Favorited";
    }

    private static class Value {
        public static final String PRIVATE_ROOM_TYPE = "PrivateRoom";
        public static final String PUBLIC_ROOM_TYPE = "PublicRoom";
    }
}