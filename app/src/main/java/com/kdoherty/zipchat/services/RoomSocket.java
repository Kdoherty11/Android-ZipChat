package com.kdoherty.zipchat.services;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kdoherty.zipchat.events.AddFavoriteEvent;
import com.kdoherty.zipchat.events.IsSubscribedEvent;
import com.kdoherty.zipchat.events.MemberJoinEvent;
import com.kdoherty.zipchat.events.MemberLeaveEvent;
import com.kdoherty.zipchat.events.ReceivedRoomMembersEvent;
import com.kdoherty.zipchat.events.RemoveFavoriteEvent;
import com.kdoherty.zipchat.events.TalkEvent;
import com.kdoherty.zipchat.models.Message;
import com.kdoherty.zipchat.models.User;
import com.kdoherty.zipchat.utils.UserManager;
import com.kdoherty.zipchat.utils.Utils;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by kdoherty on 8/13/15.
 */
public class RoomSocket {

    private static final String TAG = RoomSocket.class.getSimpleName();
    public static final String KEEP_ALIVE_MSG = "Beat";

    public interface ReconnectCallback {
        void reconnect();
    }

    private final CompletedCallback closedCallback = new CompletedCallback() {
        @Override
        public void onCompleted(Exception ex) {
            Utils.debugToast(mContext, "Closed websocket. Exception: " + ex);
            if (ex != null) {
                Log.w(TAG, "Attempting to recover from " + ex.getMessage());
                Utils.debugToast(mContext, "Attempting to recover from " + ex.getMessage());
                mReconnectCallback.reconnect();
            }
        }
    };

    private final WebSocket.StringCallback stringCallback = new WebSocket.StringCallback() {
        @Override
        public void onStringAvailable(String s) {
            try {
                Gson gson = new Gson();
                JSONObject stringJson = new JSONObject(s);

                String event = stringJson.getString("event");
                String message = stringJson.getString("message");

                switch (event) {
                    case "talk":
                        // This will still allow
                        if (!KEEP_ALIVE_MSG.equals(message)) {
                            Message msg = gson.fromJson(message, Message.class);
                            BusProvider.getInstance().post(new TalkEvent(msg));
                        } else {
                            Log.d(TAG, "Received heartbeat from socket");
                        }
                        break;
                    case "join":
                        User joinedUser = gson.fromJson(stringJson.getString("user"), User.class);
                        if (joinedUser.getUserId() != userId) {
                            BusProvider.getInstance().post(new MemberJoinEvent(joinedUser));
                        }
                        break;
                    case "quit":
                        User quitUser = gson.fromJson(stringJson.getString("user"), User.class);
                        if (quitUser.getUserId() != userId) {
                            BusProvider.getInstance().post(new MemberLeaveEvent(quitUser));
                        }
                        break;
                    case "joinSuccess":
                        JSONObject joinJson = new JSONObject(message);
                        if (joinJson.has("isSubscribed")) {
                            BusProvider.getInstance().post(new IsSubscribedEvent(joinJson.getBoolean("isSubscribed")));
                        }
                        User[] roomMembers = gson.fromJson(joinJson.getString("roomMembers"), User[].class);
                        BusProvider.getInstance().post(new ReceivedRoomMembersEvent(roomMembers));
                        break;
                    case "favorite":
                        User msgFavoritor = gson.fromJson(stringJson.getString("user"), User.class);
                        if (msgFavoritor.getUserId() != userId) {
                            BusProvider.getInstance().post(new AddFavoriteEvent(msgFavoritor, Long.parseLong(message)));
                        }
                        break;
                    case "removeFavorite":
                        User msgUnfavoritor = gson.fromJson(stringJson.getString("user"), User.class);
                        if (msgUnfavoritor.getUserId() != userId) {
                            BusProvider.getInstance().post(new RemoveFavoriteEvent(msgUnfavoritor, Long.parseLong(message)));
                        }
                        break;
                    case "error":
                        Log.e(TAG, "Error: " + message);
                        Utils.debugToast(mContext, "Error: " + message);
                        break;
                    default:
                        Utils.debugToast(mContext, "Default socket event " + s);
                        Log.w(TAG, "DEFAULT RECEIVED: " + s);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Problem parsing socket received JSON: " + s);
            }
        }
    };

    private ReconnectCallback mReconnectCallback;

    private Queue<JSONObject> mSocketEventQueue = new ArrayDeque<>();

    private final long userId;

    private Context mContext;
    private WebSocket mWebSocket;

    public RoomSocket(Context context, WebSocket webSocket, ReconnectCallback reconnectCallback) {
        this.mContext = context;
        this.mReconnectCallback = reconnectCallback;
        this.userId = UserManager.getId(context);
        setWebSocket(webSocket);
    }

    public RoomSocket(Context context, ReconnectCallback reconnectCallback) {
        this(context, null, reconnectCallback);
    }

    public void setWebSocket(@Nullable WebSocket webSocket) {
        this.mWebSocket = webSocket;
        if (webSocket != null) {
            this.mWebSocket.setStringCallback(stringCallback);
            this.mWebSocket.setClosedCallback(closedCallback);
            sendQueuedEvents();
        }
    }

    public void sendTalk(String message, boolean isAnon) {
        JSONObject talkEvent = new JSONObject();
        try {
            talkEvent.put("event", "talk");
            talkEvent.put("message", message);
            if (isAnon) {
                talkEvent.put("isAnon", true);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Problem creating the chat message JSON: " + e.getMessage());
            return;
        }

        send(talkEvent);
    }

    public void sendFavorite(long messageId, boolean isFavorite) {
        JSONObject favoriteEvent = new JSONObject();
        try {
            favoriteEvent.put("event", "FavoriteNotification");
            favoriteEvent.put("messageId", messageId);
            String action = isFavorite ? "add" : "remove";
            favoriteEvent.put("action", action);
        } catch (JSONException e) {
            Log.e(TAG, "Problem creating the favorite event JSON: " + e.getMessage());
            return;
        }

        send(favoriteEvent);
    }

    private void send(JSONObject event) {
        if (socketIsAvailable()) {
            mWebSocket.send(event.toString());
        } else {
            sendEventSocketNotAvailable(event);
        }
    }

    private boolean socketIsAvailable() {
        return mWebSocket != null && mWebSocket.isOpen();
    }

    private void sendEventSocketNotAvailable(JSONObject event) {
        String err = "WebSocket is closed when trying to send "
                + event.toString() + "... Adding event to queue";
        Utils.debugToast(mContext, err);
        Log.w(TAG, err);
        mSocketEventQueue.add(event);

        mReconnectCallback.reconnect();
        Utils.debugToast(mContext, "ChatService is not currently connecting... Attempting to reconnect");
    }

    private void sendQueuedEvents() {
        while (!mSocketEventQueue.isEmpty()) {
            JSONObject event = mSocketEventQueue.poll();
            Utils.debugToast(mContext, "Sending message from mSocketEventQueue: " + event);
            Log.w(TAG, "Sending message from mSocketEventQueue: " + event);
            send(event);
        }
    }

    public void onPause() {
        if (socketIsAvailable()) {
            Log.i(TAG, "Pausing socket!");
            Utils.debugToast(mContext, "Pausing socket in on pause", Toast.LENGTH_SHORT);
            mWebSocket.pause();
        }
    }

    public void onResume() {
        if (mWebSocket != null && mWebSocket.isPaused()) {
            mWebSocket.resume();
            Utils.debugToast(mContext, "Resuming socket in on resume", Toast.LENGTH_SHORT);
        }
    }

}
