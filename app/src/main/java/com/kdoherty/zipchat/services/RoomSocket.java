package com.kdoherty.zipchat.services;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
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
import com.kdoherty.zipchat.events.TalkConfirmationEvent;
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
    private static final long INITIAL_BACKOFF_MILLIS = 500;
    private static final long MAX_BACKOFF_DELAY_MILLIS = 64 * 1000;

    private final CompletedCallback closedCallback = new CompletedCallback() {
        @Override
        public void onCompleted(Exception ex) {
            if (ex != null) {
                Log.w(TAG, "Attempting to recover from " + ex.getMessage());
                reconnect();
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

                switch (event) {
                    case "talk":
                        String talk = stringJson.getString("message");
                        if (!KEEP_ALIVE_MSG.equals(talk)) {
                            Message msg = gson.fromJson(talk, Message.class);
                            BusProvider.getInstance().post(new TalkEvent(msg));
                        } else {
                            Log.d(TAG, "Received heartbeat from socket");
                        }
                        break;
                    case "talk-confirmation":
                        String uuid = stringJson.getString("uuid");
                        Message msg = gson.fromJson(stringJson.getString("message"), Message.class);
                        BusProvider.getInstance().post(new TalkConfirmationEvent(uuid, msg));
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
                        JSONObject joinJson = new JSONObject(stringJson.getString("message"));
                        if (joinJson.has("isSubscribed")) {
                            BusProvider.getInstance().post(new IsSubscribedEvent(joinJson.getBoolean("isSubscribed")));
                        }
                        User[] roomMembers = gson.fromJson(joinJson.getString("roomMembers"), User[].class);
                        BusProvider.getInstance().post(new ReceivedRoomMembersEvent(roomMembers));
                        break;
                    case "favorite":
                        User msgFavoritor = gson.fromJson(stringJson.getString("user"), User.class);
                        if (msgFavoritor.getUserId() != userId) {
                            BusProvider.getInstance().post(new AddFavoriteEvent(msgFavoritor, Long.parseLong(stringJson.getString("message"))));
                        }
                        break;
                    case "removeFavorite":
                        User msgUnfavoritor = gson.fromJson(stringJson.getString("user"), User.class);
                        if (msgUnfavoritor.getUserId() != userId) {
                            BusProvider.getInstance().post(new RemoveFavoriteEvent(msgUnfavoritor, Long.parseLong(stringJson.getString("message"))));
                        }
                        break;
                    case "error":
                        Log.e(TAG, "Error: " + stringJson.getString("message"));
                        Utils.debugToast(mContext, "Error: " + stringJson.getString("message"));
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

    private Runnable mReconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if (socketIsAvailable()) {
                mIsReconnecting = false;
                mBackoffDelay = INITIAL_BACKOFF_MILLIS;
            } else {
                mBackoffDelay *= 2;

                if (mBackoffDelay <= MAX_BACKOFF_DELAY_MILLIS) {
                    reconnectWithRetry();
                }
            }
        }
    };

    private Queue<JSONObject> mSocketEventQueue = new ArrayDeque<>();

    private final long userId;

    private boolean mIsReconnecting = false;
    private ChatService mChatService;
    private Context mContext;
    private WebSocket mWebSocket;
    private Handler mHandler = new Handler();

    private long mBackoffDelay = INITIAL_BACKOFF_MILLIS;

    public RoomSocket(@NonNull Context context, @NonNull ChatService chatService) {
        this.mContext = context;
        this.userId = UserManager.getId(context);
        this.mChatService = chatService;
    }

    public void setWebSocket(@Nullable WebSocket webSocket) {
        this.mWebSocket = webSocket;
        if (webSocket != null) {
            this.mWebSocket.setStringCallback(stringCallback);
            this.mWebSocket.setClosedCallback(closedCallback);
            sendQueuedEvents();
        }
    }

    public void sendTalk(String message, boolean isAnon, String uuid) {
        JSONObject talkEvent = new JSONObject();
        try {
            talkEvent.put("event", "talk");
            talkEvent.put("message", message);
            talkEvent.put("isAnon", isAnon);
            talkEvent.put("uuid", uuid);
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
            favoriteEvent.put("action", isFavorite ? "add" : "remove");
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

        reconnect();

        Utils.debugToast(mContext, "ChatService is not currently connecting... Attempting to reconnect");
    }

    private void reconnect() {
        if (!mIsReconnecting) {
            mIsReconnecting = true;
            reconnectWithRetry();
        }
    }

    private void reconnectWithRetry() {
        mWebSocket = null;
        mChatService.cancel();
        mChatService = new ChatService(mChatService);

        Utils.debugToast(mContext, "Reconnect with " + mBackoffDelay + " delay", Toast.LENGTH_SHORT);

        mHandler.postDelayed(mReconnectRunnable, mBackoffDelay);
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
            Log.i(TAG, "Resuming socket!");
            mWebSocket.resume();
            Utils.debugToast(mContext, "Resuming socket in on resume", Toast.LENGTH_SHORT);
        }
    }

}
