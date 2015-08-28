package com.kdoherty.zipchat.services;

import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

/**
 * Created by kevindoherty on 2/5/15.
 */
public class ChatService extends Thread {

    private static final String TAG = ChatService.class.getSimpleName();
    private static final String WEBSOCKET_ENDPOINT = "ws://zipchatapp.herokuapp.com/";
    private AsyncHttpClient.WebSocketConnectCallback mCallback;
    private String mUrl;

    private Future<WebSocket> mWebSocketFuture;

    ChatService(ChatService other) {
        this.mUrl = other.mUrl;
        this.mCallback = other.mCallback;
        this.start();
    }

    public ChatService(long userId, long roomId, RoomType roomType, String authToken, AsyncHttpClient.WebSocketConnectCallback callback) {
        this.mUrl = buildUrl(userId, roomId, roomType, authToken);
        this.mCallback = callback;
        this.start();
    }

    private String buildUrl(long userId, long roomId, RoomType roomType, String authToken) {
        return WEBSOCKET_ENDPOINT + roomType.resourceUrl + "/" + roomId + "/join?userId=" + userId
                + "&authToken=" + authToken;
    }

    public void cancel() {
        mWebSocketFuture.cancel();
    }

    public boolean isConnecting() {
        return !mWebSocketFuture.isDone();
    }

    @Override
    public void run() {
        mWebSocketFuture = AsyncHttpClient.getDefaultInstance().websocket(mUrl, null, mCallback);
    }

    public enum RoomType {
        PUBLIC("publicRooms"),
        PRIVATE("privateRooms");

        private String resourceUrl;

        RoomType(String resourceUrl) {
            this.resourceUrl = resourceUrl;
        }
    }
}
