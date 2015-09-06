package com.kdoherty.zipchat.services;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.kdoherty.zipchat.notifications.ChatRequestNotification;
import com.kdoherty.zipchat.notifications.ChatResponseNotification;
import com.kdoherty.zipchat.notifications.FavoriteNotification;
import com.kdoherty.zipchat.notifications.MessageNotification;

public class MyGcmListenerService extends GcmListenerService {

    public static final String EVENT_KEY = "event";
    private static final String TAG = MyGcmListenerService.class.getSimpleName();

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "onMessageReceived: " + data);
        final String event = data.getString(EVENT_KEY, "No event found in received message");

        switch (event) {
            case Event.CHAT_MESSAGE:
                new MessageNotification(this, data).handleNotification();
                break;
            case Event.MESSAGE_FAVORITED:
                new FavoriteNotification(this, data).handleNotification();
                break;
            case Event.CHAT_REQUEST:
                new ChatRequestNotification(this, data).handleNotification();
                break;
            case Event.CHAT_REQUEST_RESPONSE:
                new ChatResponseNotification(this, data).handleNotification();
                break;
            default:
                Log.i(TAG, "Received notification with event: " + event);
        }
    }

    public class Event {
        public static final String CHAT_REQUEST = "Chat Request";
        public static final String CHAT_REQUEST_RESPONSE = "Chat Request Response";
        public static final String CHAT_MESSAGE = "Chat Message";
        public static final String MESSAGE_FAVORITED = "Message Favorited";
    }
}