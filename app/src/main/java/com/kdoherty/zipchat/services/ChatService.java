package com.kdoherty.zipchat.services;

import com.kdoherty.zipchat.models.AbstractRoom;
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

    public ChatService(long userId, AbstractRoom room, String authToken, AsyncHttpClient.WebSocketConnectCallback callback) {
        this.mUrl = buildUrl(userId, room, authToken);
        this.mCallback = callback;
        this.start();
    }

    private String buildUrl(long userId, AbstractRoom room, String authToken) {
        return WEBSOCKET_ENDPOINT + getResourceUrl(room.getType()) + "/" + room.getRoomId() + "/join?userId=" + userId
                + "&authToken=" + authToken;
    }

    private String getResourceUrl(AbstractRoom.RoomType roomType) {
        return roomType == AbstractRoom.RoomType.PUBLIC ? "publicRooms" : "privateRooms";
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

//    public enum RoomType {
//        PUBLIC("publicRooms"),
//        PRIVATE("privateRooms");
//
//        private String resourceUrl;
//
//        RoomType(String resourceUrl) {
//            this.resourceUrl = resourceUrl;
//        }
//    }
}
