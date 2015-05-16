package com.kdoherty.zipchat.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationServices;
import com.kdoherty.zipchat.R;
import com.kdoherty.zipchat.activities.HomeActivity;
import com.kdoherty.zipchat.activities.PrivateRoomActivity;
import com.kdoherty.zipchat.activities.PublicRoomActivity;
import com.kdoherty.zipchat.events.ReceivedRequestEvent;
import com.kdoherty.zipchat.events.RequestAcceptedEvent;
import com.kdoherty.zipchat.models.Request;
import com.kdoherty.zipchat.receivers.GcmBroadcastReceiver;
import com.kdoherty.zipchat.utils.FacebookUtils;
import com.kdoherty.zipchat.utils.Utils;

import java.util.concurrent.TimeUnit;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    public static final int LOCATION_TIMEOUT_IN_SECONDS = 8;

    private static final String TAG = GcmIntentService.class.getSimpleName();
    private static final int LIGHT_ON_MS = 1000;
    private static final int LIGHT_OFF_MS = 4000;
    private static final int LIGHT_COLOR = Color.argb(0, 93, 188, 210);

    private static final class Key {
        private static final String EVENT = "event";
        private static final String FACEBOOK_NAME = "name";
        private static final String CHAT_REQUEST_RESPONSE = "response";
        private static final String FACEBOOK_ID = "facebookId";
        private static final String MESSAGE = "message";
        private static final String ROOM_NAME = "roomName";
        private static final String ROOM_ID = "roomId";
        private static final String ROOM_TYPE = "roomType";
        private static final String ROOM_RADIUS = "roomRadius";
        private static final String ROOM_LATITUDE = "roomLatitude";
        private static final String ROOM_LONGITUDE = "roomLongitude";
    }

    private static final class Event {
        private static final String CHAT_REQUEST = "Chat Request";
        private static final String CHAT_REQUEST_RESPONSE = "Chat Request Response";
        private static final String CHAT_MESSAGE = "Chat Message";
        private static final String MESSAGE_FAVORITED = "Message Favorited";
    }

    private static class Value {
        private static final String PRIVATE_ROOM_TYPE = "PrivateRoom";
        private static final String PUBLIC_ROOM_TYPE = "PublicRoom";
    }

    private NotificationManager mNotificationManager;
    private GoogleApiClient mGoogleApiClient;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                String event = extras.getString(Key.EVENT);

                switch (event) {
                    case Event.CHAT_MESSAGE:
                        String roomType = extras.getString(Key.ROOM_TYPE);
                        long roomId = Long.parseLong(extras.getString(Key.ROOM_ID));
                        String senderName = extras.getString(Key.FACEBOOK_NAME);
                        String senderFacebookId = extras.getString(Key.FACEBOOK_ID);
                        String message = extras.getString(Key.MESSAGE);

                        if (Value.PUBLIC_ROOM_TYPE.equals(roomType)) {
                            String roomName = extras.getString(Key.ROOM_NAME);
                            int roomRadius = Integer.parseInt(extras.getString(Key.ROOM_RADIUS));
                            double roomLat = Double.parseDouble(extras.getString(Key.ROOM_LATITUDE));
                            double roomLon = Double.parseDouble(extras.getString(Key.ROOM_LONGITUDE));
                            receivePublicChatMessage(roomId, roomName, roomRadius, roomLat, roomLon, senderName, senderFacebookId, message);
                        } else {
                            receivePrivateChatMessage(roomId, senderName, senderFacebookId, message);
                        }
                        break;
                    case Event.MESSAGE_FAVORITED:
                        String favoritorName = extras.getString(Key.FACEBOOK_NAME);
                        long messageRoomId = Long.parseLong(extras.getString(Key.ROOM_ID));
                        String messageText = extras.getString(Key.MESSAGE);
                        String type = extras.getString(Key.ROOM_TYPE);
                        if (Value.PUBLIC_ROOM_TYPE.equals(type)) {
                            String roomName = extras.getString(Key.ROOM_NAME);
                            int roomRadius = Integer.parseInt(extras.getString(Key.ROOM_RADIUS));
                            double roomLat = Double.parseDouble(extras.getString(Key.ROOM_LATITUDE));
                            double roomLon = Double.parseDouble(extras.getString(Key.ROOM_LONGITUDE));
                            receivePublicRoomMessageFavorited(favoritorName, messageRoomId, messageText, roomName, roomRadius, roomLat, roomLon);
                        } else {
                            String favoritorFacebookId = extras.getString(Key.FACEBOOK_ID);
                            receivePrivateRoomMessageFavorited(favoritorName, favoritorFacebookId, messageRoomId, messageText);
                        }
                        break;
                    case Event.CHAT_REQUEST:
                        String requesterName = extras.getString(Key.FACEBOOK_NAME);
                        String facebookId = extras.getString(Key.FACEBOOK_ID);
                        receiveChatRequest(requesterName, facebookId);
                        break;
                    case Event.CHAT_REQUEST_RESPONSE:
                        String respondedName = extras.getString(Key.FACEBOOK_NAME);
                        String response = extras.getString(Key.CHAT_REQUEST_RESPONSE);
                        receiveChatRequestResponse(respondedName, response);
                        break;
                    default:
                        Log.i(TAG, "Received notification with event: " + event);
                }

                Log.i(TAG, "Received notification extras: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void receivePrivateRoomMessageFavorited(String userName, String facebookId,
                                                    long roomId, String message) {
        PendingIntent contentIntent = getPrivateRoomIntent(roomId, userName, facebookId);
        notifyMessageFavorited(contentIntent, userName, message);
    }

    private void receivePublicRoomMessageFavorited(String userName, long roomId, String message,
                                                   String roomName, int roomRadius, double roomLat, double roomLon) {

        if (!isInArea(roomLat, roomLon, roomRadius)) {
            return;
        }

        PendingIntent contentIntent = getPublicRoomIntent(roomId, roomName, roomRadius, roomLat, roomLon);
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

    private PendingIntent getPublicRoomIntent(long roomId, String roomName, int roomRadius, double roomLat, double roomLon) {
        Intent intent = new Intent(this, PublicRoomActivity.class);
        intent.putExtra(PublicRoomActivity.EXTRA_ROOM_ID, roomId);
        intent.putExtra(PublicRoomActivity.EXTRA_ROOM_NAME, roomName);
        intent.putExtra(PublicRoomActivity.EXTRA_ROOM_RADIUS, roomRadius);
        intent.putExtra(PublicRoomActivity.EXTRA_ROOM_LATITUDE, roomLat);
        intent.putExtra(PublicRoomActivity.EXTRA_ROOM_LONGITUDE, roomLon);

        return TaskStackBuilder.create(this)
                .addParentStack(HomeActivity.class)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getPrivateRoomIntent(long roomId, String otherUserName, String otherUserFacebookId) {
        Intent intent = new Intent(this, PrivateRoomActivity.class);
        intent.putExtra(PrivateRoomActivity.EXTRA_ROOM_ID, roomId);
        intent.putExtra(PrivateRoomActivity.EXTRA_USER_NAME, otherUserName);
        intent.putExtra(PrivateRoomActivity.EXTRA_USER_FB_ID, otherUserFacebookId);

        return TaskStackBuilder.create(this)
                .addParentStack(HomeActivity.class)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void receivePrivateChatMessage(long roomId, String senderName, String senderFacebookId, String message) {
        PendingIntent contentIntent = getPrivateRoomIntent(roomId, senderName, senderFacebookId);

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

        Bitmap facebookPicture = FacebookUtils.getFacebookProfilePicture(senderFacebookId);
        if (facebookPicture != null) {
            builder.setLargeIcon(facebookPicture);
        }

        notify(builder.build());
    }

    private boolean isInArea(double lat, double lon, int radius) {

        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).build();

        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(LOCATION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);

        if (connectionResult.isSuccess()) {
            Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (currentLocation == null) {
                // No location could be found
                return false;
            }
            double distance = Utils.getDistance(currentLocation.getLatitude(), currentLocation.getLongitude(), lat, lon);
            return distance <= radius;
        }

        // Could not connect to mGoogleApi before the timeout
        return false;
    }

    private void receivePublicChatMessage(long roomId, String roomName, int roomRadius,
                                          double roomLat, double roomLon,
                                          String senderName, String senderFacebookId, String message) {

        if (!isInArea(roomLat, roomLon, roomRadius)) {
            return;
        }

        PendingIntent contentIntent = getPublicRoomIntent(roomId, roomName, roomRadius, roomLat, roomLon);

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

        Bitmap facebookPicture = FacebookUtils.getFacebookProfilePicture(senderFacebookId);
        if (facebookPicture != null) {
            builder.setLargeIcon(facebookPicture);
        } else {
            Log.w(TAG, "Null facebook picture for facebookId: " + senderFacebookId);
        }

        notify(builder.build());
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

        Bitmap facebookPicture = FacebookUtils.getFacebookProfilePicture(facebookId);
        if (facebookPicture != null) {
            builder.setLargeIcon(facebookPicture);
        }

        notify(builder.build());
    }
}