package com.kdoherty.zipchat.services;

import com.koushikdutta.async.http.AsyncHttpClient;

/**
 * Created by kevindoherty on 2/5/15.
 */
public class ChatService extends Thread {

    private AsyncHttpClient.WebSocketConnectCallback mCallback;

    private static final String WEBSOCKET_ENDPOINT = "ws://zipchatapp.herokuapp.com/";

    private String mUrl;

    public ChatService(long userId, long roomId, boolean isPublic, String authToken, AsyncHttpClient.WebSocketConnectCallback callback) {
        this.mUrl = buildUrl(userId, roomId, isPublic, authToken);
        this.mCallback = callback;
        this.start();
    }

    private String buildUrl(long userId, long roomId, boolean isPublic, String authToken) {
        String roomType = isPublic ? "publicRooms" : "privateRooms";
        return WEBSOCKET_ENDPOINT + roomType + "/" + roomId + "/join?userId=" + userId + "&authToken=" + authToken;
    }

    @Override
    public void run() {
        AsyncHttpClient.getDefaultInstance().websocket(mUrl, null, mCallback);
    }
}
