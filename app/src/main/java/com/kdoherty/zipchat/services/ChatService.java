package com.kdoherty.zipchat.services;

import com.koushikdutta.async.http.AsyncHttpClient;

/**
 * Created by kevindoherty on 2/5/15.
 */
public class ChatService extends Thread {

    private AsyncHttpClient.WebSocketConnectCallback mCallback;

    private static final String WEBSOCKET_ENDPOINT = "ws://zipchatapp.herokuapp.com/rooms/";

    private String mUrl;

    public ChatService(long userId, long roomId, AsyncHttpClient.WebSocketConnectCallback callback) {
        this.mUrl = buildUrl(userId, roomId);
        this.mCallback = callback;
        this.start();
    }

    private String buildUrl(long userId, long roomId) {
        return WEBSOCKET_ENDPOINT + roomId + "/join?userId=" + userId;
    }

    @Override
    public void run() {
        AsyncHttpClient.getDefaultInstance().websocket(mUrl, null, mCallback);
    }
}
